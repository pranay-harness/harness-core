package io.harness.registrars;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.serializer.PipelineServiceUtilAdviserRegistrar;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CI)
public class ExecutionAdvisers {
  public Map<AdviserType, Class<? extends Adviser>> getEngineAdvisers() {
    Map<AdviserType, Class<? extends Adviser>> advisersMap =
        new HashMap<>(OrchestrationAdviserRegistrar.getEngineAdvisers());
    advisersMap.putAll(PipelineServiceUtilAdviserRegistrar.getEngineAdvisers());
    return advisersMap;
  }
}
