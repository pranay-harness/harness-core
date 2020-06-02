package io.harness.executionplan.core.impl;

import static com.google.common.collect.Multimaps.synchronizedListMultimap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorRegistry;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefiner;
import lombok.Builder;
import lombok.Value;

import java.util.Optional;

public class ExecutionPlanCreatorRegistryImpl implements ExecutionPlanCreatorRegistry {
  private final Multimap<String, PlanCreatorDefinition> typeToPlanMap =
      synchronizedListMultimap(ArrayListMultimap.create());

  @Override
  public <T> Optional<ExecutionPlanCreator<T>> obtainCreator(PlanCreatorSearchContext<T> context) {
    for (PlanCreatorDefinition planCreatorDefinition : typeToPlanMap.get(context.getType())) {
      if (planCreatorDefinition.getSupportDefiner().supports(context)) {
        return Optional.ofNullable((ExecutionPlanCreator<T>) planCreatorDefinition.getExecutionPlanCreator());
      }
    }
    return Optional.empty();
  }

  public void registerCreator(SupportDefiner supportDefiner, ExecutionPlanCreator<?> executionPlanCreator) {
    for (String supportedType : supportDefiner.getSupportedTypes()) {
      typeToPlanMap.put(supportedType,
          PlanCreatorDefinition.builder()
              .supportDefiner(supportDefiner)
              .executionPlanCreator(executionPlanCreator)
              .build());
    }
  }

  @Value
  @Builder
  private static class PlanCreatorDefinition {
    ExecutionPlanCreator<?> executionPlanCreator;
    SupportDefiner supportDefiner;
  }
}
