package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.redesign.advisers.HttpResponseCodeSwitchAdviser;
import io.harness.registries.registrar.AdviserRegistrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class WingsAdviserRegistrar implements AdviserRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<AdviserType, Adviser<?>>> adviserClasses) {
    adviserClasses.add(
        Pair.of(HttpResponseCodeSwitchAdviser.ADVISER_TYPE, injector.getInstance(HttpResponseCodeSwitchAdviser.class)));
  }
}
