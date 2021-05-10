package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.FacilitatorResponseRequest;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.tasks.BinaryResponseData;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class FacilitateResponseRequestHandler implements SdkResponseEventHandler {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public void handleEvent(SdkResponseEvent event) {
    FacilitatorResponseRequest request = event.getSdkResponseEventRequest().getFacilitatorResponseRequest();
    waitNotifyEngine.doneWith(request.getNotifyId(),
        BinaryResponseData.builder().data(request.getFacilitatorResponse().toByteArray()).build());
  }
}
