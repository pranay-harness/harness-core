package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.state.StateType;
import io.harness.state.Step;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public interface StateRegistrar extends EngineRegistrar<StateType, Step> {
  void register(Set<Pair<StateType, Class<? extends Step>>> stateClasses);
}
