package io.harness.delay;

import com.google.inject.Inject;

import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DelayEventListener extends QueueListener<DelayEvent> {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Inject
  public DelayEventListener(QueueConsumer<DelayEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(DelayEvent message) {
    logger.info("Notifying for DelayEvent with resumeId {}", message.getResumeId());

    waitNotifyEngine.doneWith(
        message.getResumeId(), DelayEventNotifyData.builder().context(message.getContext()).build());
  }
}
