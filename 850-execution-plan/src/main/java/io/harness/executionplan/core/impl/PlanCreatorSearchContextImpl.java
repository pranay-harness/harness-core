package io.harness.executionplan.core.impl;

import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.PlanCreatorSearchContext;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlanCreatorSearchContextImpl<T> implements PlanCreatorSearchContext<T> {
  T objectToPlan;
  String type;
  ExecutionPlanCreationContext createExecutionPlanContext;
}
