package io.harness.pms.sdk.registries.registrar.local;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.advisers.AdviserType;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.abort.OnAbortAdviser;
import io.harness.pms.sdk.core.adviser.fail.OnFailAdviser;
import io.harness.pms.sdk.core.adviser.ignore.IgnoreAdviser;
import io.harness.pms.sdk.core.adviser.manualintervention.ManualInterventionAdviser;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviser;
import io.harness.pms.sdk.registries.registrar.AdviserRegistrar;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public class PmsSdkAdviserRegistrar implements AdviserRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<AdviserType, Adviser>> adviserClasses) {
    adviserClasses.add(Pair.of(IgnoreAdviser.ADVISER_TYPE, injector.getInstance(IgnoreAdviser.class)));
    adviserClasses.add(Pair.of(OnSuccessAdviser.ADVISER_TYPE, injector.getInstance(OnSuccessAdviser.class)));
    adviserClasses.add(Pair.of(OnFailAdviser.ADVISER_TYPE, injector.getInstance(OnFailAdviser.class)));
    adviserClasses.add(
        Pair.of(ManualInterventionAdviser.ADVISER_TYPE, injector.getInstance(ManualInterventionAdviser.class)));
    adviserClasses.add(Pair.of(OnAbortAdviser.ADVISER_TYPE, injector.getInstance(OnAbortAdviser.class)));
  }
}
