package io.harness.queue;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
// Todo(sahil): Move out of persistence
public interface EventListenerObserver<T> {
  void onListenerEnd(T message);
  void onListenerStart(T message);
}
