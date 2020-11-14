package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.field.OrchestrationFieldProcessor;
import io.harness.expression.field.OrchestrationFieldType;
import io.harness.expression.field.dummy.DummyOrchestrationField;
import io.harness.expression.field.dummy.DummyOrchestrationFieldProcessor;
import io.harness.registries.registrar.OrchestrationFieldRegistrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationBeansFieldRegistrar implements OrchestrationFieldRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<OrchestrationFieldType, OrchestrationFieldProcessor>> fieldClasses) {
    fieldClasses.add(Pair.of(DummyOrchestrationField.ORCHESTRATION_FIELD_TYPE,
        injector.getInstance(DummyOrchestrationFieldProcessor.class)));
  }
}
