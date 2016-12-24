package software.wings.beans;

import software.wings.beans.Graph.Node;

import java.util.List;

/**
 * Created by rishi on 12/22/16.
 */
public class PhaseStep {
  private PhaseStepType phaseStepType;
  private List<Node> steps;
  private List<FailureStrategy> failureStrategies;

  public PhaseStepType getPhaseStepType() {
    return phaseStepType;
  }

  public void setPhaseStepType(PhaseStepType phaseStepType) {
    this.phaseStepType = phaseStepType;
  }

  public List<Node> getSteps() {
    return steps;
  }

  public void setSteps(List<Node> steps) {
    this.steps = steps;
  }

  public List<FailureStrategy> getFailureStrategies() {
    return failureStrategies;
  }

  public void setFailureStrategies(List<FailureStrategy> failureStrategies) {
    this.failureStrategies = failureStrategies;
  }
}
