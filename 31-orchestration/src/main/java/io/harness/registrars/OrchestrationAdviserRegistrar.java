package io.harness.registrars;

import io.harness.adviser.Adviser;
import io.harness.adviser.impl.ignore.IgnoreAdviser;
import io.harness.adviser.impl.interrupts.AbortAdviser;
import io.harness.adviser.impl.retry.RetryAdviser;
import io.harness.adviser.impl.success.OnSuccessAdviser;
import io.harness.registries.registrar.AdviserRegistrar;

import java.util.Set;

public class OrchestrationAdviserRegistrar implements AdviserRegistrar {
  @Override
  public void register(Set<Class<? extends Adviser>> adviserClasses) {
    adviserClasses.add(IgnoreAdviser.class);
    adviserClasses.add(OnSuccessAdviser.class);
    adviserClasses.add(RetryAdviser.class);
    adviserClasses.add(AbortAdviser.class);
  }
}
