package io.harness.event.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.OrchestrationEngine;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.EndPlanAdvise;
import io.harness.pms.contracts.execution.events.AdviserResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.rule.Owner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class AdviserResponseRequestProcessorTest {
  @Mock private OrchestrationEngine orchestrationEngine;
  @InjectMocks private AdviserResponseRequestProcessor adviserEventResponseHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void verifyInteractions() {
    Mockito.verifyNoMoreInteractions(orchestrationEngine);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleAdviseEvent() {
    String nodeExecutionId = generateUuid();
    AdviserResponseRequest request = AdviserResponseRequest.newBuilder()
                                         .setNodeExecutionId(nodeExecutionId)
                                         .setAdviserResponse(AdviserResponse.newBuilder()
                                                                 .setType(AdviseType.END_PLAN)
                                                                 .setEndPlanAdvise(EndPlanAdvise.newBuilder().build())
                                                                 .build())
                                         .build();
    SdkResponseEventProto sdkResponseEventInternal =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                            .setAdviserResponseRequest(request)
                                            .setNodeExecutionId(nodeExecutionId)
                                            .build())
            .build();
    adviserEventResponseHandler.handleEvent(sdkResponseEventInternal);

    ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<AdviserResponse> responseCaptor = ArgumentCaptor.forClass(AdviserResponse.class);
    verify(orchestrationEngine).handleAdvise(idCaptor.capture(), responseCaptor.capture());

    assertThat(idCaptor.getValue()).isEqualTo(nodeExecutionId);
    assertThat(responseCaptor.getValue().getType()).isEqualTo(AdviseType.END_PLAN);
  }
}
