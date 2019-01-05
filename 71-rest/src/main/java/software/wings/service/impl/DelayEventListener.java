package software.wings.service.impl;

import com.google.inject.Inject;

import io.harness.queue.QueueListener;
import io.harness.waiter.WaitNotifyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelayEventListener extends QueueListener<DelayEvent> {
  private static final Logger logger = LoggerFactory.getLogger(DelayEventListener.class);

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
