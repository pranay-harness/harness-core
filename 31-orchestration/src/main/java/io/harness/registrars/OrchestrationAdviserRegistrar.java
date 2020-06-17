package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.advisers.fail.OnFailAdviser;
import io.harness.advisers.ignore.IgnoreAdviser;
import io.harness.advisers.retry.RetryAdviser;
import io.harness.advisers.success.OnSuccessAdviser;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.registrar.AdviserRegistrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationAdviserRegistrar implements AdviserRegistrar {
  @Override
  public void register(Set<Pair<AdviserType, Class<? extends Adviser>>> adviserClasses) {
    adviserClasses.add(Pair.of(IgnoreAdviser.ADVISER_TYPE, IgnoreAdviser.class));
    adviserClasses.add(Pair.of(OnSuccessAdviser.ADVISER_TYPE, OnSuccessAdviser.class));
    adviserClasses.add(Pair.of(RetryAdviser.ADVISER_TYPE, RetryAdviser.class));
    adviserClasses.add(Pair.of(OnFailAdviser.ADVISER_TYPE, OnFailAdviser.class));
  }
}
