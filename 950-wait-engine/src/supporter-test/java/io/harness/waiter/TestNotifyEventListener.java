package io.harness.waiter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.queue.QueueConsumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public final class TestNotifyEventListener extends NotifyEventListener {
  public static final String TEST_PUBLISHER = "test";

  @Inject
  public TestNotifyEventListener(QueueConsumer<NotifyEvent> consumer) {
    super(consumer);
  }
}
