/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.services.impl;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.activity.entities.Activity;
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
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.FeatureFlagService;
import io.harness.cvng.core.services.api.monitoredService.HealthSourceService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.RuntimeParameter;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ExecutionStatus;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceBuilder;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.data.structure.EmptyPredicate;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
  @Inject private VerificationJobInstanceService verificationJobInstanceService;

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

    MonitoredServiceDTO monitoredServiceDTO = monitoredServiceService.getMonitoredServiceDTO(serviceEnvironmentParams);
    Instant deploymentStartTime = Instant.ofEpochMilli(
        AmbianceUtils.getStageLevelFromAmbiance(ambiance)
            .orElseThrow(() -> new IllegalStateException("verify step needs to be part of a stage."))
            .getStartTs());
    if (monitoredServiceDTO == null || monitoredServiceDTO.getSources().getHealthSources().isEmpty()) {
      CVNGStepTaskBuilder cvngStepTaskBuilder = CVNGStepTask.builder();
      cvngStepTaskBuilder.skip(true);
      cvngStepTaskBuilder.callbackId(UUID.randomUUID().toString());
      CVNGStepTask cvngStepTask = cvngStepTaskBuilder.accountId(serviceEnvironmentParams.getAccountIdentifier())
                                      .orgIdentifier(serviceEnvironmentParams.getOrgIdentifier())
                                      .projectIdentifier(serviceEnvironmentParams.getProjectIdentifier())
                                      .serviceIdentifier(serviceEnvironmentParams.getServiceIdentifier())
                                      .environmentIdentifier(serviceEnvironmentParams.getEnvironmentIdentifier())
                                      .status(CVNGStepTask.Status.IN_PROGRESS)
                                      .deploymentStartTime(deploymentStartTime)
                                      .build();
      cvngStepTaskService.create(cvngStepTask);
      return AsyncExecutableResponse.newBuilder().addCallbackIds(cvngStepTaskBuilder.build().getCallbackId()).build();
    } else {
      String callbackId;
      if (!featureFlagService.isFeatureFlagEnabled(accountId, "CVNG_VERIFY_STEP_TO_SINGLE_ACTIVITY")) {
        callbackId = callBackIdFromActivity(serviceEnvironmentParams, ambiance, stepParameters, monitoredServiceDTO);
      } else {
        String verificationJobInstanceId;
        Activity activity = getDeploymentActivity(
            stepParameters, serviceEnvironmentParams, monitoredServiceDTO, ambiance, deploymentStartTime);
        if (isDemoEnabled(accountId, ambiance)) {
          VerificationJobInstance verificationJobInstance =
              getVerificationJobInstanceForDemo(AmbianceUtils.obtainCurrentLevel(ambiance).getIdentifier(),
                  stepParameters, serviceEnvironmentParams, monitoredServiceDTO, deploymentStartTime,
                  isDev(ambiance) ? ActivityVerificationStatus.VERIFICATION_FAILED
                                  : ActivityVerificationStatus.VERIFICATION_PASSED);
          verificationJobInstanceId =
              verificationJobInstanceService.createDemoInstances(Arrays.asList(verificationJobInstance)).get(0);
        } else {
          VerificationJobInstance verificationJobInstance =
              getVerificationJobInstanceBuilder(AmbianceUtils.obtainCurrentLevel(ambiance).getIdentifier(),
                  stepParameters, serviceEnvironmentParams, monitoredServiceDTO, deploymentStartTime)
                  .build();
          verificationJobInstanceId = verificationJobInstanceService.create(verificationJobInstance);
        }
        activity.setVerificationJobInstanceIds(Arrays.asList(verificationJobInstanceId));
        String activityId = activityService.upsert(activity);
        CVNGStepTask cvngStepTask = CVNGStepTask.builder()
                                        .accountId(serviceEnvironmentParams.getAccountIdentifier())
                                        .orgIdentifier(serviceEnvironmentParams.getOrgIdentifier())
                                        .projectIdentifier(serviceEnvironmentParams.getProjectIdentifier())
                                        .serviceIdentifier(serviceEnvironmentParams.getServiceIdentifier())
                                        .environmentIdentifier(serviceEnvironmentParams.getEnvironmentIdentifier())
                                        .status(CVNGStepTask.Status.IN_PROGRESS)
                                        .deploymentStartTime(deploymentStartTime)
                                        .activityId(activityId)
                                        .callbackId(verificationJobInstanceId)
                                        .verificationJobInstanceId(verificationJobInstanceId)
                                        .build();
        cvngStepTaskService.create(cvngStepTask);
        callbackId = verificationJobInstanceId;
      }
      return AsyncExecutableResponse.newBuilder().addCallbackIds(callbackId).build();
    }
  }

  private String callBackIdFromActivity(ServiceEnvironmentParams serviceEnvironmentParams, Ambiance ambiance,
      CVNGStepParameter stepParameters, MonitoredServiceDTO monitoredServiceDTO) {
    DeploymentActivity deploymentActivity =
        getDeploymentActivity(stepParameters, serviceEnvironmentParams, monitoredServiceDTO, ambiance,
            Instant.ofEpochMilli(
                AmbianceUtils.getStageLevelFromAmbiance(ambiance)
                    .orElseThrow(() -> new IllegalStateException("verify step needs to be part of a stage."))
                    .getStartTs()));
    String activityUuid;
    if (isDemoEnabled(AmbianceUtils.getAccountId(ambiance), ambiance)) {
      deploymentActivity.setVerificationStartTime(
          deploymentActivity.getVerificationStartTime().minus(Duration.ofMinutes(15)).toEpochMilli());
      deploymentActivity.setActivityStartTime(deploymentActivity.getActivityStartTime().minus(Duration.ofMinutes(15)));
      activityUuid = activityService.createActivityForDemo(deploymentActivity,
          isDev(ambiance) ? ActivityVerificationStatus.VERIFICATION_FAILED
                          : ActivityVerificationStatus.VERIFICATION_PASSED);
    } else {
      activityUuid = activityService.register(deploymentActivity);
    }
    Activity activity = activityService.get(activityUuid);
    CVNGStepTaskBuilder cvngStepTaskBuilder = CVNGStepTask.builder();
    cvngStepTaskBuilder.activityId(activityUuid)
        .callbackId(activityUuid)
        .verificationJobInstanceId(activity.getVerificationJobInstanceIds().get(0));
    CVNGStepTask cvngStepTask =
        cvngStepTaskBuilder.accountId(serviceEnvironmentParams.getAccountIdentifier())
            .orgIdentifier(serviceEnvironmentParams.getOrgIdentifier())
            .projectIdentifier(serviceEnvironmentParams.getProjectIdentifier())
            .serviceIdentifier(serviceEnvironmentParams.getServiceIdentifier())
            .environmentIdentifier(serviceEnvironmentParams.getEnvironmentIdentifier())
            .status(CVNGStepTask.Status.IN_PROGRESS)
            .deploymentStartTime(Instant.ofEpochMilli(
                AmbianceUtils.getStageLevelFromAmbiance(ambiance)
                    .orElseThrow(() -> new IllegalStateException("verify step needs to be part of a stage."))
                    .getStartTs()))
            .build();
    cvngStepTaskService.create(cvngStepTask);
    return cvngStepTask.getCallbackId();
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

  private VerificationJobInstanceBuilder getVerificationJobInstanceBuilder(String stepName,
      CVNGStepParameter stepParameters, ServiceEnvironmentParams serviceEnvironmentParams,
      MonitoredServiceDTO monitoredServiceDTO, Instant deploymentStartTime) {
    Instant verificationStartTime = clock.instant();
    VerificationJob verificationJob =
        stepParameters.getVerificationJobBuilder()
            .serviceIdentifier(RuntimeParameter.builder().value(stepParameters.getServiceIdentifier()).build())
            .envIdentifier(RuntimeParameter.builder().value(stepParameters.getEnvIdentifier()).build())
            .projectIdentifier(serviceEnvironmentParams.getProjectIdentifier())
            .orgIdentifier(serviceEnvironmentParams.getOrgIdentifier())
            .accountId(serviceEnvironmentParams.getAccountIdentifier())
            .monitoringSources(monitoredServiceDTO.getSources()
                                   .getHealthSources()
                                   .stream()
                                   .map(healthSource
                                       -> HealthSourceService.getNameSpacedIdentifier(
                                           monitoredServiceDTO.getIdentifier(), healthSource.getIdentifier()))
                                   .collect(Collectors.toList()))
            .build();
    VerificationJobInstanceBuilder verificationJobInstanceBuilder =
        fillOutCommonJobInstanceProperties(serviceEnvironmentParams.getAccountIdentifier(), deploymentStartTime,
            verificationJob.resolveAdditionsFields(verificationJobInstanceService), verificationStartTime);
    validateJob(verificationJob);
    verificationJobInstanceBuilder.name(stepName);
    return verificationJobInstanceBuilder;
  }

  private VerificationJobInstance getVerificationJobInstanceForDemo(String stepName, CVNGStepParameter stepParameters,
      ServiceEnvironmentParams serviceEnvironmentParams, MonitoredServiceDTO monitoredServiceDTO,
      Instant deploymentStartTime, ActivityVerificationStatus activityVerificationStatus) {
    VerificationJobInstanceBuilder verificationJobInstanceBuilder = getVerificationJobInstanceBuilder(
        stepName, stepParameters, serviceEnvironmentParams, monitoredServiceDTO, deploymentStartTime);
    verificationJobInstanceBuilder.verificationStatus(activityVerificationStatus);
    verificationJobInstanceBuilder.startTime(clock.instant().minus(Duration.ofMinutes(15)));
    verificationJobInstanceBuilder.deploymentStartTime(clock.instant().minus(Duration.ofMinutes(16)));
    return verificationJobInstanceBuilder.build();
  }

  private void validateJob(VerificationJob verificationJob) {
    List<CVConfig> cvConfigs = verificationJobInstanceService.getCVConfigsForVerificationJob(verificationJob);
    Preconditions.checkState(EmptyPredicate.isNotEmpty(cvConfigs),
        "No monitoring sources with identifiers %s defined for environment %s and service %s",
        verificationJob.getMonitoringSources(), verificationJob.getEnvIdentifier(),
        verificationJob.getServiceIdentifier());
  }

  @NotNull
  private DeploymentActivity getDeploymentActivity(CVNGStepParameter stepParameters,
      ServiceEnvironmentParams serviceEnvironmentParams, MonitoredServiceDTO monitoredServiceDTO, Ambiance ambiance,
      Instant activityStartTime) {
    Instant startTime = clock.instant();
    VerificationJob verificationJob =
        stepParameters.getVerificationJobBuilder()
            .serviceIdentifier(RuntimeParameter.builder().value(stepParameters.getServiceIdentifier()).build())
            .envIdentifier(RuntimeParameter.builder().value(stepParameters.getEnvIdentifier()).build())
            .projectIdentifier(serviceEnvironmentParams.getProjectIdentifier())
            .orgIdentifier(serviceEnvironmentParams.getOrgIdentifier())
            .accountId(serviceEnvironmentParams.getAccountIdentifier())
            .monitoringSources(monitoredServiceDTO.getSources()
                                   .getHealthSources()
                                   .stream()
                                   .map(healthSource
                                       -> HealthSourceService.getNameSpacedIdentifier(
                                           monitoredServiceDTO.getIdentifier(), healthSource.getIdentifier()))
                                   .collect(Collectors.toList()))
            .build();
    DeploymentActivity deploymentActivity =
        DeploymentActivity.builder()
            .deploymentTag(stepParameters.getDeploymentTag().getValue())
            .verificationStartTime(startTime.toEpochMilli())
            .stageId(AmbianceUtils.getStageLevelFromAmbiance(ambiance).get().getIdentifier())
            .stageStepId(AmbianceUtils.getStageLevelFromAmbiance(ambiance).get().getSetupId())
            .pipelineId(ambiance.getMetadata().getPipelineIdentifier())
            .planExecutionId(ambiance.getPlanExecutionId())
            .build();
    deploymentActivity.setVerificationJobs(Collections.singletonList(verificationJob));
    deploymentActivity.setActivityStartTime(activityStartTime);
    deploymentActivity.setOrgIdentifier(serviceEnvironmentParams.getOrgIdentifier());
    deploymentActivity.setAccountId(serviceEnvironmentParams.getAccountIdentifier());
    deploymentActivity.setProjectIdentifier(serviceEnvironmentParams.getProjectIdentifier());
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
    @Deprecated String activityId;
    String verifyStepExecutionId;
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
    @Deprecated String activityId;
    String verifyStepExecutionId;
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
                           .verifyStepExecutionId(cvngResponseData.verifyStepExecutionId)
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

  private VerificationJobInstanceBuilder fillOutCommonJobInstanceProperties(
      String accountId, Instant deploymentStartTime, VerificationJob verificationJob, Instant verficationStartTime) {
    return VerificationJobInstance.builder()
        .accountId(accountId)
        .executionStatus(ExecutionStatus.QUEUED)
        .deploymentStartTime(deploymentStartTime)
        .startTime(verficationStartTime)
        .resolvedJob(verificationJob);
  }
}
