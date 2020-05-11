package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.Adviser;
import io.harness.adviser.impl.ignore.IgnoreAdviser;
import io.harness.adviser.impl.interrupts.AbortAdviser;
import io.harness.adviser.impl.retry.RetryAdviser;
import io.harness.adviser.impl.success.OnSuccessAdviser;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.registrar.AdviserRegistrar;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationAdviserRegistrar implements AdviserRegistrar {
  @Override
  public void register(Set<Class<? extends Adviser>> adviserClasses) {
    adviserClasses.add(IgnoreAdviser.class);
    adviserClasses.add(OnSuccessAdviser.class);
    adviserClasses.add(RetryAdviser.class);
    adviserClasses.add(AbortAdviser.class);
  }
}
