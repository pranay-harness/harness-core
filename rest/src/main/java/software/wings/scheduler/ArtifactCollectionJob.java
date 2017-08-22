package software.wings.scheduler;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.common.collect.ImmutableMap;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.common.Constants;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by anubhaw on 11/8/16.
 */
public class ArtifactCollectionJob implements Job {
  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private BuildSourceService buildSourceService;
  @Inject private ServiceResourceService serviceResourceService;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String artifactStreamId = jobExecutionContext.getMergedJobDataMap().getString("artifactStreamId");
    String appId = jobExecutionContext.getMergedJobDataMap().getString("appId");
    List<Artifact> artifacts = null;
    try {
      artifacts = collectNewArtifactsFromArtifactStream(appId, artifactStreamId);
    } catch (Exception ex) {
      logger.warn("Artifact collection cron failed with error", ex);
    }

    if (artifacts != null && artifacts.size() != 0) {
      logger.info("[{}] new artifacts collected", artifacts.size());
      artifacts.forEach(artifact -> logger.info(artifact.toString()));
      artifactStreamService.triggerStreamActionPostArtifactCollectionAsync(artifacts.get(artifacts.size() - 1));
    }
  }

  // TODO:: Simplify
  private List<Artifact> collectNewArtifactsFromArtifactStream(String appId, String artifactStreamId) {
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    Validator.notNullCheck("Artifact Stream", artifactStream);
    List<Artifact> newArtifacts = new ArrayList<>();

    if (artifactStream.getArtifactStreamType().equals(DOCKER.name())
        || artifactStream.getArtifactStreamType().equals(ECR.name())
        || artifactStream.getArtifactStreamType().equals(GCR.name())) {
      List<BuildDetails> builds = buildSourceService.getBuilds(appId, artifactStreamId, artifactStream.getSettingId());
      List<Artifact> artifacts = artifactService
                                     .list(aPageRequest()
                                               .addFilter("appId", EQ, appId)
                                               .addFilter("artifactStreamId", EQ, artifactStreamId)
                                               .withLimit(UNLIMITED)
                                               .build(),
                                         false)
                                     .getResponse();

      Map<String, String> existingBuilds =
          artifacts.stream().collect(Collectors.toMap(Artifact::getBuildNo, Artifact::getUuid, (s, s2) -> s));

      builds.forEach(buildDetails -> {
        if (!existingBuilds.containsKey(buildDetails.getNumber())) {
          logger.info(
              "New Artifact version [{}] found for Artifact stream [type: {}, uuid: {}]. Add entry in Artifact collection",
              buildDetails.getNumber(), artifactStream.getArtifactStreamType(), artifactStream.getUuid());
          Artifact artifact = anArtifact()
                                  .withAppId(appId)
                                  .withArtifactStreamId(artifactStreamId)
                                  .withArtifactSourceName(artifactStream.getSourceName())
                                  .withDisplayName(artifactStream.getArtifactDisplayName(buildDetails.getNumber()))
                                  .withMetadata(ImmutableMap.of(Constants.BUILD_NO, buildDetails.getNumber()))
                                  .withRevision(buildDetails.getRevision())
                                  .build();
          newArtifacts.add(artifactService.create(artifact));
        }
      });
    } else if (artifactStream.getArtifactStreamType().equals(NEXUS.name())) {
      logger.info("Collecting Artifact for artifact stream {} ", NEXUS.name());
      BuildDetails latestVersion =
          buildSourceService.getLastSuccessfulBuild(appId, artifactStreamId, artifactStream.getSettingId());
      logger.info("Latest version in Nexus server {}", latestVersion);
      if (latestVersion != null) {
        Artifact lastCollectedArtifact = artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId);
        String buildNo =
            (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(Constants.BUILD_NO) != null)
            ? lastCollectedArtifact.getMetadata().get(Constants.BUILD_NO)
            : "";
        logger.info("Last collected Nexus artifact version {} ", buildNo);
        if (buildNo.isEmpty() || versionCompare(latestVersion.getNumber(), buildNo) > 0) {
          logger.info(
              "Existing version no {} is older than new version number {}. Collect new Artifact for ArtifactStream {}",
              buildNo, latestVersion.getNumber(), artifactStreamId);
          Artifact artifact = anArtifact()
                                  .withAppId(appId)
                                  .withArtifactStreamId(artifactStreamId)
                                  .withArtifactSourceName(artifactStream.getSourceName())
                                  .withDisplayName(artifactStream.getArtifactDisplayName(latestVersion.getNumber()))
                                  .withMetadata(ImmutableMap.of(Constants.BUILD_NO, latestVersion.getNumber()))
                                  .withRevision(latestVersion.getRevision())
                                  .build();
          newArtifacts.add(artifactService.create(artifact));
        } else {
          logger.info("Artifact of the version {} already collected.", buildNo);
        }
      }
    } else if (artifactStream.getArtifactStreamType().equals(ARTIFACTORY.name())) {
      Service service = serviceResourceService.get(appId, artifactStream.getServiceId());
      if (service.getArtifactType().equals(ArtifactType.DOCKER)) {
        logger.info("Collecting Artifact for artifact stream {} and artifact type {} ", ARTIFACTORY.name(),
            service.getArtifactType());
        List<BuildDetails> builds =
            buildSourceService.getBuilds(appId, artifactStreamId, artifactStream.getSettingId());
        List<Artifact> artifacts = artifactService
                                       .list(aPageRequest()
                                                 .addFilter("appId", EQ, appId)
                                                 .addFilter("artifactStreamId", EQ, artifactStreamId)
                                                 .withLimit(UNLIMITED)
                                                 .build(),
                                           false)
                                       .getResponse();
        Map<String, String> existingBuilds = artifacts.stream().distinct().collect(
            Collectors.toMap(Artifact::getBuildNo, Artifact::getUuid, (s, s2) -> s));
        builds.forEach(buildDetails -> {
          if (!existingBuilds.containsKey(buildDetails.getNumber())) {
            logger.info(
                "New Artifact version [{}] found for Artifact stream [type: {}, uuid: {}]. Add entry in Artifact collection",
                buildDetails.getNumber(), artifactStream.getArtifactStreamType(), artifactStream.getUuid());
            Artifact artifact = anArtifact()
                                    .withAppId(appId)
                                    .withArtifactStreamId(artifactStreamId)
                                    .withArtifactSourceName(artifactStream.getSourceName())
                                    .withDisplayName(artifactStream.getArtifactDisplayName(buildDetails.getNumber()))
                                    .withMetadata(ImmutableMap.of(Constants.BUILD_NO, buildDetails.getNumber()))
                                    .withRevision(buildDetails.getRevision())
                                    .build();
            newArtifacts.add(artifactService.create(artifact));
          }
        });
      } else if (service.getArtifactType().equals(ArtifactType.RPM)) {
        logger.info("Collecting Artifact for artifact stream {} and artifact type {} ", ARTIFACTORY.name(),
            service.getArtifactType());
        List<BuildDetails> builds =
            buildSourceService.getBuilds(appId, artifactStreamId, artifactStream.getSettingId());
        List<Artifact> artifacts = artifactService
                                       .list(aPageRequest()
                                                 .addFilter("appId", EQ, appId)
                                                 .addFilter("artifactStreamId", EQ, artifactStreamId)
                                                 .withLimit(UNLIMITED)
                                                 .build(),
                                           false)
                                       .getResponse();
        Map<String, String> existingBuilds = artifacts.stream().distinct().collect(
            Collectors.toMap(Artifact::getArtifactPath, Artifact::getUuid, (s, s2) -> s));
        builds.forEach(buildDetails -> {
          if (!existingBuilds.containsKey(buildDetails.getArtifactPath())) {
            Artifact artifact = anArtifact()
                                    .withAppId(appId)
                                    .withArtifactStreamId(artifactStreamId)
                                    .withArtifactSourceName(artifactStream.getSourceName())
                                    .withDisplayName(artifactStream.getArtifactDisplayName(""))
                                    .withMetadata(ImmutableMap.of(Constants.ARTIFACT_PATH,
                                        buildDetails.getArtifactPath(), Constants.ARTIFACT_FILE_NAME,
                                        buildDetails.getNumber(), Constants.BUILD_NO, buildDetails.getNumber()))
                                    .build();
            newArtifacts.add(artifactService.create(artifact));
          }
        });
      } else {
        logger.info("Collecting Artifact for artifact stream {} and artifact type {} ", ARTIFACTORY.name(),
            service.getArtifactType());
        BuildDetails lastSuccessfulBuild =
            buildSourceService.getLastSuccessfulBuild(appId, artifactStreamId, artifactStream.getSettingId());
        if (lastSuccessfulBuild != null) {
          Artifact lastCollectedArtifact =
              artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId);
          String artifactPath = (lastCollectedArtifact != null
                                    && lastCollectedArtifact.getMetadata().get(Constants.ARTIFACT_PATH) != null)
              ? lastCollectedArtifact.getMetadata().get(Constants.ARTIFACT_PATH)
              : "";
          if (artifactPath.compareTo(lastSuccessfulBuild.getArtifactPath()) >= 0) {
            logger.info("Latest artifact path {} already collected.", artifactPath);
          } else {
            logger.info(
                "Existing artifact path {} is older than new artifact path {}. Collect new Artifact for ArtifactStream {}",
                artifactPath, lastSuccessfulBuild.getArtifactPath(), artifactStreamId);
            Artifact artifact =
                anArtifact()
                    .withAppId(appId)
                    .withArtifactStreamId(artifactStreamId)
                    .withArtifactSourceName(artifactStream.getSourceName())
                    .withDisplayName(artifactStream.getArtifactDisplayName(""))
                    .withMetadata(ImmutableMap.of(Constants.ARTIFACT_PATH, lastSuccessfulBuild.getArtifactPath(),
                        Constants.BUILD_NO, lastSuccessfulBuild.getNumber()))
                    .build();
            newArtifacts.add(artifactService.create(artifact));
          }
        }
      }
    } else if (artifactStream.getArtifactStreamType().equals(AMAZON_S3.name())) {
      logger.info("Collecting Artifact for artifact stream {} ", AMAZON_S3.name());
      BuildDetails latestVersion =
          buildSourceService.getLastSuccessfulBuild(appId, artifactStreamId, artifactStream.getSettingId());
      if (latestVersion != null) {
        Artifact lastCollectedArtifact = artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId);
        String buildNo =
            (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(Constants.BUILD_NO) != null)
            ? lastCollectedArtifact.getMetadata().get(Constants.BUILD_NO)
            : "";
        if (buildNo.isEmpty() || versionCompare(latestVersion.getNumber(), buildNo) > 0) {
          logger.info(
              "Existing version no {} is older than new version number {}. Collect new Artifact for ArtifactStream {}",
              buildNo, latestVersion.getNumber(), artifactStreamId);
          Artifact artifact = anArtifact()
                                  .withAppId(appId)
                                  .withArtifactStreamId(artifactStreamId)
                                  .withArtifactSourceName(artifactStream.getSourceName())
                                  .withDisplayName(artifactStream.getArtifactDisplayName(latestVersion.getNumber()))
                                  .withMetadata(ImmutableMap.of(Constants.BUILD_NO, latestVersion.getNumber()))
                                  .withRevision(latestVersion.getRevision())
                                  .build();
          artifactService.create(artifact);
        } else {
          logger.info("Artifact of the version {} already collected.", buildNo);
        }
      }
    } else {
      BuildDetails lastSuccessfulBuild =
          buildSourceService.getLastSuccessfulBuild(appId, artifactStreamId, artifactStream.getSettingId());

      if (lastSuccessfulBuild != null) {
        Artifact lastCollectedArtifact = artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId);
        int buildNo =
            (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(Constants.BUILD_NO) != null)
            ? Integer.parseInt(lastCollectedArtifact.getMetadata().get(Constants.BUILD_NO))
            : 0;
        if (Integer.parseInt(lastSuccessfulBuild.getNumber()) > buildNo) {
          logger.info(
              "Existing build no {} is older than new build number {}. Collect new Artifact for ArtifactStream {}",
              buildNo, lastSuccessfulBuild.getNumber(), artifactStreamId);

          Map<String, String> metadata = lastSuccessfulBuild.getBuildParameters();
          metadata.put(Constants.BUILD_NO, lastSuccessfulBuild.getNumber());

          Artifact artifact =
              anArtifact()
                  .withAppId(appId)
                  .withArtifactStreamId(artifactStreamId)
                  .withArtifactSourceName(artifactStream.getSourceName())
                  .withDisplayName(artifactStream.getArtifactDisplayName(lastSuccessfulBuild.getNumber()))
                  .withDescription(lastSuccessfulBuild.getDescription())
                  .withMetadata(metadata)
                  .withRevision(lastSuccessfulBuild.getRevision())
                  .build();
          newArtifacts.add(artifactService.create(artifact));
        } else {
          logger.info("Artifact of the version {} already collected.", buildNo);
        }
      }
    }
    return newArtifacts;
  }

  /**
   * Compares two version strings.
   * <p>
   * Use this instead of String.compareTo() for a non-lexicographical
   * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
   *
   * @param str1 a string of ordinal numbers separated by decimal points.
   * @param str2 a string of ordinal numbers separated by decimal points.
   * @return The result is a negative integer if str1 is _numerically_ less than str2.
   * The result is a positive integer if str1 is _numerically_ greater than str2.
   * The result is zero if the strings are _numerically_ equal.
   * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
   */
  public static int versionCompare(String str1, String str2) {
    String[] vals1 = str1.split("\\.");
    String[] vals2 = str2.split("\\.");
    int i = 0;
    // set index to first non-equal ordinal or length of shortest version string
    while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
      i++;
    }
    // compare first non-equal ordinal number
    if (i < vals1.length && i < vals2.length) {
      int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
      return Integer.signum(diff);
    }
    // the strings are equal or one string is a substring of the other
    // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
    return Integer.signum(vals1.length - vals2.length);
  }

  public static void main(String... args) {
    System.out.println("Version compare " + versionCompare("30", "40"));
  }
}
