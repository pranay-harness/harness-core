package io.harness.queue;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
// TODO (sahil): Rename this to EventListenerObserver and move it out of Persistence
public interface QueueListenerObserver<T extends Queuable> {
  void onListenerEnd(T message);
  void onListenerStart(T message);
}
