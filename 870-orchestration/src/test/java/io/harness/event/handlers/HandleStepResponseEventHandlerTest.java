package io.harness.event.handlers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.PmsNodeExecutionServiceImpl;
import io.harness.pms.contracts.execution.events.HandleStepResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.rule.Owner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HandleStepResponseEventHandlerTest extends OrchestrationTestBase {
  @Mock PmsNodeExecutionServiceImpl pmsNodeExecutionService;

  @InjectMocks HandleStepResponseEventHandler handleStepResponseEventHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void verifyInteractions() {
    verifyNoMoreInteractions(pmsNodeExecutionService);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEvent() {
    HandleStepResponseRequest handleStepResponseRequest = HandleStepResponseRequest.newBuilder().build();
    handleStepResponseEventHandler.handleEvent(
        SdkResponseEvent.builder()
            .sdkResponseEventRequest(
                SdkResponseEventRequest.newBuilder().setHandleStepResponseRequest(handleStepResponseRequest).build())
            .sdkResponseEventType(SdkResponseEventType.HANDLE_STEP_RESPONSE)
            .build());
    verify(pmsNodeExecutionService)
        .handleStepResponse(
            handleStepResponseRequest.getNodeExecutionId(), handleStepResponseRequest.getStepResponse());
  }
}