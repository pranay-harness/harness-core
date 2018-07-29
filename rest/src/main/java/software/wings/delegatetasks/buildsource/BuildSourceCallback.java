package software.wings.delegatetasks.buildsource;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.beans.artifact.Artifact.Status.APPROVED;
import static software.wings.beans.artifact.Artifact.Status.FAILED;
import static software.wings.beans.artifact.Artifact.Status.QUEUED;
import static software.wings.beans.artifact.Artifact.Status.READY;
import static software.wings.beans.artifact.Artifact.Status.REJECTED;
import static software.wings.beans.artifact.Artifact.Status.RUNNING;
import static software.wings.beans.artifact.Artifact.Status.WAITING;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.GCS;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.service.impl.artifact.ArtifactCollectionServiceImpl.metadataOnlyStreams;
import static software.wings.service.impl.artifact.ArtifactCollectionUtil.getArtifact;

import com.google.inject.Inject;

import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.HIterator;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.scheduler.PermitService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.utils.ArtifactType;
import software.wings.utils.MavenVersionCompareUtil;
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 7/20/18.
 */
public class BuildSourceCallback implements NotifyCallback {
  private String accountId;
  private String appId;
  private String artifactStreamId;
  private String permitId;
  private List<BuildDetails> builds;

  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient ArtifactService artifactService;
  @Inject private transient WingsPersistence wingsPersistence;
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient TriggerService triggerService;
  @Inject private transient PermitService permitService;

  private static final Logger logger = LoggerFactory.getLogger(BuildSourceCallback.class);

  public BuildSourceCallback(String accountId, String appId, String artifactStreamId, String permitId) {
    this.accountId = accountId;
    this.appId = appId;
    this.artifactStreamId = artifactStreamId;
    this.permitId = permitId;
  }

  @Override
  public void notify(Map<String, NotifyResponseData> response) {
    NotifyResponseData notifyResponseData = response.values().iterator().next();
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    if (notifyResponseData instanceof BuildSourceExecutionResponse) {
      if (SUCCESS.equals(((BuildSourceExecutionResponse) notifyResponseData).getCommandExecutionStatus())) {
        updatePermit(artifactStream, false);
        BuildSourceExecutionResponse buildSourceExecutionResponse = (BuildSourceExecutionResponse) notifyResponseData;
        builds = buildSourceExecutionResponse.getBuildSourceDelegateResponse().getBuildDetails();
        List<Artifact> artifacts = processBuilds(appId, artifactStream);
        if (isNotEmpty(artifacts)) {
          logger.info("[{}] new artifacts collected", artifacts.size());
          artifacts.forEach(artifact -> logger.info(artifact.toString()));
          Artifact latestArtifact = artifacts.get(artifacts.size() - 1);
          logger.info("Calling trigger execution if any for new artifact id {}", latestArtifact.getUuid());
          triggerService.triggerExecutionPostArtifactCollectionAsync(latestArtifact);
        }
      } else {
        logger.info("Request for artifactStreamId:[{}] failed :[{}]", artifactStreamId,
            ((BuildSourceExecutionResponse) notifyResponseData).getErrorMessage());
        //        permitService.releasePermit(permitId, true);
        updatePermit(artifactStream, true);
      }
    } else {
      notifyError(response);
    }
  }

  private void updatePermit(ArtifactStream artifactStream, boolean failed) {
    if (failed) {
      updateArtifactStreamFailedCount(artifactStream, artifactStream.getFailedCronAttempts() + 1);
      logger.warn("ASYNC_ARTIFACT_CRON: failed to get builds for artifactStream[{}], totalFailedAttempt:[{}]",
          artifactStreamId, artifactStream.getFailedCronAttempts() + 1);
    } else {
      permitService.releasePermit(permitId);
      if (artifactStream.getFailedCronAttempts() != 0) {
        updateArtifactStreamFailedCount(artifactStream, 0);
        logger.warn("ASYNC_ARTIFACT_CRON: successfully fetched build after [{}] failures for artifactStream[{}]",
            artifactStream.getFailedCronAttempts(), artifactStreamId);
      }
    }
  }

