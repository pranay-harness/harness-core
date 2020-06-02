package io.harness.executionplan.core;

import java.util.Optional;

/**
 * plan creators can register themselves with support definition.
 * they can later be searched by defining search criteria mostly by other plan creators
 * to delegate sub plan creation work
 */
public interface ExecutionPlanCreatorRegistry {
  <T> Optional<ExecutionPlanCreator<T>> obtainCreator(PlanCreatorSearchContext<T> context);
  void registerCreator(SupportDefiner supportDefiner, ExecutionPlanCreator<?> executionPlanCreator);
}
