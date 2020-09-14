package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.field.OrchestrationFieldProcessor;
import io.harness.expression.field.OrchestrationFieldType;
import io.harness.registries.Registrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public interface OrchestrationFieldRegistrar extends Registrar<OrchestrationFieldType, OrchestrationFieldProcessor> {
  void register(Set<Pair<OrchestrationFieldType, Class<? extends OrchestrationFieldProcessor>>> fieldClasses);
}
