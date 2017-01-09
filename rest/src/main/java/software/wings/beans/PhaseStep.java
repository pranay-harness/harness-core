package software.wings.beans;

import software.wings.beans.Graph.Node;
import software.wings.common.UUIDGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rishi on 12/21/16.
 */
public class PhaseStep {
  private String uuid;
  private String name;
  private PhaseStepType phaseStepType;
  private List<Node> steps = new ArrayList<>();
  private boolean stepsInParallel;
  private List<FailureStrategy> failureStrategies = new ArrayList<>();

  public PhaseStep() {}

  public PhaseStep(PhaseStepType phaseStepType) {
    this.phaseStepType = phaseStepType;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getName() {
    if (name == null && phaseStepType != null) {
      return phaseStepType.name();
    }
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

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

    PhaseStep phaseStep = (PhaseStep) o;

    if (stepsInParallel != phaseStep.stepsInParallel)
      return false;
    if (uuid != null ? !uuid.equals(phaseStep.uuid) : phaseStep.uuid != null)
      return false;
    if (name != null ? !name.equals(phaseStep.name) : phaseStep.name != null)
      return false;
    if (phaseStepType != phaseStep.phaseStepType)
      return false;
    if (steps != null ? !steps.equals(phaseStep.steps) : phaseStep.steps != null)
      return false;
    return failureStrategies != null ? failureStrategies.equals(phaseStep.failureStrategies)
                                     : phaseStep.failureStrategies == null;
  }

  @Override
  public int hashCode() {
    int result = uuid != null ? uuid.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (phaseStepType != null ? phaseStepType.hashCode() : 0);
    result = 31 * result + (steps != null ? steps.hashCode() : 0);
    result = 31 * result + (stepsInParallel ? 1 : 0);
    result = 31 * result + (failureStrategies != null ? failureStrategies.hashCode() : 0);
    return result;
  }

  public static final class PhaseStepBuilder {
    private String uuid;
    private String name;
    private PhaseStepType phaseStepType;
    private List<Node> steps = new ArrayList<>();
    private boolean stepsInParallel;
    private List<FailureStrategy> failureStrategies = new ArrayList<>();

    private PhaseStepBuilder() {}

    public static PhaseStepBuilder aPhaseStep(PhaseStepType phaseStepType) {
      PhaseStepBuilder phaseStepBuilder = new PhaseStepBuilder();
      phaseStepBuilder.phaseStepType = phaseStepType;
      phaseStepBuilder.uuid = UUIDGenerator.getUuid();
      return phaseStepBuilder;
    }

    public PhaseStepBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public PhaseStepBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public PhaseStepBuilder withPhaseStepType(PhaseStepType phaseStepType) {
      this.phaseStepType = phaseStepType;
      return this;
    }

    public PhaseStepBuilder addStep(Node step) {
      this.steps.add(step);
      return this;
    }

    public PhaseStepBuilder withStepsInParallel(boolean stepsInParallel) {
      this.stepsInParallel = stepsInParallel;
      return this;
    }

    public PhaseStepBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public PhaseStep build() {
      PhaseStep phaseStep = new PhaseStep();
      phaseStep.setUuid(uuid);
      phaseStep.setName(name);
      phaseStep.setPhaseStepType(phaseStepType);
      phaseStep.setSteps(steps);
      phaseStep.setStepsInParallel(stepsInParallel);
      phaseStep.setFailureStrategies(failureStrategies);
      return phaseStep;
    }
  }
}
