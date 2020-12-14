package io.harness.pms.sdk.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.registries.Registrar;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public interface AdviserRegistrar extends Registrar<AdviserType, Adviser> {
  void register(Set<Pair<AdviserType, Adviser>> adviserClasses);
}
