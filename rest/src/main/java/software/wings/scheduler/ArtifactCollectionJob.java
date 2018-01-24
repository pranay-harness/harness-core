package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static software.wings.beans.ErrorCode.GENERAL_ERROR;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.Artifact.Status.APPROVED;
import static software.wings.beans.artifact.Artifact.Status.READY;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.DOCKER;
import static software.wings.beans.artifact.ArtifactStreamType.ECR;
import static software.wings.beans.artifact.ArtifactStreamType.GCR;
import static software.wings.beans.artifact.ArtifactStreamType.NEXUS;
import static software.wings.common.Constants.ARTIFACT_FILE_NAME;
import static software.wings.common.Constants.ARTIFACT_PATH;
import static software.wings.common.Constants.BUCKET_NAME;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.common.Constants.KEY;
import static software.wings.common.Constants.URL;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.exception.WingsException.Scenario.BACKGROUND_JOB;
import static software.wings.utils.ArtifactType.RPM;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.utils.ArtifactType;
import software.wings.utils.MavenVersionCompareUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 11/8/16.
 */
public class ArtifactCollectionJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(ArtifactCollectionJob.class);

  public static final String GROUP = "ARTIFACT_STREAM_CRON_GROUP";
  private static final int POLL_INTERVAL = 60; // in secs

  public static final String APP_ID_KEY = "appId";
  public static final String ARTIFACT_STREAM_ID_KEY = "artifactStreamId";

  @Inject private ArtifactService artifactService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private BuildSourceService buildSourceService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ExecutorService executorService;
  @Inject private TriggerService triggerService;
  @Inject private PersistentLocker persistentLocker;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  public static void addDefaultJob(QuartzScheduler jobScheduler, String appId, String artifactStreamId) {
    // If somehow this job was scheduled from before, we would like to reset it to start counting from now.
    jobScheduler.deleteJob(artifactStreamId, GROUP);

    JobDetail job = JobBuilder.newJob(ArtifactCollectionJob.class)
                        .withIdentity(artifactStreamId, ArtifactCollectionJob.GROUP)
                        .usingJobData(ARTIFACT_STREAM_ID_KEY, artifactStreamId)
                        .usingJobData(APP_ID_KEY, appId)
                        .build();

    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity(artifactStreamId, ArtifactCollectionJob.GROUP)
                          .startNow()
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(POLL_INTERVAL)
                                            .repeatForever()
                                            .withMisfireHandlingInstructionNowWithExistingCount())
                          .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String artifactStreamId = jobExecutionContext.getMergedJobDataMap().getString(ARTIFACT_STREAM_ID_KEY);
    String appId = jobExecutionContext.getMergedJobDataMap().getString(APP_ID_KEY);
    logger.info("Received artifact collection job request for appId {} artifactStreamId {}", appId, artifactStreamId);
    executorService.submit(() -> executeJobAsync(appId, artifactStreamId));
    logger.info("Submitted request successfully");
  }

  private void executeJobAsync(String appId, String artifactStreamId) {
    List<Artifact> artifacts = null;
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    if (artifactStream == null || !artifactStream.isAutoDownload()) {
      jobScheduler.deleteJob(artifactStreamId, GROUP);
      return;
    }

    try {
      artifacts = collectNewArtifactsFromArtifactStream(appId, artifactStream);
    } catch (WingsException exception) {
      exception.logProcessedMessages(BACKGROUND_JOB);
    } catch (Exception e) {
      logger.warn("Failed to collect artifact for appId {}, artifact stream {}", appId, artifactStream, e);
    }
    if (isNotEmpty(artifacts)) {
      logger.info("[{}] new artifacts collected", artifacts.size());
      artifacts.forEach(artifact -> logger.info(artifact.toString()));
      Artifact latestArtifact = artifacts.get(artifacts.size() - 1);
      if (latestArtifact.getStatus().equals(READY) || latestArtifact.getStatus().equals(APPROVED)) {
        triggerService.triggerExecutionPostArtifactCollectionAsync(latestArtifact);
      } else {
        logger.info("Artifact is not yet READY to trigger post artifact collection deployment");
      }
    }
  }

  private List<Artifact> collectNewArtifactsFromArtifactStream(String appId, ArtifactStream artifactStream) {
    List<Artifact> newArtifacts = new ArrayList<>();
    String artifactStreamId = artifactStream.getUuid();
    if (artifactStream.getArtifactStreamType().equals(DOCKER.name())
        || artifactStream.getArtifactStreamType().equals(ECR.name())
        || artifactStream.getArtifactStreamType().equals(GCR.name())) {
      collectDockerArtifacts(appId, artifactStream, newArtifacts);
    } else if (artifactStream.getArtifactStreamType().equals(NEXUS.name())) {
      collectNexusArtifacts(appId, artifactStream, newArtifacts);
    } else if (artifactStream.getArtifactStreamType().equals(ARTIFACTORY.name())) {
      collectArtifactoryArtifacts(appId, artifactStream, newArtifacts);
    } else if (artifactStream.getArtifactStreamType().equals(AMAZON_S3.name())) {
      collectS3Artifacts(appId, artifactStream, newArtifacts, artifactStreamId);
    } else if (AMI.name().equals(artifactStream.getArtifactStreamType())) {
      collectAmiImages(appId, artifactStream, newArtifacts);
    } else {
      collectJenkinsBambooArtifacts(appId, artifactStream, newArtifacts);
    }
    return newArtifacts;
  }

  private void collectAmiImages(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    String artifactStreamId = artifactStream.getUuid();
    try (AcquiredLock lock =
             persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, Duration.ofMinutes(3))) {
      logger.info("Collecting images for artifact stream id {} type {} and source name {} ", artifactStreamId,
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
          logger.info("New Artifact version [{}] found for Artifact stream [type: {}, uuid: {}]. "
                  + "Add entry in Artifact collection",
              buildDetails.getNumber(), artifactStream.getArtifactStreamType(), artifactStream.getUuid());
          Artifact artifact = anArtifact()
                                  .withAppId(appId)
                                  .withArtifactStreamId(artifactStreamId)
                                  .withArtifactSourceName(artifactStream.getSourceName())
                                  .withDisplayName(artifactStream.getArtifactDisplayName(buildDetails.getNumber()))
                                  .withMetadata(ImmutableMap.of(BUILD_NO, buildDetails.getNumber()))
                                  .withRevision(buildDetails.getRevision())
                                  .build();
          newArtifacts.add(artifactService.create(artifact));
        }
      });
    }
  }

  private void collectJenkinsBambooArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    String artifactStreamId = artifactStream.getUuid();
    try (AcquiredLock lock =
             persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, Duration.ofMinutes(3))) {
      logger.info("Collecting Artifact for artifact stream id {} type {} and source name {} ", artifactStreamId,
          artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
      BuildDetails lastSuccessfulBuild =
          buildSourceService.getLastSuccessfulBuild(appId, artifactStreamId, artifactStream.getSettingId());
      if (lastSuccessfulBuild == null) {
        return;
      }

      Artifact lastCollectedArtifact =
          artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId, artifactStream.getSourceName());
      int buildNo = (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(BUILD_NO) != null)
          ? Integer.parseInt(lastCollectedArtifact.getMetadata().get(BUILD_NO))
          : 0;
      if (Integer.parseInt(lastSuccessfulBuild.getNumber()) > buildNo) {
        logger.info(
            "Existing build no {} is older than new build number {}. Collect new Artifact for ArtifactStream {}",
            buildNo, lastSuccessfulBuild.getNumber(), artifactStreamId);

        Map<String, String> metadata = lastSuccessfulBuild.getBuildParameters();
        metadata.put(BUILD_NO, lastSuccessfulBuild.getNumber());
        metadata.put(URL, lastSuccessfulBuild.getBuildUrl());

        Artifact artifact = anArtifact()
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
        logger.info("Artifact of the version {} already collected. Artifact status {}", buildNo,
            lastCollectedArtifact.getStatus());
      }
    }
  }

  private void collectDockerArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    String artifactStreamId = artifactStream.getUuid();
    try (AcquiredLock lock =
             persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, Duration.ofMinutes(3))) {
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
          logger.debug("New Artifact version [{}] found for Artifact stream [type: {}, uuid: {}]."
                  + " Add entry in Artifact collection",
              buildDetails.getNumber(), artifactStream.getArtifactStreamType(), artifactStream.getUuid());
          Artifact artifact = anArtifact()
                                  .withAppId(appId)
                                  .withArtifactStreamId(artifactStreamId)
                                  .withArtifactSourceName(artifactStream.getSourceName())
                                  .withDisplayName(artifactStream.getArtifactDisplayName(buildDetails.getNumber()))
                                  .withMetadata(ImmutableMap.of(BUILD_NO, buildDetails.getNumber()))
                                  .withRevision(buildDetails.getRevision())
                                  .build();
          newArtifacts.add(artifactService.create(artifact));
        }
      });
    }
  }

  private void collectS3Artifacts(
      String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts, String artifactStreamId) {
    try (AcquiredLock lock =
             persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, Duration.ofMinutes(3))) {
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
          map.put(ARTIFACT_PATH, buildParameters.get(ARTIFACT_PATH));
          map.put(ARTIFACT_FILE_NAME, buildParameters.get(ARTIFACT_PATH));
          map.put(BUILD_NO, buildParameters.get(BUILD_NO));
          map.put(BUCKET_NAME, buildParameters.get(BUCKET_NAME));
          map.put(KEY, buildParameters.get(KEY));
          map.put(URL, buildParameters.get(URL));

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
    }
  }

  private void collectArtifactoryArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    Service service = obtainService(appId, artifactStream);
    ArtifactType artifactType = service.getArtifactType();
    if (artifactType.equals(ArtifactType.DOCKER)) {
      collectArtifactoryDockerArtifacts(appId, artifactStream, newArtifacts);
    } else if (artifactStream.getArtifactStreamAttributes().getRepositoryType() == null
        || !artifactStream.getArtifactStreamAttributes().getRepositoryType().equals("maven")) {
      collectMavenArtifactoryArtifacts(appId, artifactStream, newArtifacts);
    } else {
      collectArtifactoryGenericArtifacts(appId, artifactStream, newArtifacts, artifactType);
    }
  }

  private void collectArtifactoryGenericArtifacts(
      String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts, ArtifactType artifactType) {
    String artifactStreamId = artifactStream.getUuid();
    try (AcquiredLock lock =
             persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, Duration.ofMinutes(3))) {
      logger.info("Collecting Artifact for artifact stream id {} type {} and source name {} ", artifactStreamId,
          artifactStream.getArtifactStreamType(), artifactStream.getSourceName());
      BuildDetails latestVersion =
          buildSourceService.getLastSuccessfulBuild(appId, artifactStreamId, artifactStream.getSettingId());
      if (latestVersion == null) {
        return;
      }
      logger.debug("Latest version in artifactory server {}", latestVersion);
      Artifact lastCollectedArtifact =
          artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId, artifactStream.getSourceName());
      String buildNo = (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(BUILD_NO) != null)
          ? lastCollectedArtifact.getMetadata().get(BUILD_NO)
          : "";

      logger.debug("Last collected artifactory maven artifact version {} ", buildNo);
      if (buildNo.isEmpty() || versionCompare(latestVersion.getNumber(), buildNo) > 0) {
        logger.debug(
            "Existing version no {} is older than new version number {}. Collect new Artifact for ArtifactStream {}",
            buildNo, latestVersion.getNumber(), artifactStreamId);
        Artifact artifact = anArtifact()
                                .withAppId(appId)
                                .withArtifactStreamId(artifactStreamId)
                                .withArtifactSourceName(artifactStream.getSourceName())
                                .withDisplayName(artifactStream.getArtifactDisplayName(latestVersion.getNumber()))
                                .withMetadata(ImmutableMap.of(BUILD_NO, latestVersion.getNumber()))
                                .withRevision(latestVersion.getRevision())
                                .build();
        newArtifacts.add(artifactService.create(artifact, artifactType));
      }
    }
  }

  private void collectMavenArtifactoryArtifacts(
      String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    String artifactStreamId = artifactStream.getUuid();
    try (AcquiredLock lock =
             persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, Duration.ofMinutes(3))) {
      logger.info("Collecting Artifact for artifact stream id {} type {} and source name {} ", artifactStreamId,
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
      Map<String, String> existingBuilds = artifacts.stream().distinct().collect(
          Collectors.toMap(Artifact::getArtifactPath, Artifact::getUuid, (s, s2) -> s));
      builds.forEach(buildDetails -> {
        if (!existingBuilds.containsKey(buildDetails.getArtifactPath())) {
          Artifact artifact = anArtifact()
                                  .withAppId(appId)
                                  .withArtifactStreamId(artifactStreamId)
                                  .withArtifactSourceName(artifactStream.getSourceName())
                                  .withDisplayName(artifactStream.getArtifactDisplayName(""))
                                  .withMetadata(ImmutableMap.of(ARTIFACT_PATH, buildDetails.getArtifactPath(),
                                      ARTIFACT_FILE_NAME, buildDetails.getNumber(), BUILD_NO, buildDetails.getNumber()))
                                  .build();
          newArtifacts.add(artifactService.create(artifact, RPM));
        }
      });
    }
  }

  private void collectArtifactoryDockerArtifacts(
      String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    String artifactStreamId = artifactStream.getUuid();
    try (AcquiredLock lock =
             persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, Duration.ofMinutes(3))) {
      logger.info("Collecting Artifact for artifact stream id {} type {} and source name {} ", artifactStreamId,
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
                                  .withMetadata(ImmutableMap.of(BUILD_NO, buildDetails.getNumber()))
                                  .withRevision(buildDetails.getRevision())
                                  .build();
          newArtifacts.add(artifactService.create(artifact));
        }
      });
    }
  }

  private void collectNexusArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    Service service = obtainService(appId, artifactStream);
    ArtifactType artifactType = service.getArtifactType();
    if (artifactType.equals(ArtifactType.DOCKER)) {
      collectNexusMavenArtifacts(appId, artifactStream, newArtifacts);
    } else {
      collectNexusGenericArtifacts(appId, artifactStream, newArtifacts);
    }
  }

  private void collectNexusGenericArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    String artifactStreamId = artifactStream.getUuid();
    try (AcquiredLock lock =
             persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, Duration.ofMinutes(3))) {
      logger.info("Collecting artifact for artifact stream id {} type {} and source name {} ", artifactStreamId,
          artifactStream.getArtifactStreamType(), artifactStream.getSourceName());

      BuildDetails latestVersion =
          buildSourceService.getLastSuccessfulBuild(appId, artifactStreamId, artifactStream.getSettingId());
      if (latestVersion == null) {
        return;
      }
      logger.debug("Latest version in Nexus server {}", latestVersion);
      Artifact lastCollectedArtifact =
          artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId, artifactStream.getSourceName());
      String buildNo = (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(BUILD_NO) != null)
          ? lastCollectedArtifact.getMetadata().get(BUILD_NO)
          : "";
      logger.debug("Last collected Nexus artifact version {} ", buildNo);
      if (buildNo.isEmpty() || versionCompare(latestVersion.getNumber(), buildNo) > 0) {
        logger.info(
            "Existing version no {} is older than new version number {}. Collect new Artifact for ArtifactStream {}",
            buildNo, latestVersion.getNumber(), artifactStreamId);
        Artifact artifact = anArtifact()
                                .withAppId(appId)
                                .withArtifactStreamId(artifactStreamId)
                                .withArtifactSourceName(artifactStream.getSourceName())
                                .withDisplayName(artifactStream.getArtifactDisplayName(latestVersion.getNumber()))
                                .withMetadata(ImmutableMap.of(BUILD_NO, latestVersion.getNumber()))
                                .withRevision(latestVersion.getRevision())
                                .build();
        newArtifacts.add(artifactService.create(artifact));
      }
    }
  }

  private void collectNexusMavenArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    String artifactStreamId = artifactStream.getUuid();
    try (AcquiredLock lock =
             persistentLocker.acquireLock(ArtifactStream.class, artifactStreamId, Duration.ofMinutes(3))) {
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
                                  .withMetadata(ImmutableMap.of(BUILD_NO, buildDetails.getNumber()))
                                  .withRevision(buildDetails.getRevision())
                                  .build();
          newArtifacts.add(artifactService.create(artifact, ArtifactType.DOCKER));
        }
      });
    }
  }

  public Service obtainService(String appId, ArtifactStream artifactStream) {
    Service service = serviceResourceService.get(appId, artifactStream.getServiceId(), false);
    if (service == null) {
      PruneEntityJob.addDefaultJob(
          jobScheduler, Service.class, appId, artifactStream.getServiceId(), Duration.ofMillis(0));
      throw new WingsException(GENERAL_ERROR)
          .addParam("args", format("Artifact stream %s is a zombie.", artifactStream.getUuid()));
    }
    return service;
  }

  /**
   * Compares two maven format version strings.
   *
   */
  public static int versionCompare(String str1, String str2) {
    return MavenVersionCompareUtil.compare(str1).with(str2);
  }
}
