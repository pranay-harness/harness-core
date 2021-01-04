package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.field.dummy.DummyOrchestrationField;
import io.harness.expression.field.dummy.DummyOrchestrationFieldProcessor;
import io.harness.pms.expression.OrchestrationFieldProcessor;
import io.harness.pms.expression.OrchestrationFieldType;
import io.harness.pms.sdk.registries.registrar.OrchestrationFieldRegistrar;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.ParameterFieldProcessor;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public class OrchestrationBeansFieldRegistrar implements OrchestrationFieldRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<OrchestrationFieldType, OrchestrationFieldProcessor>> fieldClasses) {
    fieldClasses.add(Pair.of(DummyOrchestrationField.ORCHESTRATION_FIELD_TYPE,
        injector.getInstance(DummyOrchestrationFieldProcessor.class)));
    fieldClasses.add(
        Pair.of(ParameterField.ORCHESTRATION_FIELD_TYPE, injector.getInstance(ParameterFieldProcessor.class)));
  }
}
