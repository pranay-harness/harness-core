package software.wings.delegatetasks.buildsource;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.waiter.NotifyCallback;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.FeatureName;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ArtifactCollectionFailedAlert;
import software.wings.beans.artifact.Artifact;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    if (isEmpty(builds)) {
      return new ArrayList<>();
    }
    if (artifactStream == null) {
      logger.info("Artifact stream: [{}] does not exist. Returning", artifactStreamId);
      return new ArrayList<>();
    }

    // New build are filtered at the delegate. So all the builds coming in the BuildSourceExecutionResponse are the ones
    // not present in the DB.
    return builds.stream()
        .map(buildDetails -> artifactService.create(artifactCollectionUtils.getArtifact(artifactStream, buildDetails)))
        .collect(Collectors.toList());
  }
}
