package io.harness.event.handlers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.EndPlanAdvise;
import io.harness.pms.contracts.execution.events.AdviserResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
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
public class AdviserEventSdkResponseHandlerTest {
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @InjectMocks private AdviserEventResponseHandler adviserEventResponseHandler;

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
    AdviserResponseRequest request = AdviserResponseRequest.newBuilder()
                                         .setAdviserResponse(AdviserResponse.newBuilder()
                                                                 .setType(AdviseType.END_PLAN)
                                                                 .setEndPlanAdvise(EndPlanAdvise.newBuilder().build())
                                                                 .build())
                                         .build();
    SdkResponseEventProto sdkResponseEventInternal =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventRequest(SdkResponseEventRequest.newBuilder().setAdviserResponseRequest(request).build())
            .build();
    adviserEventResponseHandler.handleEvent(sdkResponseEventInternal);
    verify(waitNotifyEngine)
        .doneWith(request.getNotifyId(),
            BinaryResponseData.builder().data(request.getAdviserResponse().toByteArray()).build());
  }
}
