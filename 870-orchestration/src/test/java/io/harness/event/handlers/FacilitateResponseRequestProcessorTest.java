package io.harness.event.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.events.FacilitatorResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.rule.Owner;
import io.harness.tasks.BinaryResponseData;
import io.harness.waiter.WaitNotifyEngine;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class FacilitateResponseRequestProcessorTest extends CategoryTest {
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @Mock private io.harness.engine.OrchestrationEngine orchestrationEngine;
  @InjectMocks private FacilitateResponseRequestProcessor facilitateResponseRequestHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void verifyInteractions() {
    Mockito.verifyNoMoreInteractions(waitNotifyEngine);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  @Ignore("Modify it to use orchestrationEngine inplace of waitEngine")
  public void testHandleAdviseEvent() {
    String nodeExecutionId = generateUuid();
    FacilitatorResponseRequest request =
        FacilitatorResponseRequest.newBuilder()
            .setFacilitatorResponse(FacilitatorResponseProto.newBuilder().setExecutionMode(ExecutionMode.TASK).build())
            .build();
    SdkResponseEventProto sdkResponseEventInternal = SdkResponseEventProto.newBuilder()
                                                         .setFacilitatorResponseRequest(request)
                                                         .setNodeExecutionId(nodeExecutionId)
                                                         .build();
    facilitateResponseRequestHandler.handleEvent(sdkResponseEventInternal);
    verify(waitNotifyEngine)
        .doneWith(request.getNotifyId(),
            BinaryResponseData.builder().data(request.getFacilitatorResponse().toByteArray()).build());
  }
}
