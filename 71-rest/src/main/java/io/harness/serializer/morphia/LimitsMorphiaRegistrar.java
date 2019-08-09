package io.harness.serializer.morphia;

import io.harness.limits.ConfiguredLimit;
import io.harness.limits.Counter;
import io.harness.limits.checker.rate.UsageBucket;
import io.harness.mongo.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class LimitsMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void register(Set<Class> set) {
    set.add(Counter.class);
    set.add(ConfiguredLimit.class);
    set.add(UsageBucket.class);
  }

  @Override
  public void register(Map<String, Class> map) {}
}
