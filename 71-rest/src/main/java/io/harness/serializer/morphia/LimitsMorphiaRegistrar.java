package io.harness.serializer.morphia;

import io.harness.limits.ConfiguredLimit;
import io.harness.limits.Counter;
import io.harness.limits.checker.rate.UsageBucket;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class LimitsMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Counter.class);
    set.add(ConfiguredLimit.class);
    set.add(UsageBucket.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {}
}
