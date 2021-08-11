package io.harness.pms.sdk.core.execution.events.node.resume;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.resume.ChainDetails;
import io.harness.pms.contracts.resume.NodeResumeEvent;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.AmbianceTestUtils;
import io.harness.pms.sdk.core.DummyExecutionStrategy;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.ExecutableProcessor;
import io.harness.pms.sdk.core.execution.ExecutableProcessorFactory;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

@OwnedBy(HarnessTeam.PIPELINE)
public class NodeResumeEventHandlerTest extends PmsSdkCoreTestBase {
  @Mock ExecutableProcessorFactory executableProcessorFactory;
  @Mock EngineObtainmentHelper engineObtainmentHelper;
  @InjectMocks NodeResumeEventHandler nodeResumeEventHandler;
  @Mock SdkNodeExecutionService sdkNodeExecutionService;
  @Mock KryoSerializer mockedKryoSerializer;
  @Inject KryoSerializer kryoSerializer1;

  private NodeResumeEvent nodeResumeEvent;
  private Ambiance ambiance;

  @Before
  public void setup() {
    when(executableProcessorFactory.obtainProcessor(ExecutionMode.APPROVAL))
        .thenReturn(new ExecutableProcessor(new DummyExecutionStrategy()));

    ambiance = AmbianceTestUtils.buildAmbiance();
    Map<String, ByteString> responseDataMap = new HashMap<>();
    StepResponseNotifyData stepResponseNotifyData = StepResponseNotifyData.builder().build();
    ByteString byteString =
        ByteString.copyFrom(kryoSerializer1.asDeflatedBytes(StepResponseNotifyData.builder().build()));

    responseDataMap.put("response", byteString);
    when(mockedKryoSerializer.asInflatedObject(byteString.toByteArray())).thenReturn(stepResponseNotifyData);

    nodeResumeEvent = NodeResumeEvent.newBuilder()
                          .setExecutionMode(ExecutionMode.APPROVAL)
                          .setAmbiance(ambiance)
                          .setAsyncError(false)
                          .putAllResponse(responseDataMap)
                          .build();
  }

  public void verifyInteractions() {
    Mockito.verifyNoMoreInteractions(executableProcessorFactory);
    Mockito.verifyNoMoreInteractions(sdkNodeExecutionService);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExtractMetricContext() {
    Map<String, String> metricsMap = nodeResumeEventHandler.extractMetricContext(nodeResumeEvent);
    assertThat(metricsMap.isEmpty()).isFalse();
    assertThat(metricsMap.size()).isEqualTo(3);
    assertThat(metricsMap.get("accountId")).isEqualTo(AmbianceTestUtils.ACCOUNT_ID);
    assertThat(metricsMap.get("orgIdentifier")).isEqualTo(AmbianceTestUtils.ORG_ID);
    assertThat(metricsMap.get("projectIdentifier")).isEqualTo(AmbianceTestUtils.PROJECT_ID);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testMetricPrefix() {
    assertThat(nodeResumeEventHandler.getMetricPrefix(nodeResumeEvent)).isEqualTo("resume_event");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExtractLogProperties() {
    Map<String, String> metricsMap = nodeResumeEventHandler.extraLogProperties(nodeResumeEvent);
    assertThat(metricsMap.isEmpty()).isFalse();
    assertThat(metricsMap.size()).isEqualTo(1);
    assertThat(metricsMap.get("eventType")).isEqualTo(NodeExecutionEventType.RESUME.name());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExtractAmbiance() {
    assertThat(nodeResumeEventHandler.extractAmbiance(nodeResumeEvent)).isEqualTo(ambiance);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEventWithContext() {
    nodeResumeEventHandler.handleEventWithContext(nodeResumeEvent);
    Mockito.verify(executableProcessorFactory).obtainProcessor(ExecutionMode.APPROVAL);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testCalculateIsEndOnApproval() {
    nodeResumeEvent =
        nodeResumeEvent.toBuilder().setChainDetails(ChainDetails.newBuilder().setIsEnd(true).build()).build();
    assertThat(nodeResumeEventHandler.calculateIsEnd(nodeResumeEvent, new HashMap<>())).isTrue();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testCalculateIsEndOnChildCHain() {
    nodeResumeEvent = nodeResumeEvent.toBuilder()
                          .setExecutionMode(ExecutionMode.CHILD_CHAIN)
                          .setChainDetails(ChainDetails.newBuilder().setIsEnd(false).build())
                          .build();
    Map<String, ResponseData> responseDataMap = new HashMap<>();
    responseDataMap.put("response", StepResponseNotifyData.builder().build());
    assertThat(nodeResumeEventHandler.calculateIsEnd(nodeResumeEvent, responseDataMap)).isFalse();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEventWithContextWithAsyncError() {
    Map<String, ByteString> responseDataMap = new HashMap<>();
    DummyErrorResponseData errorResponseData = DummyErrorResponseData.builder().build();
    ByteString byteString = ByteString.copyFrom(kryoSerializer1.asDeflatedBytes(errorResponseData));

    responseDataMap.put("response", byteString);
    when(mockedKryoSerializer.asInflatedObject(byteString.toByteArray())).thenReturn(errorResponseData);

    nodeResumeEvent = nodeResumeEvent.toBuilder().setAsyncError(true).putAllResponse(responseDataMap).build();
    nodeResumeEventHandler.handleEventWithContext(nodeResumeEvent);
    Mockito.verify(executableProcessorFactory).obtainProcessor(ExecutionMode.APPROVAL);
    Mockito.verify(sdkNodeExecutionService)
        .handleStepResponse(eq(nodeResumeEvent.getAmbiance().getPlanExecutionId()),
            eq(AmbianceUtils.obtainCurrentRuntimeId(nodeResumeEvent.getAmbiance())), any(StepResponseProto.class));
  }
}