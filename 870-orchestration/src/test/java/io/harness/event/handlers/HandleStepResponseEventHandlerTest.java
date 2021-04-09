package io.harness.event.handlers;

import static io.harness.rule.OwnerRule.SAHIL;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.events.HandleStepResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.pms.execution.SdkResponseEventInternal;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class HandleStepResponseEventHandlerTest extends OrchestrationTestBase {
  @InjectMocks HandleStepResponseEventHandler handleStepResponseEventHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  @Ignore("This was a noop test will improve on this")
  public void testHandleEvent() {
    HandleStepResponseRequest handleStepResponseRequest = HandleStepResponseRequest.newBuilder().build();
    handleStepResponseEventHandler.handleEvent(
        SdkResponseEventInternal.builder()
            .sdkResponseEventRequest(
                SdkResponseEventRequest.newBuilder().setHandleStepResponseRequest(handleStepResponseRequest).build())
            .sdkResponseEventType(SdkResponseEventType.HANDLE_STEP_RESPONSE)
            .build());
  }
}