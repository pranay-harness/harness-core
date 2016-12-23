package software.wings.beans;

import software.wings.beans.Graph.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rishi on 12/21/16.
 */
public class WorkflowOuterSteps {
  private List<Node> steps = new ArrayList<>();
  private boolean stepsInParallel;
  private List<FailureStrategy> failureStrategies = new ArrayList<>();

  public List<Node> getSteps() {
    return steps;
  }

  public void setSteps(List<Node> steps) {
    this.steps = steps;
  }

  public boolean isStepsInParallel() {
    return stepsInParallel;
  }

  public void setStepsInParallel(boolean stepsInParallel) {
    this.stepsInParallel = stepsInParallel;
  }

  public List<FailureStrategy> getFailureStrategies() {
    return failureStrategies;
  }

  public void setFailureStrategies(List<FailureStrategy> failureStrategies) {
    this.failureStrategies = failureStrategies;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    WorkflowOuterSteps that = (WorkflowOuterSteps) o;

    if (stepsInParallel != that.stepsInParallel)
      return false;
    if (steps != null ? !steps.equals(that.steps) : that.steps != null)
      return false;
    return failureStrategies != null ? failureStrategies.equals(that.failureStrategies)
                                     : that.failureStrategies == null;
  }

  @Override
  public int hashCode() {
    int result = steps != null ? steps.hashCode() : 0;
    result = 31 * result + (stepsInParallel ? 1 : 0);
    result = 31 * result + (failureStrategies != null ? failureStrategies.hashCode() : 0);
    return result;
  }

  public static final class WorkflowOuterStepsBuilder {
    private List<Node> steps = new ArrayList<>();
    private boolean stepsInParallel;
    private List<FailureStrategy> failureStrategies = new ArrayList<>();

    private WorkflowOuterStepsBuilder() {}

    public static WorkflowOuterStepsBuilder aWorkflowOuterSteps() {
      return new WorkflowOuterStepsBuilder();
    }

    public WorkflowOuterStepsBuilder withSteps(List<Node> steps) {
      this.steps = steps;
      return this;
    }

    public WorkflowOuterStepsBuilder withStepsInParallel(boolean stepsInParallel) {
      this.stepsInParallel = stepsInParallel;
      return this;
    }

    public WorkflowOuterStepsBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public WorkflowOuterSteps build() {
      WorkflowOuterSteps workflowOuterSteps = new WorkflowOuterSteps();
      workflowOuterSteps.setSteps(steps);
      workflowOuterSteps.setStepsInParallel(stepsInParallel);
      workflowOuterSteps.setFailureStrategies(failureStrategies);
      return workflowOuterSteps;
    }
  }
}
