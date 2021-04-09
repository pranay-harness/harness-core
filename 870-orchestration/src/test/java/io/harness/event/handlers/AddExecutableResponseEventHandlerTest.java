package io.harness.event.handlers;

import static io.harness.rule.OwnerRule.SAHIL;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
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
public class AddExecutableResponseEventHandlerTest {
  @InjectMocks AddExecutableResponseEventHandler addExecutableResponseEventHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  @Ignore("This was a noop test will improve on this")
  public void testHandleEvent() {
    AddExecutableResponseRequest request = AddExecutableResponseRequest.newBuilder().build();
    addExecutableResponseEventHandler.handleEvent(
        SdkResponseEventInternal.builder()
            .sdkResponseEventRequest(
                SdkResponseEventRequest.newBuilder().setAddExecutableResponseRequest(request).build())
            .sdkResponseEventType(SdkResponseEventType.QUEUE_NODE)
            .build());
  }
}