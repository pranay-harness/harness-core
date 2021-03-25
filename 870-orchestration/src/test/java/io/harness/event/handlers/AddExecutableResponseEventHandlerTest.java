package io.harness.event.handlers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.PmsNodeExecutionServiceImpl;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
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

public class AddExecutableResponseEventHandlerTest {
  @Mock PmsNodeExecutionServiceImpl pmsNodeExecutionService;

  @InjectMocks AddExecutableResponseEventHandler addExecutableResponseEventHandler;

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
    AddExecutableResponseRequest request = AddExecutableResponseRequest.newBuilder().build();
    addExecutableResponseEventHandler.handleEvent(
        SdkResponseEvent.builder()
            .sdkResponseEventRequest(
                SdkResponseEventRequest.newBuilder().setAddExecutableResponseRequest(request).build())
            .sdkResponseEventType(SdkResponseEventType.QUEUE_NODE)
            .build());
    verify(pmsNodeExecutionService)
        .addExecutableResponse(request.getNodeExecutionId(), request.getStatus(), request.getExecutableResponse(),
            request.getCallbackIdsList());
  }
}