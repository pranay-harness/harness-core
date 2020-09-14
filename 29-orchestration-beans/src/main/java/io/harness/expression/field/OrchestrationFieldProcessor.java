package io.harness.expression.field;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.dev.OwnedBy;
import io.harness.registries.RegistrableEntity;

@OwnedBy(CDC)
public interface OrchestrationFieldProcessor<T extends OrchestrationField> extends RegistrableEntity {
  ProcessorResult process(Ambiance ambiance, T field);
}
