package io.harness.waiter;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.queue.QueueConsumer;

@Singleton
public final class TestNotifyEventListener extends NotifyEventListener {
  public static final String TEST_PUBLISHER = "test";

  @Inject
  public TestNotifyEventListener(QueueConsumer<NotifyEvent> consumer) {
    super(consumer);
  }
}
