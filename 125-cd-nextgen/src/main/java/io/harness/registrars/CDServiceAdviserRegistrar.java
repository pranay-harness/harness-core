package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.advisers.RollbackCustomAdviser;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.serializer.PipelineServiceUtilAdviserRegistrar;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class CDServiceAdviserRegistrar {
  public Map<AdviserType, Class<? extends Adviser>> getEngineAdvisers() {
    Map<AdviserType, Class<? extends Adviser>> advisersMap =
        new HashMap<>(OrchestrationAdviserRegistrar.getEngineAdvisers());
    advisersMap.putAll(PipelineServiceUtilAdviserRegistrar.getEngineAdvisers());
    advisersMap.put(RollbackCustomAdviser.ADVISER_TYPE, RollbackCustomAdviser.class);
    return advisersMap;
  }
}
