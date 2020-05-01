package io.harness.registrars;

import io.harness.adviser.Adviser;
import io.harness.redesign.advisers.HttpResponseCodeSwitchAdviser;
import io.harness.registries.registrar.AdviserRegistrar;

import java.util.Set;

public class WingsAdviserRegistrar implements AdviserRegistrar {
  @Override
  public void register(Set<Class<? extends Adviser>> adviserClasses) {
    adviserClasses.add(HttpResponseCodeSwitchAdviser.class);
  }
}
