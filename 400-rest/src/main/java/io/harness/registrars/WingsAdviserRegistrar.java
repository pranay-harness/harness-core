package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.registries.registrar.AdviserRegistrar;
import io.harness.redesign.advisers.HttpResponseCodeSwitchAdviser;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public class WingsAdviserRegistrar implements AdviserRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<AdviserType, Adviser>> adviserClasses) {
    adviserClasses.add(
        Pair.of(HttpResponseCodeSwitchAdviser.ADVISER_TYPE, injector.getInstance(HttpResponseCodeSwitchAdviser.class)));
  }
}
