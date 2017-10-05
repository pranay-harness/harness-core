package software.wings.scheduler;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.Artifact.Status.APPROVED;
import static software.wings.beans.artifact.Artifact.Status.READY;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;
import software.wings.utils.MavenVersionCompareUtil;
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

    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    Validator.notNullCheck("Artifact Stream", artifactStream);
    try {
      artifacts = collectNewArtifactsFromArtifactStream(appId, artifactStream);
    } catch (Exception e) {
      if (e instanceof WingsException
          && ((WingsException) e)
                 .getResponseMessageList()
                 .stream()
                 .anyMatch(responseMessage -> responseMessage.getCode() == ErrorCode.UNAVAILABLE_DELEGATES)) {
        logger.warn("No delegate available to collect artifact for app {}, artifact stream {}", appId, artifactStream);
      } else {
        logger.warn("Failed to collect artifact for appId {}, artifact stream {}", appId, artifactStream, e);
      }
    }

    if (artifacts != null && artifacts.size() != 0) {
      logger.info("[{}] new artifacts collected", artifacts.size());
      artifacts.forEach(artifact -> logger.info(artifact.toString()));
      Artifact latestArtifact = artifacts.get(artifacts.size() - 1);
      if (latestArtifact.getStatus().equals(READY) || latestArtifact.getStatus().equals(APPROVED)) {
        artifactStreamService.triggerStreamActionPostArtifactCollectionAsync(latestArtifact);
      } else {
        logger.info("Artifact is not yet READY to trigger post artifact collection deployment");
      }
    }
  }

  // TODO:: Simplify
  private List<Artifact> collectNewArtifactsFromArtifactStream(String appId, ArtifactStream artifactStream) {
    List<Artifact> newArtifacts = new ArrayList<>();
    String artifactStreamId = artifactStream.getUuid();
    if (artifactStream.getArtifactStreamType().equals(DOCKER.name())
        || artifactStream.getArtifactStreamType().equals(ECR.name())
        || artifactStream.getArtifactStreamType().equals(GCR.name())) {
      logger.info("Collecting tags for artifact stream id {} type {} and source name {} ", artifactStreamId,
          artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
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
      logger.info("Collecting artifact for artifact stream id {} type {} and source name {} ", artifactStreamId,
          artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
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
      Service service = serviceResourceService.get(appId, artifactStream.getServiceId(), false);
      Validator.notNullCheck("Service", service);
      ArtifactType artifactType = service.getArtifactType();
      if (artifactType.equals(ArtifactType.DOCKER)) {
        logger.info("Collecting Artifact for artifact stream id {} type {} and source name {} ", artifactStreamId,
            artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
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
      } else if (artifactType.equals(ArtifactType.RPM)) {
        logger.info("Collecting Artifact for artifact stream id {} type {} and source name {} ", artifactStreamId,
            artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
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
        logger.info("Collecting Artifact for artifact stream id {} type {} and source name {} ", artifactStreamId,
            artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
        BuildDetails latestVersion =
            buildSourceService.getLastSuccessfulBuild(appId, artifactStreamId, artifactStream.getSettingId());
        logger.info("Latest version in artifactory server {}", latestVersion);
        if (latestVersion != null) {
          Artifact lastCollectedArtifact =
              artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId);
          String buildNo =
              (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(Constants.BUILD_NO) != null)
              ? lastCollectedArtifact.getMetadata().get(Constants.BUILD_NO)
              : "";
          logger.info("Last collected artifactory maven artifact version {} ", buildNo);
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
            newArtifacts.add(artifactService.create(artifact, artifactType));
          } else {
            logger.info("Artifact of the version {} already collected.", buildNo);
          }
        }
      }
    } else if (artifactStream.getArtifactStreamType().equals(AMAZON_S3.name())) {
      logger.info("Collecting Artifact for artifact stream {} ", AMAZON_S3.name());
      List<BuildDetails> builds = buildSourceService.getBuilds(appId, artifactStreamId, artifactStream.getSettingId());
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
          Map<String, String> buildParameters = buildDetails.getBuildParameters();
          Map<String, String> map = Maps.newHashMap();
          map.put(Constants.ARTIFACT_PATH, buildParameters.get(Constants.ARTIFACT_PATH));
          map.put(Constants.ARTIFACT_FILE_NAME, buildParameters.get(Constants.ARTIFACT_PATH));
          map.put(Constants.BUILD_NO, buildParameters.get(Constants.BUILD_NO));
          map.put(Constants.BUCKET_NAME, buildParameters.get(Constants.BUCKET_NAME));
          map.put(Constants.KEY, buildParameters.get(Constants.KEY));
          map.put(Constants.URL, buildParameters.get(Constants.URL));

          Artifact artifact = anArtifact()
                                  .withAppId(appId)
                                  .withArtifactStreamId(artifactStreamId)
                                  .withArtifactSourceName(artifactStream.getSourceName())
                                  .withDisplayName(artifactStream.getArtifactDisplayName(""))
                                  .withMetadata(map)
                                  .build();
          newArtifacts.add(artifactService.create(artifact));
        }
      });

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
   * Compares two maven format version strings.
   *
   */
  public static int versionCompare(String str1, String str2) {
    return MavenVersionCompareUtil.compare(str1).with(str2);
    /* String[] vals1 = str1.split("\\.");
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
     return Integer.signum(vals1.length - vals2.length);*/
  }

  public static void main(String... args) {
    System.out.println("Version compare " + versionCompare("1.0-2017", "1.0-2017-1"));

    System.out.println("Snap shot version compare " + versionCompare("3.0-SNAPSHOT", "3.1-SNAPSHOT"));
  }
}
