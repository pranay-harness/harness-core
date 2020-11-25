package io.harness.expression.field;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ambiance.Ambiance;

@OwnedBy(CDC)
public interface OrchestrationFieldProcessor<T extends OrchestrationField> {
  ProcessorResult process(Ambiance ambiance, T field);
}
