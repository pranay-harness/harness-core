package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.Adviser;
import io.harness.advisers.abort.OnAbortAdviser;
import io.harness.advisers.fail.OnFailAdviser;
import io.harness.advisers.ignore.IgnoreAdviser;
import io.harness.advisers.manualintervention.ManualInterventionAdviser;
import io.harness.advisers.retry.RetryAdviser;
import io.harness.advisers.success.OnSuccessAdviser;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.advisers.AdviserType;
import io.harness.registries.registrar.AdviserRegistrar;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public class OrchestrationAdviserRegistrar implements AdviserRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<AdviserType, Adviser>> adviserClasses) {
    adviserClasses.add(Pair.of(IgnoreAdviser.ADVISER_TYPE, injector.getInstance(IgnoreAdviser.class)));
    adviserClasses.add(Pair.of(OnSuccessAdviser.ADVISER_TYPE, injector.getInstance(OnSuccessAdviser.class)));
    adviserClasses.add(Pair.of(RetryAdviser.ADVISER_TYPE, injector.getInstance(RetryAdviser.class)));
    adviserClasses.add(Pair.of(OnFailAdviser.ADVISER_TYPE, injector.getInstance(OnFailAdviser.class)));
    adviserClasses.add(
        Pair.of(ManualInterventionAdviser.ADVISER_TYPE, injector.getInstance(ManualInterventionAdviser.class)));
    adviserClasses.add(Pair.of(OnAbortAdviser.ADVISER_TYPE, injector.getInstance(OnAbortAdviser.class)));
  }
}
