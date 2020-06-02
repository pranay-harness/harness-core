package io.harness.executionplan.core;

/**
 * Would generate execution plan for objectToPlan
 * can delegate to other plan creators to create subplan for embedded objects and stich them in the master plan
 * @param <T> object type to plan for
 */
public interface ExecutionPlanCreator<T> {
  CreateExecutionPlanResponse createPlan(T objectToPlan, CreateExecutionPlanContext context);
}
