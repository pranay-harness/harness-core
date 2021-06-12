package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.events.AdviserResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.tasks.BinaryResponseData;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class AdviserEventResponseHandler implements SdkResponseEventHandler {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    AdviserResponseRequest request = event.getSdkResponseEventRequest().getAdviserResponseRequest();
    waitNotifyEngine.doneWith(
        request.getNotifyId(), BinaryResponseData.builder().data(request.getAdviserResponse().toByteArray()).build());
  }
}
