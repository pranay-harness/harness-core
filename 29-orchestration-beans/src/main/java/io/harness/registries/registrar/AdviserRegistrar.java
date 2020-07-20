package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.Registrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public interface AdviserRegistrar extends Registrar<AdviserType, Adviser> {
  void register(Set<Pair<AdviserType, Class<? extends Adviser>>> adviserClasses);
}
