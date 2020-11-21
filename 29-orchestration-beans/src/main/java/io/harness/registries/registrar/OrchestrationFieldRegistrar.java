package io.harness.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.field.OrchestrationFieldProcessor;
import io.harness.expression.field.OrchestrationFieldType;
import io.harness.registries.Registrar;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public interface OrchestrationFieldRegistrar extends Registrar<OrchestrationFieldType, OrchestrationFieldProcessor> {
  void register(Set<Pair<OrchestrationFieldType, OrchestrationFieldProcessor>> fieldClasses);
}
