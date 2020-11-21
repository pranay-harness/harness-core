package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.steps.StepType;
import io.harness.registries.Registrar;
import io.harness.state.Step;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public interface StepRegistrar extends Registrar<StepType, Step> {
  void register(Set<Pair<StepType, Step>> stateClasses);
}
