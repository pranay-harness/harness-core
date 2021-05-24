package io.harness.queue;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface QueueListenerObserver<T extends Queuable> {
  void onListenerEnd(T message);
}
