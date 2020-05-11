package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.Adviser;
import io.harness.annotations.dev.OwnedBy;
import io.harness.redesign.advisers.HttpResponseCodeSwitchAdviser;
import io.harness.registries.registrar.AdviserRegistrar;

import java.util.Set;

@OwnedBy(CDC)
public class WingsAdviserRegistrar implements AdviserRegistrar {
  @Override
  public void register(Set<Class<? extends Adviser>> adviserClasses) {
    adviserClasses.add(HttpResponseCodeSwitchAdviser.class);
  }
}
