package io.harness.pms.sdk.core.interrupt.publisher;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptEventNotifyProto;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.sdk.core.events.PipelineSdkEventSender;

import com.google.inject.Inject;

@OwnedBy(PIPELINE)
public class RedisSdkInterruptEventNotifyPublisher implements SdkInterruptEventNotifyPublisher {
  @Inject private PipelineSdkEventSender pipelineSdkEventSender;

  @Override
  public void publishEvent(String notifyId, InterruptType interruptType) {
    pipelineSdkEventSender.sendEvent(InterruptEventNotifyProto.newBuilder()
                                         .setNotifyId(notifyId)
                                         .setInterruptType(interruptType)
                                         .build()
                                         .toByteString(),
        PmsEventCategory.SDK_INTERRUPT_EVENT_NOTIFY, false);
  }
}
