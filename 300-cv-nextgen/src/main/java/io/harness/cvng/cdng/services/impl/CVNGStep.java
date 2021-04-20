package io.harness.cvng.cdng.services.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.DeploymentActivityDTO;
import io.harness.cvng.cdng.beans.CVNGStepParameter;
import io.harness.cvng.cdng.beans.CVNGStepType;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.services.api.CVNGStepTaskService;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobKeys;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.executables.AsyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CV)
public class CVNGStep implements AsyncExecutable<CVNGStepParameter> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(CVNGStepType.CVNG_VERIFY.getDisplayName()).build();
  @Inject private ActivityService activityService;
  @Inject private CVNGStepTaskService cvngStepTaskService;
  @Inject private Clock clock;
  @Override
  public AsyncExecutableResponse executeAsync(
      Ambiance ambiance, CVNGStepParameter stepParameters, StepInputPackage inputPackage) {
    log.info("ExecuteAsync called for CVNGStep");
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    Instant startTime = clock.instant();
    String activityUuid = activityService.register(accountId,
        DeploymentActivityDTO.builder()
            .serviceIdentifier(stepParameters.getServiceIdentifier().getValue())
            .environmentIdentifier(stepParameters.getEnvIdentifier().getValue())
            .accountIdentifier(accountId)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .verificationStartTime(startTime.toEpochMilli())
            .activityStartTime(startTime.toEpochMilli())
            .name("TODO")
            .deploymentTag(stepParameters.getDeploymentTag().getValue())
            .deploymentTag(stepParameters.getDeploymentTag().getValue())
            .verificationJobRuntimeDetails(
                Collections.singletonList(ActivityDTO.VerificationJobRuntimeDetails.builder()
                                              .verificationJobIdentifier(stepParameters.getVerificationJobIdentifier())
                                              .runtimeValues(getRuntimeValues(stepParameters))
                                              .build()))
            .build());
    CVNGStepTask cvngStepTask = CVNGStepTask.builder()
                                    .accountId(accountId)
                                    .status(CVNGStepTask.Status.IN_PROGRESS)
                                    .activityId(activityUuid)
                                    .build();
    cvngStepTaskService.create(cvngStepTask);
    return AsyncExecutableResponse.newBuilder().addCallbackIds(activityUuid).build();
  }

  private Map<String, String> getRuntimeValues(CVNGStepParameter stepParameters) {
    Map<String, String> runtimeValues = new HashMap<>();
    runtimeValues.put(VerificationJobKeys.serviceIdentifier, stepParameters.getServiceIdentifier().getValue());
    runtimeValues.put(VerificationJobKeys.envIdentifier, stepParameters.getEnvIdentifier().getValue());
    //  runtimeValues.put(VerificationJobKeys.duration, stepParameters.getDuration().getValue());
    return runtimeValues;
  }

  @Value
  @Builder
  public static class CVNGResponseData implements ResponseData {
    String activityId;
    ActivityStatusDTO activityStatusDTO;
  }

  @Override
  public StepResponse handleAsyncResponse(
      Ambiance ambiance, CVNGStepParameter stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("handleAsyncResponse async response");
    // Status.ERRORED - for exceptions
    // FAILED - for verification failed
    // SUCCEEDED - for verification success
    return StepResponse.builder().status(Status.SUCCEEDED).build();
    /* return StepResponse.builder()
        .status(Status.ERRORED)
        .failureInfo(FailureInfo.newBuilder()
                         .addFailureData(FailureData.newBuilder()
                                             .addFailureTypes(FailureType.UNKNOWN_FAILURE)
                                             .setMessage("Verification failed")
                                             .build())
                         .build())
        .build(); */
  }

  @Override
  public Class<CVNGStepParameter> getStepParametersClass() {
    return CVNGStepParameter.class;
  }

  @Override
  public void handleAbort(
      Ambiance ambiance, CVNGStepParameter stepParameters, AsyncExecutableResponse executableResponse) {}
}