  private void updateArtifactStreamFailedCount(ArtifactStream artifactStream, int failedCount) {
    boolean updateFailedCronAttempts = artifactStreamService.updateFailedCronAttempts(
        artifactStream.getAppId(), artifactStream.getUuid(), failedCount);
  }

  @Override
  public void notifyError(Map<String, NotifyResponseData> response) {
    NotifyResponseData notifyResponseData = response.values().iterator().next();
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);

    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      logger.info("Request for artifactStreamId:[{}] failed :[{}]", artifactStreamId,
          ((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
    } else {
      logger.error("Unexpected notify response:[{}] for account {}", response, accountId);
    }
    updatePermit(artifactStream, true);
  }

  private List<Artifact> processBuilds(String appId, ArtifactStream artifactStream) {
    List<Artifact> newArtifacts = new ArrayList<>();
    if (artifactStream == null) {
      logger.info("Artifact Stream {} does not exist. Returning", artifactStreamId);
      return newArtifacts;
    }
    String artifactStreamType = artifactStream.getArtifactStreamType();
    if (metadataOnlyStreams.contains(artifactStreamType)) {
      collectMetaDataOnlyArtifacts(artifactStream, newArtifacts);
    } else if (ARTIFACTORY.name().equals(artifactStreamType)) {
      collectArtifactoryArtifacts(appId, artifactStream, newArtifacts);
    } else if (AMAZON_S3.name().equals(artifactStreamType) || GCS.name().equals(artifactStreamType)) {
      collectGenericArtifacts(appId, artifactStream, newArtifacts);
    } else {
      // Jenkins or Bamboo case
      collectLatestArtifact(appId, artifactStream, newArtifacts);
    }
    return newArtifacts;
  }

  private void collectArtifactoryArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    if (getService(appId, artifactStream).getArtifactType().equals(ArtifactType.DOCKER)) {
      collectMetaDataOnlyArtifacts(artifactStream, newArtifacts);
    } else if (artifactStream.getArtifactStreamAttributes().getRepositoryType() == null
        || !artifactStream.getArtifactStreamAttributes().getRepositoryType().equals("maven")) {
      collectGenericArtifacts(appId, artifactStream, newArtifacts);
    } else {
      collectMavenArtifacts(appId, artifactStream, newArtifacts);
    }
  }

  private void collectLatestArtifact(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    if (isEmpty(builds)) {
      return;
    }
    BuildDetails lastSuccessfulBuild = builds.get(0);
    Artifact lastCollectedArtifact = artifactService.fetchLatestArtifactForArtifactStream(
        appId, artifactStream.getUuid(), artifactStream.getSourceName());
    int buildNo = (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(BUILD_NO) != null)
        ? parseInt(lastCollectedArtifact.getMetadata().get(BUILD_NO))
        : 0;
    if (lastSuccessfulBuild != null && parseInt(lastSuccessfulBuild.getNumber()) > buildNo) {
      logger.info("Existing build no {} is older than new build number {}. Collect new Artifact for ArtifactStream {}",
          buildNo, lastSuccessfulBuild.getNumber(), artifactStream.getUuid());
      newArtifacts.add(artifactService.create(getArtifact(artifactStream, lastSuccessfulBuild)));
    }
  }

  private void collectMetaDataOnlyArtifacts(ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    if (!isEmpty(builds)) {
      Set<String> newBuildNumbers = getNewBuildNumbers(artifactStream, builds);
      builds.forEach((BuildDetails buildDetails1) -> {
        if (newBuildNumbers.contains(buildDetails1.getNumber())) {
          logger.info("New Artifact version [{}] found for Artifact stream [type: {}, uuid: {}]. "
                  + "Add entry in Artifact collection",
              buildDetails1.getNumber(), artifactStream.getArtifactStreamType(), artifactStream.getUuid());
          Artifact newArtifact = getArtifact(artifactStream, buildDetails1);
          newArtifacts.add(artifactService.create(newArtifact));
        }
      });
    }
  }

