package io.harness.serializer.spring;

import io.harness.spring.AliasRegistrar;
import io.harness.timeout.trackers.AbsoluteTimeoutTracker;
import io.harness.timeout.trackers.ActiveTimeoutTracker;

import java.util.Map;

public class TimeoutEngineAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("absoluteTimeoutTracker", AbsoluteTimeoutTracker.class);
    orchestrationElements.put("activeTimeoutTracker", ActiveTimeoutTracker.class);
  }
}
