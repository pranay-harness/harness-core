package software.wings.delegatetasks.buildsource;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static java.lang.Integer.parseInt;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.artifact.ArtifactStreamType.AMAZON_S3;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;
import static software.wings.beans.artifact.ArtifactStreamType.ARTIFACTORY;
import static software.wings.beans.artifact.ArtifactStreamType.GCS;
import static software.wings.service.impl.artifact.ArtifactCollectionServiceAsyncImpl.metadataOnlyStreams;

import com.google.inject.Inject;

import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.HIterator;
import io.harness.waiter.NotifyCallback;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.FeatureName;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ArtifactCollectionFailedAlert;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.PermitServiceImpl;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.PermitService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 7/20/18.
 */
@Data
@Slf4j
public class BuildSourceCallback implements NotifyCallback {
  private String accountId;
  private String artifactStreamId;
  private String permitId;
  private String settingId;
  private List<BuildDetails> builds;

  @Inject private transient ServiceResourceService serviceResourceService;
  @Inject private transient ArtifactService artifactService;
  @Inject private transient WingsPersistence wingsPersistence;
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient TriggerService triggerService;
  @Inject private transient DeploymentTriggerService deploymentTriggerService;
  @Inject private transient FeatureFlagService featureFlagService;
  @Inject private transient PermitService permitService;
  @Inject private transient AlertService alertService;
  @Inject private transient ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private transient ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  public BuildSourceCallback(String accountId, String artifactStreamId, String permitId,
      String settingId) { // todo: new constr with settingId
    this.accountId = accountId;
    this.artifactStreamId = artifactStreamId;
    this.permitId = permitId;
    this.settingId = settingId;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    ResponseData notifyResponseData = response.values().iterator().next();
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
    logger.info("In notify for artifact stream id: [{}]", artifactStreamId);
    if (notifyResponseData instanceof BuildSourceExecutionResponse) {
      if (SUCCESS.equals(((BuildSourceExecutionResponse) notifyResponseData).getCommandExecutionStatus())) {
        updatePermit(artifactStream, false);
        BuildSourceExecutionResponse buildSourceExecutionResponse = (BuildSourceExecutionResponse) notifyResponseData;
        if (buildSourceExecutionResponse.getBuildSourceResponse() != null) {
          builds = buildSourceExecutionResponse.getBuildSourceResponse().getBuildDetails();
        } else {
          logger.warn(
              "ASYNC_ARTIFACT_COLLECTION: null BuildSourceResponse in buildSourceExecutionResponse:[{}] for artifactStreamId [{}]",
              buildSourceExecutionResponse, artifactStreamId);
        }
        try {
          List<Artifact> artifacts = processBuilds(artifactStream);
          if (isNotEmpty(artifacts)) {
            logger.info("[{}] new artifacts collected for artifactStreamId {}",
                artifacts.stream().map(Artifact::getBuildNo).collect(Collectors.toList()), artifactStream.getUuid());
            if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
              triggerService.triggerExecutionPostArtifactCollectionAsync(
                  accountId, artifactStream.fetchAppId(), artifactStreamId, artifacts);
            } else {
              deploymentTriggerService.triggerExecutionPostArtifactCollectionAsync(
                  accountId, artifactStream.fetchAppId(), artifactStreamId, artifacts);
            }
          }
        } catch (WingsException ex) {
          ex.addContext(Account.class, accountId);
          ex.addContext(ArtifactStream.class, artifactStreamId);
          ExceptionLogger.logProcessedMessages(ex, MANAGER, logger);
          updatePermit(artifactStream, true);
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
      int failedCronAttempts = artifactStream.getFailedCronAttempts() + 1;
      artifactStreamService.updateFailedCronAttempts(
          artifactStream.getAccountId(), artifactStream.getUuid(), failedCronAttempts);
      logger.warn(
          "ASYNC_ARTIFACT_COLLECTION: failed to fetch/process builds for artifactStream[{}], totalFailedAttempt:[{}]",
          artifactStreamId, failedCronAttempts);
      if (PermitServiceImpl.shouldSendAlert(failedCronAttempts)) {
        String appId = artifactStream.fetchAppId();
        if (!GLOBAL_APP_ID.equals(appId)) {
          alertService.openAlert(accountId, null, AlertType.ARTIFACT_COLLECTION_FAILED,
              ArtifactCollectionFailedAlert.builder()
                  .appId(appId)
                  .serviceId(artifactStream.getServiceId())
                  .artifactStreamId(artifactStreamId)
                  .build());
        } else {
          alertService.openAlert(accountId, null, AlertType.ARTIFACT_COLLECTION_FAILED,
              ArtifactCollectionFailedAlert.builder()
                  .settingId(artifactStream.getSettingId())
                  .artifactStreamId(artifactStreamId)
                  .build());
        }
      }
    } else {
      if (artifactStream.getFailedCronAttempts() != 0) {
        logger.warn("ASYNC_ARTIFACT_COLLECTION: successfully fetched builds after [{}] failures for artifactStream[{}]",
            artifactStream.getFailedCronAttempts(), artifactStreamId);
        artifactStreamService.updateFailedCronAttempts(artifactStream.getAccountId(), artifactStream.getUuid(), 0);
        permitService.releasePermitByKey(artifactStream.getUuid());
        alertService.closeAlert(accountId, null, AlertType.ARTIFACT_COLLECTION_FAILED,
            ArtifactCollectionFailedAlert.builder().artifactStreamId(artifactStreamId).build());
      }
    }
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    ResponseData notifyResponseData = response.values().iterator().next();
    ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);

    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      logger.info("Request for artifactStreamId:[{}] failed :[{}]", artifactStreamId,
          ((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
    } else {
      logger.error("Unexpected  notify response:[{}] during artifact collection for artifactStreamId {} ", response,
          artifactStreamId);
    }
    updatePermit(artifactStream, true);
  }

  private List<Artifact> processBuilds(ArtifactStream artifactStream) {
    List<Artifact> newArtifacts = new ArrayList<>();
    if (artifactStream == null) {
      logger.info("Artifact Stream {} does not exist. Returning", artifactStreamId);
      return newArtifacts;
    }
    String artifactStreamType = artifactStream.getArtifactStreamType();
    if (metadataOnlyStreams.contains(artifactStreamType)) {
      collectMetaDataOnlyArtifacts(artifactStream, newArtifacts);
    } else if (ARTIFACTORY.name().equals(artifactStreamType)) {
      collectArtifactoryArtifacts(artifactStream, newArtifacts);
    } else if (AMAZON_S3.name().equals(artifactStreamType) || GCS.name().equals(artifactStreamType)) {
      collectGenericArtifacts(artifactStream, newArtifacts);
    } else {
      // Jenkins or Bamboo case
      if (!GLOBAL_APP_ID.equals(artifactStream.fetchAppId())) {
        collectLatestArtifact(artifactStream, newArtifacts);
      } else {
        collectSuccessfulArtifacts(artifactStream, newArtifacts);
      }
    }
    return newArtifacts;
  }

  private void collectArtifactoryArtifacts(ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    String appId = artifactStream.fetchAppId();
    if (!GLOBAL_APP_ID.equals(appId)) {
      if (artifactStreamServiceBindingService.getService(appId, artifactStream.getUuid(), true)
              .getArtifactType()
              .equals(ArtifactType.DOCKER)) {
        collectMetaDataOnlyArtifacts(artifactStream, newArtifacts);
      } else {
        collectGenericArtifacts(artifactStream, newArtifacts);
      }
    } else {
      if (artifactStream.fetchArtifactStreamAttributes().getRepositoryType().equals(RepositoryType.docker.name())) {
        collectMetaDataOnlyArtifacts(artifactStream, newArtifacts);
      } else {
        collectGenericArtifacts(artifactStream, newArtifacts);
      }
    }
  }

  private void collectLatestArtifact(ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    if (isEmpty(builds)) {
      return;
    }
    BuildDetails lastSuccessfulBuild = builds.get(0);
    Artifact lastCollectedArtifact = artifactService.fetchLatestArtifactForArtifactStream(artifactStream);
    int buildNo =
        (lastCollectedArtifact != null && lastCollectedArtifact.getMetadata().get(ArtifactMetadataKeys.buildNo) != null)
        ? parseInt(lastCollectedArtifact.getMetadata().get(ArtifactMetadataKeys.buildNo))
        : 0;
    if (lastSuccessfulBuild != null && parseInt(lastSuccessfulBuild.getNumber()) > buildNo) {
      logger.info("Existing build no {} is older than new build number {}. Collect new Artifact for ArtifactStream {}",
          buildNo, lastSuccessfulBuild.getNumber(), artifactStream.getUuid());
      newArtifacts.add(
          artifactService.create(artifactCollectionUtils.getArtifact(artifactStream, lastSuccessfulBuild)));
    }
  }

  private void collectSuccessfulArtifacts(ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    if (isEmpty(builds)) {
      return;
    }
    Set<String> newBuildNumbers = getNewBuildNumbers(artifactStream, builds);
    builds.forEach(buildDetails -> {
      if (newBuildNumbers.contains(buildDetails.getNumber())) {
        newArtifacts.add(artifactService.create(artifactCollectionUtils.getArtifact(artifactStream, buildDetails)));
      }
    });
  }

  private void collectMetaDataOnlyArtifacts(ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    if (!isEmpty(builds)) {
      Set<String> newBuildNumbers = getNewBuildNumbers(artifactStream, builds);
      builds.forEach((BuildDetails buildDetails1) -> {
        boolean artifactToBeCreated = false;
        if (AMI.name().equals(artifactStream.getArtifactStreamType())) {
          if (newBuildNumbers.contains(buildDetails1.getRevision())) {
            artifactToBeCreated = true;
          }
        } else if (newBuildNumbers.contains(buildDetails1.getNumber())) {
          artifactToBeCreated = true;
        }
        if (artifactToBeCreated) {
          logger.info("New Artifact version [{}] found for Artifact stream [type: {}, uuid: {}]. "
                  + "Add entry in Artifact collection",
              buildDetails1.getNumber(), artifactStream.getArtifactStreamType(), artifactStream.getUuid());
          Artifact newArtifact = artifactCollectionUtils.getArtifact(artifactStream, buildDetails1);
          newArtifacts.add(artifactService.create(newArtifact));
        }
      });
    }
  }

  private void collectGenericArtifacts(ArtifactStream artifactStream, List<Artifact> newArtifacts) {
    if (!isEmpty(builds)) {
      Set<String> newArtifactPaths = getNewArtifactPaths(artifactStream, builds);
      builds.forEach(buildDetails -> {
        if (newArtifactPaths.contains(buildDetails.getArtifactPath())) {
          newArtifacts.add(artifactService.create(artifactCollectionUtils.getArtifact(artifactStream, buildDetails)));
        }
      });
    }
  }

  /**
   * Gets all  existing artifacts for the given artifact stream, and compares with artifact source data
   */
  private Set<String> getNewBuildNumbers(ArtifactStream artifactStream, List<BuildDetails> builds) {
    Map<String, BuildDetails> buildNoDetails = new HashMap<>();
    if (!isEmpty(builds)) {
      if (AMI.name().equals(artifactStream.getArtifactStreamType())) {
        // AMI: AMI Name is not unique. So, treating AMI Id as unique which is stored in revision
        buildNoDetails =
            builds.parallelStream().collect(Collectors.toMap(BuildDetails::getRevision, Function.identity()));
        try (HIterator<Artifact> iterator =
                 new HIterator(artifactService.prepareArtifactWithMetadataQuery(artifactStream).fetch())) {
          while (iterator.hasNext()) {
            buildNoDetails.remove(iterator.next().getRevision());
          }
        }
      } else {
        buildNoDetails =
            builds.parallelStream().collect(Collectors.toMap(BuildDetails::getNumber, Function.identity()));
        try (HIterator<Artifact> iterator =
                 new HIterator(artifactService.prepareArtifactWithMetadataQuery(artifactStream).fetch())) {
          while (iterator.hasNext()) {
            buildNoDetails.remove(iterator.next().getBuildNo());
          }
        }
      }
    }
    return buildNoDetails.keySet();
  }

  private Set<String> getNewArtifactPaths(ArtifactStream artifactStream, List<BuildDetails> builds) {
    Map<String, BuildDetails> buildArtifactPathDetails =
        builds.parallelStream().collect(Collectors.toMap(BuildDetails::getArtifactPath, Function.identity()));
    try (HIterator<Artifact> iterator =
             new HIterator<>(artifactService.prepareArtifactWithMetadataQuery(artifactStream).fetch())) {
      while (iterator.hasNext()) {
        buildArtifactPathDetails.remove(iterator.next().getArtifactPath());
      }
    }
    return buildArtifactPathDetails.keySet();
  }
}
