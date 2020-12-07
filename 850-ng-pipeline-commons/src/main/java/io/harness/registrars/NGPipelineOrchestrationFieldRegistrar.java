package io.harness.registrars;

import io.harness.beans.ParameterField;
import io.harness.ngpipeline.expressions.ParameterFieldProcessor;
import io.harness.pms.sdk.core.expression.OrchestrationFieldProcessor;
import io.harness.pms.sdk.core.expression.OrchestrationFieldType;
import io.harness.pms.sdk.registries.registrar.OrchestrationFieldRegistrar;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

public class NGPipelineOrchestrationFieldRegistrar implements OrchestrationFieldRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<OrchestrationFieldType, OrchestrationFieldProcessor>> fieldClasses) {
    fieldClasses.add(
        Pair.of(ParameterField.ORCHESTRATION_FIELD_TYPE, injector.getInstance(ParameterFieldProcessor.class)));
  }
}
