package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.Status.ASYNC_WAITING;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutableResponse.ResponseCase;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.execution.AsyncSdkProgressCallback;
import io.harness.pms.sdk.core.execution.AsyncSdkResumeCallback;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.supporter.async.TestAsyncStep;
import io.harness.pms.sdk.core.supporter.async.TestStepParameters;
import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.rule.Owner;
import io.harness.waiter.StringNotifyResponseData;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class AsyncStrategyTest extends PmsSdkCoreTestBase {
  @Mock private SdkNodeExecutionService sdkNodeExecutionService;
  @Mock private AsyncWaitEngine asyncWaitEngine;
  @Inject @InjectMocks private AsyncStrategy asyncStrategy;

  @Inject private StepRegistry stepRegistry;

  @Before
  public void setup() {
    stepRegistry.register(TestAsyncStep.ASYNC_STEP_TYPE, new TestAsyncStep());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestStart() {
    Ambiance ambiance = Ambiance.newBuilder()
                            .putAllSetupAbstractions(setupAbstractions())
                            .setPlanId(generateUuid())
                            .addLevels(Level.newBuilder()
                                           .setSetupId(generateUuid())
                                           .setRuntimeId(generateUuid())
                                           .setStepType(TestAsyncStep.ASYNC_STEP_TYPE)
                                           .setIdentifier(generateUuid())
                                           .build())
                            .build();
    InvokerPackage invokerPackage = InvokerPackage.builder()
                                        .ambiance(ambiance)
                                        .executionMode(ExecutionMode.ASYNC)
                                        .passThroughData(null)
                                        .stepParameters(TestStepParameters.builder().param("TEST_PARAM").build())
                                        .build();

    ArgumentCaptor<String> planExecutionIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> nodeExecutionIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
    ArgumentCaptor<ExecutableResponse> responseArgumentCaptor = ArgumentCaptor.forClass(ExecutableResponse.class);

    asyncStrategy.start(invokerPackage);
    Mockito.verify(sdkNodeExecutionService, Mockito.times(1))
        .addExecutableResponse(planExecutionIdCaptor.capture(), nodeExecutionIdCaptor.capture(), statusCaptor.capture(),
            responseArgumentCaptor.capture());

    ArgumentCaptor<AsyncSdkResumeCallback> notifyCallbackArgumentCaptor =
        ArgumentCaptor.forClass(AsyncSdkResumeCallback.class);
    ArgumentCaptor<AsyncSdkProgressCallback> progressCallbackArgumentCaptor =
        ArgumentCaptor.forClass(AsyncSdkProgressCallback.class);
    ArgumentCaptor<String> correlationIdsCaptor = ArgumentCaptor.forClass(String.class);

    Mockito.verify(asyncWaitEngine, Mockito.times(1))
        .waitForAllOn(notifyCallbackArgumentCaptor.capture(), progressCallbackArgumentCaptor.capture(),
            correlationIdsCaptor.capture());
    // Wait Engine Mock verify interactions
    AsyncSdkResumeCallback resumeCallback = notifyCallbackArgumentCaptor.getValue();
    assertThat(resumeCallback.getNodeExecutionId()).isEqualTo(AmbianceUtils.obtainCurrentRuntimeId(ambiance));

    AsyncSdkProgressCallback progressCallback = progressCallbackArgumentCaptor.getValue();
    assertThat(progressCallback.getAmbianceBytes()).isEqualTo(ambiance.toByteArray());
    assertThat(progressCallback.getStepParameters())
        .isEqualTo(ByteString.copyFromUtf8(RecastOrchestrationUtils.toDocumentJson(invokerPackage.getStepParameters()))
                       .toByteArray());

    assertThat(nodeExecutionIdCaptor.getValue()).isEqualTo(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    assertThat(statusCaptor.getValue()).isEqualTo(ASYNC_WAITING);

    ExecutableResponse executableResponse = responseArgumentCaptor.getValue();
    assertThat(executableResponse.getResponseCase()).isEqualTo(ResponseCase.ASYNC);

    AsyncExecutableResponse asyncExecutableResponse = executableResponse.getAsync();
    assertThat(asyncExecutableResponse.getCallbackIdsList()).hasSize(1);
    String callbackId = asyncExecutableResponse.getCallbackIdsList().get(0);
    String corrId = correlationIdsCaptor.getValue();
    assertThat(callbackId).isEqualTo(corrId);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestResume() {
    Ambiance ambiance = Ambiance.newBuilder()
                            .putAllSetupAbstractions(setupAbstractions())
                            .setPlanId(generateUuid())
                            .setPlanExecutionId(generateUuid())
                            .addLevels(Level.newBuilder()
                                           .setSetupId(generateUuid())
                                           .setRuntimeId(generateUuid())
                                           .setStepType(TestAsyncStep.ASYNC_STEP_TYPE)
                                           .setIdentifier(generateUuid())
                                           .build())
                            .build();
    ResumePackage resumePackage = ResumePackage.builder()
                                      .ambiance(ambiance)
                                      .stepParameters(TestStepParameters.builder().param("TEST_PARAM").build())
                                      .responseDataMap(ImmutableMap.of(generateUuid(),
                                          StringNotifyResponseData.builder().data("someString").build()))
                                      .build();

    ArgumentCaptor<String> planExecutionIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> nodeExecutionIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<StepResponseProto> stepResponseCaptor = ArgumentCaptor.forClass(StepResponseProto.class);
    asyncStrategy.resume(resumePackage);
    Mockito.verify(sdkNodeExecutionService, Mockito.times(1))
        .handleStepResponse(
            planExecutionIdCaptor.capture(), nodeExecutionIdCaptor.capture(), stepResponseCaptor.capture());

    assertThat(nodeExecutionIdCaptor.getValue()).isEqualTo(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    assertThat(planExecutionIdCaptor.getValue()).isEqualTo(ambiance.getPlanExecutionId());
  }
  private Map<String, String> setupAbstractions() {
    return ImmutableMap.<String, String>builder()
        .put(SetupAbstractionKeys.accountId, generateUuid())
        .put(SetupAbstractionKeys.orgIdentifier, generateUuid())
        .put(SetupAbstractionKeys.projectIdentifier, generateUuid())
        .build();
  }
}