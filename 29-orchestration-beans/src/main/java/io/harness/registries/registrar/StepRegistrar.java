package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.Registrar;
import io.harness.state.Step;
import io.harness.state.StepType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public interface StepRegistrar extends Registrar<StepType, Step> {
  void register(Set<Pair<StepType, Class<? extends Step>>> stateClasses);
}
