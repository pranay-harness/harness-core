package io.harness.executionplan.core;

public abstract class AbstractPlanCreator<T> implements ExecutionPlanCreator<T> {
  /** Can add some pre-planned steps common for complete plan */
  public void prePlanCreation(T input, CreateExecutionPlanContext context) {
    // Do nothing.
  }

  /** Can add some post-planned steps common for complete plan */
  public void postPlanCreation(T input, CreateExecutionPlanContext context) {
    // Do nothing.
  }

  /** Create PlanNode for self. */
  protected abstract CreateExecutionPlanResponse createPlanForSelf(T input, CreateExecutionPlanContext context);

  @Override
  public CreateExecutionPlanResponse createPlan(T input, CreateExecutionPlanContext context) {
    try {
      prePlanCreation(input, context);
      return createPlanForSelf(input, context);
    } finally {
      postPlanCreation(input, context);
    }
  }
}
