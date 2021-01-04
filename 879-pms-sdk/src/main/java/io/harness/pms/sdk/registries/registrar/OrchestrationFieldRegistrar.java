package io.harness.pms.sdk.registries.registrar;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.expression.OrchestrationFieldProcessor;
import io.harness.pms.expression.OrchestrationFieldType;
import io.harness.registries.Registrar;

import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public interface OrchestrationFieldRegistrar extends Registrar<OrchestrationFieldType, OrchestrationFieldProcessor> {
  void register(Set<Pair<OrchestrationFieldType, OrchestrationFieldProcessor>> fieldClasses);
}
