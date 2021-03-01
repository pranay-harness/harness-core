package io.harness.pms.sdk.core.interrupt;

import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.interrupts.InterruptEvent;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterruptEventListener extends QueueListener<InterruptEvent> {
  @Inject private PMSInterruptService pmsInterruptService;

  @Inject
  public InterruptEventListener(QueueConsumer<InterruptEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(InterruptEvent event) {
    InterruptType interruptType = event.getInterruptType();
    StepType stepType = event.getStepType();
    log.info("Starting to handle InterruptEvent of type: {} for step type : {}", interruptType, stepType);
    pmsInterruptService.handleAbort(event.getNotifyId());
  }
}
