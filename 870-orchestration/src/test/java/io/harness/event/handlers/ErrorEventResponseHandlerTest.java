package io.harness.event.handlers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.events.EventErrorRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.rule.Owner;
import io.harness.tasks.FailureResponseData;
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
public class ErrorEventResponseHandlerTest {
  @Mock private WaitNotifyEngine waitNotifyEngine;
  @InjectMocks private ErrorEventResponseHandler errorEventResponseHandler;

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
    EventErrorRequest request = EventErrorRequest.newBuilder()
                                    .setFailureInfo(FailureInfo.newBuilder()
                                                        .setErrorMessage("message")
                                                        .addFailureTypes(FailureType.AUTHENTICATION_FAILURE)
                                                        .build())
                                    .build();
    SdkResponseEvent sdkResponseEventInternal =
        SdkResponseEvent.builder()
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder().setEventErrorRequest(request).build())
            .build();
    errorEventResponseHandler.handleEvent(sdkResponseEventInternal);
    verify(waitNotifyEngine)
        .doneWith(request.getEventNotifyId(),
            FailureResponseData.builder()
                .errorMessage(request.getFailureInfo().getErrorMessage())
                .failureTypes(
                    EngineExceptionUtils.transformToWingsFailureTypes(request.getFailureInfo().getFailureTypesList()))
                .build());
  }
}
