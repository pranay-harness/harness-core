package io.harness.serializer.registrars;

import io.harness.beans.ParameterField;
import io.harness.expression.field.OrchestrationFieldProcessor;
import io.harness.expression.field.OrchestrationFieldType;
import io.harness.ng.expressions.ParameterFieldProcessor;
import io.harness.registries.registrar.OrchestrationFieldRegistrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class NGPipelineCommonsFieldRegistrar implements OrchestrationFieldRegistrar {
  @Override
  public void register(Set<Pair<OrchestrationFieldType, Class<? extends OrchestrationFieldProcessor>>> fieldClasses) {
    fieldClasses.add(Pair.of(ParameterField.ORCHESTRATION_FIELD_TYPE, ParameterFieldProcessor.class));
  }
}
