package io.harness.registrars;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.beans.ParameterField;
import io.harness.expression.field.OrchestrationFieldProcessor;
import io.harness.expression.field.OrchestrationFieldType;
import io.harness.ngpipeline.expressions.ParameterFieldProcessor;
import io.harness.registries.registrar.OrchestrationFieldRegistrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public class NGPipelineOrchestrationFieldRegistrar implements OrchestrationFieldRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<OrchestrationFieldType, OrchestrationFieldProcessor>> fieldClasses) {
    fieldClasses.add(
        Pair.of(ParameterField.ORCHESTRATION_FIELD_TYPE, injector.getInstance(ParameterFieldProcessor.class)));
  }
}
