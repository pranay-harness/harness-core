package io.harness.pms.sdk.core.interrupt.publisher;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.sdk.core.PmsSdkCoreEventsFrameworkConstants.SDK_INTERRUPT_RESPONSE_PRODUCER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Producer;
import io.harness.pms.contracts.interrupts.InterruptEventResponseProto;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.sdk.core.events.PipelineSdkEventSender;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@OwnedBy(PIPELINE)
public class RedisSdkInterruptResponsePublisher implements SdkInterruptResponsePublisher {
  @Inject private PipelineSdkEventSender pipelineSdkEventSender;
  @Inject @Named(SDK_INTERRUPT_RESPONSE_PRODUCER) private Producer eventProducer;

  @Override
  public void publishEvent(String notifyId, InterruptType interruptType) {
    pipelineSdkEventSender.sendEvent(eventProducer,
        InterruptEventResponseProto.newBuilder()
            .setNotifyId(notifyId)
            .setInterruptType(interruptType)
            .build()
            .toByteString(),
        PmsEventCategory.SDK_INTERRUPT_RESPONSE, false);
  }
}
