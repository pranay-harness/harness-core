package io.harness.serializer.spring;

import io.harness.spring.AliasRegistrar;
import io.harness.timeout.TimeoutInstance;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTracker;

import java.util.Map;

public class TimeoutEngineAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("timeoutInstance", TimeoutInstance.class);
    orchestrationElements.put("absoluteTimeoutTracker", AbsoluteTimeoutTracker.class);
  }
}
