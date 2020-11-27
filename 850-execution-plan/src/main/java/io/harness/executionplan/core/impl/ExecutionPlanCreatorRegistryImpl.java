package io.harness.executionplan.core.impl;

import static com.google.common.collect.Multimaps.synchronizedListMultimap;

import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorRegistry;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportedConfigDefiner;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Optional;
import lombok.Builder;
import lombok.Value;

public class ExecutionPlanCreatorRegistryImpl implements ExecutionPlanCreatorRegistry {
  private final Multimap<String, PlanCreatorDefinition> typeToPlanMap =
      synchronizedListMultimap(ArrayListMultimap.create());

  @Override
  public <T> Optional<ExecutionPlanCreator<T>> obtainCreator(PlanCreatorSearchContext<T> context) {
    for (PlanCreatorDefinition planCreatorDefinition : typeToPlanMap.get(context.getType())) {
      if (planCreatorDefinition.getSupportedConfigDefiner().supports(context)) {
        return Optional.ofNullable((ExecutionPlanCreator<T>) planCreatorDefinition.getExecutionPlanCreator());
      }
    }
    return Optional.empty();
  }

  public void registerCreator(
      SupportedConfigDefiner supportedConfigDefiner, ExecutionPlanCreator<?> executionPlanCreator) {
    for (String supportedType : supportedConfigDefiner.getSupportedTypes()) {
      typeToPlanMap.put(supportedType,
          PlanCreatorDefinition.builder()
              .supportedConfigDefiner(supportedConfigDefiner)
              .executionPlanCreator(executionPlanCreator)
              .build());
    }
  }

  @Value
  @Builder
  private static class PlanCreatorDefinition {
    ExecutionPlanCreator<?> executionPlanCreator;
    SupportedConfigDefiner supportedConfigDefiner;
  }
}
