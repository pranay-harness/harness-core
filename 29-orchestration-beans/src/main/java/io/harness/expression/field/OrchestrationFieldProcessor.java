package io.harness.expression.field;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface OrchestrationFieldProcessor<T extends OrchestrationField> {
  ProcessorResult process(Ambiance ambiance, T field);
}
