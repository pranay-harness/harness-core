package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class NotifyEventListener extends QueueListener<NotifyEvent> {
  @Inject private NotifyEventListenerHelper notifyEventListenerHelper;

  @Inject
  public NotifyEventListener(QueueConsumer<NotifyEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(NotifyEvent message) {
    notifyEventListenerHelper.onMessage(message.getWaitInstanceId());
  }
}
