package io.harness.pms.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;

@OwnedBy(CDC)
public interface OrchestrationFieldProcessor<T extends OrchestrationField> {
  ProcessorResult process(Ambiance ambiance, T field);
}
