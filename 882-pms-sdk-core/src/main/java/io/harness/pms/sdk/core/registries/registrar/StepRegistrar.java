package io.harness.pms.sdk.core.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.steps.StepType;
import io.harness.registries.Registrar;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public interface StepRegistrar extends Registrar<StepType, Step> {
  void register(Set<Pair<StepType, Step>> stateClasses);
}
