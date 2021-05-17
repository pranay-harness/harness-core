package io.harness.event.handlers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.events.FacilitatorResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.rule.Owner;
import io.harness.tasks.BinaryResponseData;
import io.harness.waiter.WaitNotifyEngine;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class FacilitateEventSdkResponseHandlerTest {
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @InjectMocks private FacilitateResponseRequestHandler facilitateResponseRequestHandler;

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
  public void testHandleAdviseEvent() {
    FacilitatorResponseRequest request =
        FacilitatorResponseRequest.newBuilder()
            .setFacilitatorResponse(FacilitatorResponseProto.newBuilder().setExecutionMode(ExecutionMode.TASK).build())
            .build();
    SdkResponseEvent sdkResponseEventInternal =
        SdkResponseEvent.builder()
            .sdkResponseEventRequest(
                SdkResponseEventRequest.newBuilder().setFacilitatorResponseRequest(request).build())
            .build();
    facilitateResponseRequestHandler.handleEvent(sdkResponseEventInternal);
    verify(waitNotifyEngine)
        .doneWith(request.getNotifyId(),
            BinaryResponseData.builder().data(request.getFacilitatorResponse().toByteArray()).build());
  }
}
