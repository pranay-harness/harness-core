package io.harness.pms.sdk.core.interrupt;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.pms.interrupts.InterruptEvent;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
@Deprecated
public class InterruptEventListener extends QueueListener<InterruptEvent> {
  @Inject InterruptEventHandler interruptEventHandler;

  @Inject
  public InterruptEventListener(QueueConsumer<InterruptEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(InterruptEvent event) {
    io.harness.pms.contracts.interrupts.InterruptEvent interruptEvent =
        io.harness.pms.contracts.interrupts.InterruptEvent.newBuilder()
            .setType(event.getInterruptType())
            .setNodeExecution(event.getNodeExecution())
            .setInterruptUuid(event.getInterruptUuid())
            .setNotifyId(event.getNotifyId())
            .putAllMetadata(CollectionUtils.emptyIfNull(event.getMetadata()))
            .build();
    interruptEventHandler.handleEvent(interruptEvent);
  }
}
