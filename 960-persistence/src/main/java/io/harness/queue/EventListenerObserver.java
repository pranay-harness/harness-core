package io.harness.queue;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
// Todo(sahil): Move out of persistence
public interface EventListenerObserver<T> {
  void onListenerEnd(T message, Map<String, String> metadataMap);
  void onListenerStart(T message, Map<String, String> metadataMap);
}
