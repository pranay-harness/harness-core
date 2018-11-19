package io.harness.limits;

import com.google.common.collect.ImmutableSet;

import io.harness.limits.checker.rate.UsageBucket;

import java.util.Set;

public class LimitsMorphiaClasses {
  public static final Set<Class> classes =
      ImmutableSet.<Class>of(Counter.class, ConfiguredLimit.class, UsageBucket.class);
}
