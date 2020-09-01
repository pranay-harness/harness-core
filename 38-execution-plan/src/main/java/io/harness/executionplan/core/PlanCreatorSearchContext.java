package io.harness.executionplan.core;

import io.harness.executionplan.core.impl.PlanCreatorSearchContextImpl;
import io.harness.executionplan.core.impl.PlanCreatorSearchContextImpl.PlanCreatorSearchContextImplBuilder;

/**
 * define  criteria to lookup into plan creator registry to fetch matching plan creator
 * @param <T> object type for which plan creator needs to be looked up
 */
public interface PlanCreatorSearchContext<T> {
  T getObjectToPlan();
  String getType();
  ExecutionPlanCreationContext getCreateExecutionPlanContext();

  static <T> PlanCreatorSearchContextImplBuilder<T> builder() {
    return PlanCreatorSearchContextImpl.builder();
  }
}
