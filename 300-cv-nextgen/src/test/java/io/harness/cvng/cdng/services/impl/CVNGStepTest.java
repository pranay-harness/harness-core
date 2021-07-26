package io.harness.cvng.cdng.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.cdng.beans.CVNGStepParameter;
import io.harness.cvng.cdng.beans.TestVerificationJobSpec;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.entities.CVNGStepTask.CVNGStepTaskKeys;
import io.harness.cvng.cdng.services.impl.CVNGStep.VerifyStepOutcome;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob.RuntimeParameter;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobBuilder;
import io.harness.eraro.ErrorCode;
import io.harness.persistence.HPersistence;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CVNGStepTest extends CvNextGenTestBase {
  private CVNGStep cvngStep;
  @Inject private Injector injector;
  @Inject private HPersistence hPersistence;
  @Inject private MonitoredServiceService monitoredServiceService;
  @Inject private MetricPackService metricPackService;
  private BuilderFactory builderFactory;
  private String accountId;
  private String projectIdentifier;
  private String orgIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;
  private MonitoredServiceDTO monitoredServiceDTO;

  @Before
  public void setup() {
    cvngStep = new CVNGStep();
    injector.injectMembers(cvngStep);
    builderFactory = BuilderFactory.getDefault();
    accountId = builderFactory.getContext().getAccountId();
    projectIdentifier = builderFactory.getContext().getProjectIdentifier();
    orgIdentifier = builderFactory.getContext().getOrgIdentifier();
    serviceIdentifier = builderFactory.getContext().getServiceIdentifier();
    envIdentifier = builderFactory.getContext().getEnvIdentifier();
    monitoredServiceDTO = builderFactory.monitoredServiceDTOBuilder().build();
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecuteAsync_noMonitoringSourceDefined() {
    Ambiance ambiance = getAmbiance();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    CVNGStepParameter cvngStepParameter = getCvngStepParameter();
    AsyncExecutableResponse asyncExecutableResponse =
        cvngStep.executeAsync(ambiance, cvngStepParameter, stepInputPackage, null);
    assertThat(asyncExecutableResponse.getCallbackIdsList()).hasSize(1);
    String callbackId = asyncExecutableResponse.getCallbackIds(0);
    CVNGStepTask cvngStepTask =
        hPersistence.createQuery(CVNGStepTask.class).filter(CVNGStepTaskKeys.callbackId, callbackId).get();
    assertThat(cvngStepTask.getStatus()).isEqualTo(CVNGStepTask.Status.IN_PROGRESS);
    assertThat(cvngStepTask.isSkip()).isTrue();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecuteAsync_skipWhenMonitoredServiceRefIsEmpty() {
    Ambiance ambiance = getAmbiance();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    CVNGStepParameter cvngStepParameter = getCvngStepParameter();
    cvngStepParameter.setMonitoredServiceRef(ParameterField.createValueField(""));
    AsyncExecutableResponse asyncExecutableResponse =
        cvngStep.executeAsync(ambiance, cvngStepParameter, stepInputPackage, null);
    assertThat(asyncExecutableResponse.getCallbackIdsList()).hasSize(1);
    String callbackId = asyncExecutableResponse.getCallbackIds(0);
    CVNGStepTask cvngStepTask =
        hPersistence.createQuery(CVNGStepTask.class).filter(CVNGStepTaskKeys.callbackId, callbackId).get();
    assertThat(cvngStepTask.getStatus()).isEqualTo(CVNGStepTask.Status.IN_PROGRESS);
    assertThat(cvngStepTask.isSkip()).isTrue();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecuteAsync_createActivity() {
    Ambiance ambiance = getAmbiance();
    metricPackService.createDefaultMetricPackAndThresholds(accountId, orgIdentifier, projectIdentifier);
    monitoredServiceService.create(accountId, monitoredServiceDTO);
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    CVNGStepParameter cvngStepParameter = getCvngStepParameter();
    AsyncExecutableResponse asyncExecutableResponse =
        cvngStep.executeAsync(ambiance, cvngStepParameter, stepInputPackage, null);
    assertThat(asyncExecutableResponse.getCallbackIdsList()).hasSize(1);
    String activityId = asyncExecutableResponse.getCallbackIds(0);
    CVNGStepTask cvngStepTask =
        hPersistence.createQuery(CVNGStepTask.class).filter(CVNGStepTaskKeys.activityId, activityId).get();
    assertThat(cvngStepTask.getStatus()).isEqualTo(CVNGStepTask.Status.IN_PROGRESS);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecuteAsync_monitoringServiceRefDoesNotMatch() {
    Ambiance ambiance = getAmbiance();
    metricPackService.createDefaultMetricPackAndThresholds(accountId, orgIdentifier, projectIdentifier);
    monitoredServiceService.create(accountId, monitoredServiceDTO);
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    CVNGStepParameter cvngStepParameter = getCvngStepParameter();
    cvngStepParameter.setMonitoredServiceRef(ParameterField.createValueField("monitoredService"));
    assertThatThrownBy(() -> cvngStep.executeAsync(ambiance, cvngStepParameter, stepInputPackage, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Invalid monitored service identifier for service %s and env %s", serviceIdentifier, envIdentifier);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse() {
    Ambiance ambiance = getAmbiance();
    CVNGStepParameter cvngStepParameter = getCvngStepParameter();
    String activityId = generateUuid();
    ActivityStatusDTO activityStatusDTO = ActivityStatusDTO.builder()
                                              .activityId(activityId)
                                              .status(ActivityVerificationStatus.VERIFICATION_PASSED)
                                              .progressPercentage(100)
                                              .remainingTimeMs(0)
                                              .durationMs(Duration.ofMinutes(30).toMillis())
                                              .build();
    StepResponse stepResponse = cvngStep.handleAsyncResponse(ambiance, cvngStepParameter,
        Collections.singletonMap(activityId,
            CVNGStep.CVNGResponseData.builder().activityId(activityId).activityStatusDTO(activityStatusDTO).build()));
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(stepResponse.getFailureInfo()).isNull();
    StepOutcome stepOutcome = StepOutcome.builder()
                                  .name("output")
                                  .outcome(VerifyStepOutcome.builder()
                                               .progressPercentage(100)
                                               .estimatedRemainingTime("0 minutes")
                                               .activityId(activityId)
                                               .build())
                                  .build();
    assertThat(stepResponse.getStepOutcomes()).isEqualTo(Collections.singletonList(stepOutcome));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_verificationFailure() {
    Ambiance ambiance = getAmbiance();
    CVNGStepParameter cvngStepParameter = getCvngStepParameter();
    String activityId = generateUuid();
    ActivityStatusDTO activityStatusDTO = ActivityStatusDTO.builder()
                                              .activityId(activityId)
                                              .status(ActivityVerificationStatus.VERIFICATION_FAILED)
                                              .progressPercentage(100)
                                              .remainingTimeMs(0)
                                              .durationMs(Duration.ofMinutes(30).toMillis())
                                              .build();
    StepResponse stepResponse = cvngStep.handleAsyncResponse(ambiance, cvngStepParameter,
        Collections.singletonMap(activityId,
            CVNGStep.CVNGResponseData.builder().activityId(activityId).activityStatusDTO(activityStatusDTO).build()));
    assertThat(stepResponse.getStatus()).isEqualTo(Status.FAILED);
    assertThat(stepResponse.getFailureInfo())
        .isEqualTo(FailureInfo.newBuilder()
                       .addFailureData(FailureData.newBuilder()
                                           .setCode(ErrorCode.DEFAULT_ERROR_CODE.name())
                                           .setLevel(io.harness.eraro.Level.ERROR.name())
                                           .addFailureTypes(FailureType.VERIFICATION_FAILURE)
                                           .setMessage("Verification failed")
                                           .build())
                       .build());
    StepOutcome stepOutcome = StepOutcome.builder()
                                  .name("output")
                                  .outcome(VerifyStepOutcome.builder()
                                               .progressPercentage(100)
                                               .estimatedRemainingTime("0 minutes")
                                               .activityId(activityId)
                                               .build())
                                  .build();
    assertThat(stepResponse.getStepOutcomes()).isEqualTo(Collections.singletonList(stepOutcome));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_inProgress() {
    Ambiance ambiance = getAmbiance();
    CVNGStepParameter cvngStepParameter = getCvngStepParameter();
    String activityId = generateUuid();
    ActivityStatusDTO activityStatusDTO = ActivityStatusDTO.builder()
                                              .activityId(activityId)
                                              .status(ActivityVerificationStatus.IN_PROGRESS)
                                              .progressPercentage(50)
                                              .remainingTimeMs(Duration.ofMinutes(3).toMillis())
                                              .durationMs(Duration.ofMinutes(30).toMillis())
                                              .build();
    assertThatThrownBy(()
                           -> cvngStep.handleAsyncResponse(ambiance, cvngStepParameter,
                               Collections.singletonMap(activityId,
                                   CVNGStep.CVNGResponseData.builder()
                                       .activityId(activityId)
                                       .activityStatusDTO(activityStatusDTO)
                                       .build())))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_skip() {
    Ambiance ambiance = getAmbiance();
    CVNGStepParameter cvngStepParameter = getCvngStepParameter();
    String activityId = generateUuid();
    StepResponse stepResponse = cvngStep.handleAsyncResponse(ambiance, cvngStepParameter,
        Collections.singletonMap(activityId, CVNGStep.CVNGResponseData.builder().skip(true).build()));
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SKIPPED);
    assertThat(stepResponse.getFailureInfo())
        .isEqualTo(FailureInfo.newBuilder()
                       .addFailureData(FailureData.newBuilder()
                                           .setCode(ErrorCode.UNKNOWN_ERROR.name())
                                           .setLevel(io.harness.eraro.Level.INFO.name())
                                           .addFailureTypes(FailureType.SKIPPING_FAILURE)
                                           .setMessage("No monitoredServiceRef is defined for service "
                                               + serviceIdentifier + " and env " + envIdentifier + "")
                                           .build())
                       .build());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleAsyncResponse_error() {
    Ambiance ambiance = getAmbiance();
    CVNGStepParameter cvngStepParameter = getCvngStepParameter();
    String activityId = generateUuid();
    ActivityStatusDTO activityStatusDTO = ActivityStatusDTO.builder()
                                              .activityId(activityId)
                                              .status(ActivityVerificationStatus.ERROR)
                                              .progressPercentage(50)
                                              .remainingTimeMs(Duration.ofMinutes(3).toMillis())
                                              .durationMs(Duration.ofMinutes(30).toMillis())
                                              .build();
    StepResponse stepResponse = cvngStep.handleAsyncResponse(ambiance, cvngStepParameter,
        Collections.singletonMap(activityId,
            CVNGStep.CVNGResponseData.builder().activityId(activityId).activityStatusDTO(activityStatusDTO).build()));
    assertThat(stepResponse.getStatus()).isEqualTo(Status.ERRORED);
    assertThat(stepResponse.getFailureInfo())
        .isEqualTo(FailureInfo.newBuilder()
                       .addFailureData(FailureData.newBuilder()
                                           .setCode(ErrorCode.UNKNOWN_ERROR.name())
                                           .setLevel(io.harness.eraro.Level.ERROR.name())
                                           .addFailureTypes(FailureType.UNKNOWN_FAILURE)
                                           .setMessage("Verification could not complete due to an unknown error")
                                           .build())
                       .build());
    StepOutcome stepOutcome = StepOutcome.builder()
                                  .name("output")
                                  .outcome(VerifyStepOutcome.builder()
                                               .progressPercentage(50)
                                               .estimatedRemainingTime("3 minutes")
                                               .activityId(activityId)
                                               .build())
                                  .build();
    assertThat(stepResponse.getStepOutcomes()).isEqualTo(Collections.singletonList(stepOutcome));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testHandleProgress() {
    Ambiance ambiance = getAmbiance();
    CVNGStepParameter cvngStepParameter = getCvngStepParameter();
    String activityId = generateUuid();
    ActivityStatusDTO activityStatusDTO = ActivityStatusDTO.builder()
                                              .activityId(activityId)
                                              .status(ActivityVerificationStatus.ERROR)
                                              .progressPercentage(50)
                                              .remainingTimeMs(Duration.ofMinutes(3).toMillis())
                                              .durationMs(Duration.ofMinutes(30).toMillis())
                                              .build();
    VerifyStepOutcome verifyStepOutcome = (VerifyStepOutcome) cvngStep.handleProgress(ambiance, cvngStepParameter,
        CVNGStep.CVNGResponseData.builder().activityId(activityId).activityStatusDTO(activityStatusDTO).build());
    VerifyStepOutcome expected = VerifyStepOutcome.builder()
                                     .progressPercentage(50)
                                     .estimatedRemainingTime("3 minutes")
                                     .activityId(activityId)
                                     .build();
    assertThat(verifyStepOutcome).isEqualTo(expected);
  }

  private CVNGStepParameter getCvngStepParameter() {
    TestVerificationJobSpec spec = TestVerificationJobSpec.builder()
                                       .deploymentTag(randomParameter())
                                       .duration(ParameterField.<String>builder().value("5m").build())
                                       .sensitivity(ParameterField.<String>builder().value("High").build())
                                       .build();
    return CVNGStepParameter.builder()
        .serviceIdentifier(ParameterField.createValueField(serviceIdentifier))
        .envIdentifier(ParameterField.createValueField(envIdentifier))
        .verificationJobBuilder(getVerificationJobBuilder())
        .deploymentTag(spec.getDeploymentTag())
        .monitoredServiceRef(ParameterField.createValueField(monitoredServiceDTO.getIdentifier()))
        .build();
  }

  private Ambiance getAmbiance() {
    String uuid = generateUuid();
    HashMap<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("accountId", accountId);
    setupAbstractions.put("projectIdentifier", projectIdentifier);
    setupAbstractions.put("orgIdentifier", orgIdentifier);
    return Ambiance.newBuilder()
        .addAllLevels(Collections.singletonList(Level.newBuilder().setRuntimeId(uuid).build()))
        .setPlanExecutionId(generateUuid())
        .putAllSetupAbstractions(setupAbstractions)
        .build();
  }
  private VerificationJobBuilder getVerificationJobBuilder() {
    return TestVerificationJob.builder()
        .sensitivity(RuntimeParameter.builder().value("Low").build())
        .duration(RuntimeParameter.builder().value("5m").build());
  }

  private ParameterField<String> randomParameter() {
    return ParameterField.<String>builder().value(generateUuid()).build();
  }
}