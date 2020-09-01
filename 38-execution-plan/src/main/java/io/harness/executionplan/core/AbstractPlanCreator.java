package io.harness.executionplan.core;

public abstract class AbstractPlanCreator<T> implements ExecutionPlanCreator<T> {
  /** Can add some pre-planned steps common for complete plan */
  public void prePlanCreation(T input, ExecutionPlanCreationContext context) {
    // Do nothing.
  }

  /** Can add some post-planned steps common for complete plan */
  public void postPlanCreation(T input, ExecutionPlanCreationContext context) {
    // Do nothing.
  }

  /** Create PlanNode for self. */
  protected abstract ExecutionPlanCreatorResponse createPlanForSelf(T input, ExecutionPlanCreationContext context);

  @Override
  public ExecutionPlanCreatorResponse createPlan(T input, ExecutionPlanCreationContext context) {
    try {
      prePlanCreation(input, context);
      return createPlanForSelf(input, context);
    } finally {
      postPlanCreation(input, context);
    }
  }
}
