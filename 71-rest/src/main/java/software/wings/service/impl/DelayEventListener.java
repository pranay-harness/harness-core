package software.wings.service.impl;

import com.google.inject.Inject;

import io.harness.queue.QueueListener;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DelayEventListener extends QueueListener<DelayEvent> {
  @Inject private WaitNotifyEngine waitNotifyEngine;

  public DelayEventListener() {
    super(false);
  }

  @Override
  public void onMessage(DelayEvent message) {
    logger.info("Notifying for DelayEvent with resumeId {}", message.getResumeId());

    waitNotifyEngine.notify(
        message.getResumeId(), DelayEventNotifyData.builder().context(message.getContext()).build());
  }
}
