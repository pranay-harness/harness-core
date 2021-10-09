package io.harness.pms.sdk.execution.events;

import java.util.Map;

public interface PmsCommonsBaseEventHandler<T> {
  void handleEvent(T event, Map<String, String> metadataMap, long timestamp);
}