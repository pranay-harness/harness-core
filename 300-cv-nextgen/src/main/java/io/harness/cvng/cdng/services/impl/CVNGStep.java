package io.harness.cvng.cdng.services.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.cdng.beans.CVNGStepParameter;
import io.harness.cvng.cdng.beans.CVNGStepType;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.services.api.CVNGStepTaskService;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.TypeAlias;

@Slf4j
@OwnedBy(HarnessTeam.CV)
public class CVNGStep implements AsyncExecutable<CVNGStepParameter> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(CVNGStepType.CVNG_VERIFY.getDisplayName()).build();
  @Inject private ActivityService activityService;
  @Inject private CVNGStepTaskService cvngStepTaskService;
  @Inject private Clock clock;
  @Override
  public AsyncExecutableResponse executeAsync(Ambiance ambiance, CVNGStepParameter stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    log.info("ExecuteAsync called for CVNGStep");
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    validate(stepParameters);
    Instant startTime = clock.instant();
    VerificationJob verificationJob =
        stepParameters.getVerificationJobBuilder()
            .serviceIdentifier(
                RuntimeParameter.builder().value(stepParameters.getServiceIdentifier().getValue()).build())
            .envIdentifier(RuntimeParameter.builder().value(stepParameters.getEnvIdentifier().getValue()).build())
            .projectIdentifier(projectIdentifier)
            .orgIdentifier(orgIdentifier)
            .accountId(accountId)
            .monitoringSources(stepParameters.getHealthSources()
                                   .stream()
                                   .map(healthSource -> healthSource.getIdentifier().getValue())
                                   .collect(Collectors.toList()))
            .build();
    DeploymentActivity deploymentActivity = DeploymentActivity.builder()
                                                .deploymentTag(stepParameters.getDeploymentTag().getValue())
                                                .verificationStartTime(startTime.toEpochMilli())
                                                .build();
    deploymentActivity.setVerificationJobs(Collections.singletonList(verificationJob));
    deploymentActivity.setActivityStartTime(startTime.minus(Duration.ofMinutes(5)));
    deploymentActivity.setOrgIdentifier(orgIdentifier);
    deploymentActivity.setAccountId(accountId);
    deploymentActivity.setProjectIdentifier(projectIdentifier);
    deploymentActivity.setServiceIdentifier(stepParameters.getServiceIdentifier().getValue());
    deploymentActivity.setEnvironmentIdentifier(stepParameters.getEnvIdentifier().getValue());
    deploymentActivity.setActivityName(getActivityName(stepParameters));
    String activityUuid = activityService.register(deploymentActivity);
    CVNGStepTask cvngStepTask = CVNGStepTask.builder()
                                    .accountId(accountId)
                                    .status(CVNGStepTask.Status.IN_PROGRESS)
                                    .activityId(activityUuid)
                                    .build();
    cvngStepTaskService.create(cvngStepTask);
    return AsyncExecutableResponse.newBuilder().addCallbackIds(activityUuid).build();
  }

  private void validate(CVNGStepParameter stepParameters) {
    Preconditions.checkNotNull(stepParameters.getServiceIdentifier().getValue(),
        "Could not resolve expression for serviceRef. Please check your expression.");
    Preconditions.checkNotNull(stepParameters.getEnvIdentifier().getValue(),
        "Could not resolve expression for envRef. Please check your expression.");
    Preconditions.checkNotNull(stepParameters.getDeploymentTag().getValue(),
        "Could not resolve expression for deployment tag. Please check your expression.");
  }

  private String getActivityName(CVNGStepParameter stepParameters) {
    return "CD Nextgen - " + stepParameters.getServiceIdentifier().getValue() + " - "
        + stepParameters.getDeploymentTag().getValue();
  }

  @Value
  @Builder
  public static class CVNGResponseData implements ResponseData, ProgressData {
    String activityId;
    ActivityStatusDTO activityStatusDTO;
  }
  @Value
  @Builder
  @JsonTypeName("verifyStepOutcome")
  @TypeAlias("verifyStepOutcome")
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
    // Status.ERRORED - for exceptions
    // FAILED - for verification failed
    // SUCCEEDED - for verification success

    Status status;
    switch (cvngResponseData.getActivityStatusDTO().getStatus()) {
      case VERIFICATION_PASSED:
        status = Status.SUCCEEDED;
        break;
      case VERIFICATION_FAILED:
        status = Status.FAILED;
        break;
      case ERROR:
      case IGNORED:
        status = Status.ERRORED;
        break;
      default:
        throw new IllegalStateException("Invalid status value: " + cvngResponseData.getActivityStatusDTO().getStatus());
    }

    StepResponseBuilder stepResponseBuilder = StepResponse.builder().status(status).stepOutcome(
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
                                                              .setCode(ErrorCode.DEFAULT_ERROR_CODE.name())
                                                              .setLevel(Level.ERROR.name())
                                                              .addFailureTypes(FailureType.VERIFICATION_FAILURE)
                                                              .setMessage("Verification failed")
                                                              .build())
                                          .build());
    }
    if (status == Status.ERRORED) {
      stepResponseBuilder.failureInfo(
          FailureInfo.newBuilder()
              .addFailureData(FailureData.newBuilder()
                                  .setCode(ErrorCode.UNKNOWN_ERROR.name())
                                  .setLevel(Level.ERROR.name())
                                  .addFailureTypes(FailureType.UNKNOWN_FAILURE)
                                  .setMessage("Verification could not complete due to an unknown error")
                                  .build())
              .build());
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
      Ambiance ambiance, CVNGStepParameter stepParameters, AsyncExecutableResponse executableResponse) {}
}
