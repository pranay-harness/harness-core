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

  public static final class PhaseStepBuilder {
    private PhaseStepType phaseStepType;
    private List<Node> steps;
    private List<FailureStrategy> failureStrategies;

    private PhaseStepBuilder() {}

    public static PhaseStepBuilder aPhaseStep() {
      return new PhaseStepBuilder();
    }

    public PhaseStepBuilder withPhaseStepType(PhaseStepType phaseStepType) {
      this.phaseStepType = phaseStepType;
      return this;
    }

    public PhaseStepBuilder withSteps(List<Node> steps) {
      this.steps = steps;
      return this;
    }

    public PhaseStepBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public PhaseStep build() {
      PhaseStep phaseStep = new PhaseStep();
      phaseStep.setPhaseStepType(phaseStepType);
      phaseStep.setSteps(steps);
      phaseStep.setFailureStrategies(failureStrategies);
      return phaseStep;
    }
  }
}
