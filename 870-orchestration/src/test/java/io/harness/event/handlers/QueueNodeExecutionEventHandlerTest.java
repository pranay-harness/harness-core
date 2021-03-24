package io.harness.event.handlers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.PmsNodeExecutionServiceImpl;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.events.QueueNodeExecutionRequest;
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

public class QueueNodeExecutionEventHandlerTest extends OrchestrationTestBase {
  @Mock PmsNodeExecutionServiceImpl pmsNodeExecutionService;

  @InjectMocks QueueNodeExecutionEventHandler queueNodeExecutionEventHandler;

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
    queueNodeExecutionEventHandler.handleEvent(
        SdkResponseEvent.builder()
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setQueueNodeExecutionRequest(QueueNodeExecutionRequest.newBuilder().build())
                                         .build())
            .sdkResponseEventType(SdkResponseEventType.QUEUE_NODE)
            .build());
    verify(pmsNodeExecutionService).queueNodeExecution(NodeExecutionProto.newBuilder().build());
  }
}