  private void collectGenericArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    if (!isEmpty(builds)) {
      Set<String> newArtifactPaths = getNewArtifactPaths(artifactStream, builds);
      builds.forEach(buildDetails -> {
        if (newArtifactPaths.contains(buildDetails.getArtifactPath())) {
          newArtifacts.add(artifactService.create(getArtifact(artifactStream, buildDetails)));
        }
      });
    }
  }

  private void collectMavenArtifacts(String appId, ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    if (isEmpty(builds)) {
      return;
    }
    BuildDetails latestVersion = builds.get(0);
    if (latestVersion == null) {
      return;
    }
    logger.info("Latest version in artifactory server {}", latestVersion);
    Artifact lastCollectedArtifact =
        artifactService.fetchLatestArtifactForArtifactStream(appId, artifactStreamId, artifactStream.getSourceName());
    String buildNo = (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(BUILD_NO) != null)
        ? lastCollectedArtifact.getMetadata().get(BUILD_NO)
        : "";
    logger.info("Last collected artifactory maven artifact version {} ", buildNo);
    if (buildNo.isEmpty() || versionCompare(latestVersion.getNumber(), buildNo) > 0) {
      logger.info(
          "Existing version no {} is older than new version number {}. Collect new Artifact for ArtifactStream {}",
          buildNo, latestVersion.getNumber(), artifactStreamId);
      newArtifacts.add(artifactService.create(getArtifact(artifactStream, latestVersion)));
    }
  }

  /**
   * Gets all  existing artifacts for the given artifact stream, and compares with artifact source data
   */
  private Set<String> getNewBuildNumbers(ArtifactStream artifactStream, List<BuildDetails> builds) {
    Map<String, BuildDetails> buildNoDetails =
        builds.parallelStream().collect(Collectors.toMap(BuildDetails::getNumber, Function.identity()));
    try (HIterator<Artifact> iterator = new HIterator(getArtifactQuery(artifactStream).fetch())) {
      while (iterator.hasNext()) {
        buildNoDetails.remove(iterator.next().getBuildNo());
      }
    }
    return buildNoDetails.keySet();
  }

  private Set<String> getNewArtifactPaths(ArtifactStream artifactStream, List<BuildDetails> builds) {
    Map<String, BuildDetails> buildArtifactPathDetails =
        builds.parallelStream().collect(Collectors.toMap(BuildDetails::getArtifactPath, Function.identity()));
    try (HIterator<Artifact> iterator = new HIterator<>(getArtifactQuery(artifactStream).fetch())) {
      while (iterator.hasNext()) {
        buildArtifactPathDetails.remove(iterator.next().getArtifactPath());
      }
    }
    return buildArtifactPathDetails.keySet();
  }

  private Query<Artifact> getArtifactQuery(ArtifactStream artifactStream) {
    return wingsPersistence.createQuery(Artifact.class)
        .project("metadata", true)
        .filter(Artifact.APP_ID_KEY, artifactStream.getAppId())
        .filter("artifactStreamId", artifactStream.getUuid())
        .filter("artifactSourceName", artifactStream.getSourceName())
        .field("status")
        .hasAnyOf(asList(QUEUED, RUNNING, REJECTED, WAITING, READY, APPROVED, FAILED))
        .disableValidation();
  }

  private Service getService(String appId, ArtifactStream artifactStream) {
    Service service = serviceResourceService.get(appId, artifactStream.getServiceId(), false);
    if (service == null) {
      artifactStreamService.delete(appId, artifactStream.getUuid());
      throw new WingsException(ErrorCode.GENERAL_ERROR)
          .addParam("message", format("Artifact stream %s is a zombie.", artifactStream.getUuid()));
    }
    return service;
  }

  /**
   * Compares two maven format version strings.
   */
  public static int versionCompare(String str1, String str2) {
    return MavenVersionCompareUtil.compare(str1).with(str2);
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getArtifactStreamId() {
    return artifactStreamId;
  }

  public void setArtifactStreamId(String artifactStreamId) {
    this.artifactStreamId = artifactStreamId;
  }

  public List<BuildDetails> getBuilds() {
    return builds;
  }

  public void setBuilds(List<BuildDetails> builds) {
    this.builds = builds;
  }

  public String getPermitId() {
    return permitId;
  }

  public void setPermitId(String permitId) {
    this.permitId = permitId;
  }
}
