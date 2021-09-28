package io.harness.cvng.cdng.services.impl;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.cdng.beans.CVNGStepParameter;
import io.harness.cvng.cdng.beans.CVNGStepType;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.entities.CVNGStepTask.CVNGStepTaskBuilder;
import io.harness.cvng.cdng.services.api.CVNGStepTaskService;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.services.api.FeatureFlagService;
import io.harness.cvng.core.services.api.monitoredService.HealthSourceService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.RuntimeParameter;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.tasks.ProgressData;
import io.harness.tasks.ResponseData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.annotation.TypeAlias;

@Slf4j
@OwnedBy(HarnessTeam.CV)
public class CVNGStep implements AsyncExecutable<CVNGStepParameter> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(CVNGStepType.CVNG_VERIFY.getDisplayName())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  @Inject private ActivityService activityService;
  @Inject private CVNGStepTaskService cvngStepTaskService;
  @Inject private Clock clock;
  @Inject private MonitoredServiceService monitoredServiceService;
  @Inject private FeatureFlagService featureFlagService;
  @Override
  public AsyncExecutableResponse executeAsync(Ambiance ambiance, CVNGStepParameter stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    log.info("ExecuteAsync called for CVNGStep");
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    validate(stepParameters);
    String serviceIdentifier = stepParameters.getServiceIdentifier();
    String envIdentifier = stepParameters.getEnvIdentifier();

    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(accountId)
                                                            .orgIdentifier(orgIdentifier)
                                                            .projectIdentifier(projectIdentifier)
                                                            .serviceIdentifier(serviceIdentifier)
                                                            .environmentIdentifier(envIdentifier)
                                                            .build();

    CVNGStepTaskBuilder cvngStepTaskBuilder = CVNGStepTask.builder();
    MonitoredServiceDTO monitoredServiceDTO = monitoredServiceService.getMonitoredServiceDTO(serviceEnvironmentParams);
    if (monitoredServiceDTO == null || monitoredServiceDTO.getSources().getHealthSources().isEmpty()) {
      cvngStepTaskBuilder.skip(true);
      cvngStepTaskBuilder.callbackId(UUID.randomUUID().toString());
    } else {
      DeploymentActivity deploymentActivity =
          getDeploymentActivity(stepParameters, accountId, projectIdentifier, orgIdentifier, monitoredServiceDTO,
              Instant.ofEpochMilli(
                  AmbianceUtils.getStageLevelFromAmbiance(ambiance)
                      .orElseThrow(() -> new IllegalStateException("verify step needs to be part of a stage."))
                      .getStartTs()));
      String activityUuid;
      if (isDemoEnabled(accountId, ambiance)) {
        deploymentActivity.setVerificationStartTime(
            deploymentActivity.getVerificationStartTime().minus(Duration.ofMinutes(15)).toEpochMilli());
        deploymentActivity.setActivityStartTime(
            deploymentActivity.getActivityStartTime().minus(Duration.ofMinutes(15)));
        activityUuid = activityService.createActivityForDemo(deploymentActivity,
            isDev(ambiance) ? ActivityVerificationStatus.VERIFICATION_FAILED
                            : ActivityVerificationStatus.VERIFICATION_PASSED);
      } else {
        activityUuid = activityService.register(deploymentActivity);
      }
      cvngStepTaskBuilder.activityId(activityUuid).callbackId(activityUuid);
    }
    CVNGStepTask cvngStepTask =
        cvngStepTaskBuilder.accountId(accountId).status(CVNGStepTask.Status.IN_PROGRESS).build();
    cvngStepTaskService.create(cvngStepTask);
    return AsyncExecutableResponse.newBuilder().addCallbackIds(cvngStepTask.getCallbackId()).build();
  }

  private boolean isDemoEnabled(String accountId, Ambiance ambiance) {
    String identifier = AmbianceUtils.obtainCurrentLevel(ambiance).getIdentifier();
    return (identifier.endsWith("_dev") || identifier.endsWith("_prod"))
        && featureFlagService.isFeatureFlagEnabled(accountId, "CVNG_VERIFY_STEP_DEMO");
  }

  private boolean isDev(Ambiance ambiance) {
    String identifier = AmbianceUtils.obtainCurrentLevel(ambiance).getIdentifier();
    return identifier.endsWith("_dev");
  }

  @NotNull
  private DeploymentActivity getDeploymentActivity(CVNGStepParameter stepParameters, String accountId,
      String projectIdentifier, String orgIdentifier, MonitoredServiceDTO monitoredServiceDTO,
      Instant activityStartTime) {
    Instant startTime = clock.instant();
    VerificationJob verificationJob =
        stepParameters.getVerificationJobBuilder()
            .serviceIdentifier(RuntimeParameter.builder().value(stepParameters.getServiceIdentifier()).build())
            .envIdentifier(RuntimeParameter.builder().value(stepParameters.getEnvIdentifier()).build())
            .projectIdentifier(projectIdentifier)
            .orgIdentifier(orgIdentifier)
            .accountId(accountId)
            .monitoringSources(monitoredServiceDTO.getSources()
                                   .getHealthSources()
                                   .stream()
                                   .map(healthSource
                                       -> HealthSourceService.getNameSpacedIdentifier(
                                           monitoredServiceDTO.getIdentifier(), healthSource.getIdentifier()))
                                   .collect(Collectors.toList()))
            .build();
    DeploymentActivity deploymentActivity = DeploymentActivity.builder()
                                                .deploymentTag(stepParameters.getDeploymentTag().getValue())
                                                .verificationStartTime(startTime.toEpochMilli())
                                                .build();
    deploymentActivity.setVerificationJobs(Collections.singletonList(verificationJob));
    deploymentActivity.setActivityStartTime(activityStartTime);
    deploymentActivity.setOrgIdentifier(orgIdentifier);
    deploymentActivity.setAccountId(accountId);
    deploymentActivity.setProjectIdentifier(projectIdentifier);
    deploymentActivity.setServiceIdentifier(stepParameters.getServiceIdentifier());
    deploymentActivity.setEnvironmentIdentifier(stepParameters.getEnvIdentifier());
    deploymentActivity.setActivityName(getActivityName(stepParameters));
    deploymentActivity.setType(ActivityType.DEPLOYMENT);
    return deploymentActivity;
  }

  private void validate(CVNGStepParameter stepParameters) {
    Preconditions.checkNotNull(stepParameters.getDeploymentTag().getValue(),
        "Could not resolve expression for deployment tag. Please check your expression.");
  }

  private String getActivityName(CVNGStepParameter stepParameters) {
    return "CD Nextgen - " + stepParameters.getServiceIdentifier() + " - "
        + stepParameters.getDeploymentTag().getValue();
  }

  @Value
  @Builder
  public static class CVNGResponseData implements ResponseData, ProgressData {
    boolean skip;
    String activityId;
    ActivityStatusDTO activityStatusDTO;
  }
  @Value
  @Builder
  @JsonTypeName("verifyStepOutcome")
  @TypeAlias("verifyStepOutcome")
  @RecasterAlias("io.harness.cvng.cdng.services.impl.CVNGStep$VerifyStepOutcome")
  public static class VerifyStepOutcome implements ProgressData, Outcome {
    int progressPercentage;
    String estimatedRemainingTime;
    String activityId;
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, CVNGStepParameter stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("handleAsyncResponse async response");
    CVNGResponseData cvngResponseData = (CVNGResponseData) responseDataMap.values().iterator().next();
    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    if (cvngResponseData.isSkip()) {
      stepResponseBuilder.status(Status.SKIPPED)
          .failureInfo(
              FailureInfo.newBuilder()
                  .addFailureData(
                      FailureData.newBuilder()
                          .setCode(ErrorCode.UNKNOWN_ERROR.name())
                          .setLevel(Level.INFO.name())
                          .addFailureTypes(FailureType.SKIPPING_FAILURE)
                          .setMessage(String.format("No monitoredServiceRef is defined for service %s and env %s",
                              stepParameters.getServiceIdentifier(), stepParameters.getEnvIdentifier()))
                          .build())
                  .build());
    } else {
      Status status;
      FailureType failureType = null;
      String failureMessage = null;
      ErrorCode errorCode = null;
      switch (cvngResponseData.getActivityStatusDTO().getStatus()) {
        case VERIFICATION_PASSED:
          status = Status.SUCCEEDED;
          break;
        case VERIFICATION_FAILED:
          status = Status.FAILED;
          errorCode = ErrorCode.DEFAULT_ERROR_CODE;
          failureType = FailureType.VERIFICATION_FAILURE;
          failureMessage = "Verification failed";
          break;
        case ERROR:
        case IGNORED:
          status = Status.FAILED;
          errorCode = ErrorCode.UNKNOWN_ERROR;
          failureType = FailureType.UNKNOWN_FAILURE;
          failureMessage = "Verification could not complete due to an unknown error";
          break;
        default:
          throw new IllegalStateException(
              "Invalid status value: " + cvngResponseData.getActivityStatusDTO().getStatus());
      }
      stepResponseBuilder.status(status).stepOutcome(
          StepResponse.StepOutcome.builder()
              .name("output")
              .outcome(VerifyStepOutcome.builder()
                           .progressPercentage(cvngResponseData.getActivityStatusDTO().getProgressPercentage())
                           .estimatedRemainingTime(TimeUnit.MILLISECONDS.toMinutes(
                                                       cvngResponseData.getActivityStatusDTO().getRemainingTimeMs())
                               + " minutes")
                           .activityId(cvngResponseData.getActivityId())
                           .build())
              .build());
      if (status == Status.FAILED) {
        stepResponseBuilder.failureInfo(FailureInfo.newBuilder()
                                            .addFailureData(FailureData.newBuilder()
                                                                .setCode(errorCode.name())
                                                                .setLevel(Level.ERROR.name())
                                                                .addFailureTypes(failureType)
                                                                .setMessage(failureMessage)
                                                                .build())
                                            .build());
      }
    }
    return stepResponseBuilder.build();
  }

  @Override
  public ProgressData handleProgress(Ambiance ambiance, CVNGStepParameter stepParameters, ProgressData progressData) {
    CVNGResponseData cvngResponseData = (CVNGResponseData) progressData;
    return VerifyStepOutcome.builder()
        .progressPercentage(cvngResponseData.getActivityStatusDTO().getProgressPercentage())
        .estimatedRemainingTime(
            TimeUnit.MILLISECONDS.toMinutes(cvngResponseData.getActivityStatusDTO().getRemainingTimeMs()) + " minutes")
        .activityId(cvngResponseData.getActivityId())
        .build();
  }

  @Override
  public Class<CVNGStepParameter> getStepParametersClass() {
    return CVNGStepParameter.class;
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, CVNGStepParameter stepParameters, AsyncExecutableResponse executableResponse) {
    CVNGStepTask cvngStepTask = cvngStepTaskService.getByCallBackId(executableResponse.getCallbackIds(0));
    activityService.abort(cvngStepTask.getActivityId());
  }
}
