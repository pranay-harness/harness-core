package io.harness.limits;

import io.harness.limits.checker.rate.UsageBucket;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LimitsMorphiaClasses {
  public static final Set<Class> classes =
      ImmutableSet.<Class>of(Counter.class, ConfiguredLimit.class, UsageBucket.class);
}
