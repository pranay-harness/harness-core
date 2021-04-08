package io.harness.event.handlers;

import static io.harness.rule.OwnerRule.SAHIL;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.events.QueueNodeExecutionRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class QueueNodeExecutionEventHandlerTest extends OrchestrationTestBase {
  @InjectMocks QueueNodeExecutionEventHandler queueNodeExecutionEventHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  @Ignore("This was a noop test will improve on this")
  public void testHandleEvent() {
    queueNodeExecutionEventHandler.handleEvent(
        SdkResponseEvent.builder()
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder()
                                         .setQueueNodeExecutionRequest(QueueNodeExecutionRequest.newBuilder().build())
                                         .build())
            .sdkResponseEventType(SdkResponseEventType.QUEUE_NODE)
            .build());
  }
}