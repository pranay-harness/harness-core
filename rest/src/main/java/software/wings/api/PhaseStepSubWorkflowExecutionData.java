package software.wings.api;

import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.FailureStrategy;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.PhaseStepExecutionState;
import software.wings.sm.states.ElementStateExecutionData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rishi on 1/19/17.
 */
public class PhaseStepSubWorkflowExecutionData extends ElementStateExecutionData implements NotifyResponseData {
  private boolean stepsInParallel;
  private boolean defaultFailureStrategy;
  private List<FailureStrategy> failureStrategies = new ArrayList<>();
  private PhaseStepExecutionState phaseStepExecutionState;

  public boolean isStepsInParallel() {
    return stepsInParallel;
  }

  public void setStepsInParallel(boolean stepsInParallel) {
    this.stepsInParallel = stepsInParallel;
  }

  public boolean isDefaultFailureStrategy() {
    return defaultFailureStrategy;
  }

  public void setDefaultFailureStrategy(boolean defaultFailureStrategy) {
    this.defaultFailureStrategy = defaultFailureStrategy;
  }

  public List<FailureStrategy> getFailureStrategies() {
    return failureStrategies;
  }

  public void setFailureStrategies(List<FailureStrategy> failureStrategies) {
    this.failureStrategies = failureStrategies;
  }

  public PhaseStepExecutionState getPhaseStepExecutionState() {
    return phaseStepExecutionState;
  }

  public void setPhaseStepExecutionState(PhaseStepExecutionState phaseStepExecutionState) {
    this.phaseStepExecutionState = phaseStepExecutionState;
  }

  public static final class PhaseStepSubWorkflowExecutionDataBuilder {
    private List<ElementExecutionSummary> elementStatusSummary = new ArrayList<>();
    private boolean stepsInParallel;
    private String stateName;
    private boolean defaultFailureStrategy;
    private List<InstanceStatusSummary> instanceStatusSummary;
    private Long startTs;
    private Long endTs;
    private List<FailureStrategy> failureStrategies = new ArrayList<>();
    private ExecutionStatus status;
    private String errorMsg;

    private PhaseStepSubWorkflowExecutionDataBuilder() {}

    public static PhaseStepSubWorkflowExecutionDataBuilder aPhaseStepSubWorkflowExecutionData() {
      return new PhaseStepSubWorkflowExecutionDataBuilder();
    }

    public PhaseStepSubWorkflowExecutionDataBuilder withElementStatusSummary(
        List<ElementExecutionSummary> elementStatusSummary) {
      this.elementStatusSummary = elementStatusSummary;
      return this;
    }

    public PhaseStepSubWorkflowExecutionDataBuilder withStepsInParallel(boolean stepsInParallel) {
      this.stepsInParallel = stepsInParallel;
      return this;
    }

    public PhaseStepSubWorkflowExecutionDataBuilder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public PhaseStepSubWorkflowExecutionDataBuilder withDefaultFailureStrategy(boolean defaultFailureStrategy) {
      this.defaultFailureStrategy = defaultFailureStrategy;
      return this;
    }

    public PhaseStepSubWorkflowExecutionDataBuilder withInstanceStatusSummary(
        List<InstanceStatusSummary> instanceStatusSummary) {
      this.instanceStatusSummary = instanceStatusSummary;
      return this;
    }

    public PhaseStepSubWorkflowExecutionDataBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public PhaseStepSubWorkflowExecutionDataBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public PhaseStepSubWorkflowExecutionDataBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public PhaseStepSubWorkflowExecutionDataBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public PhaseStepSubWorkflowExecutionDataBuilder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public PhaseStepSubWorkflowExecutionData build() {
      PhaseStepSubWorkflowExecutionData phaseStepSubWorkflowExecutionData = new PhaseStepSubWorkflowExecutionData();
      phaseStepSubWorkflowExecutionData.setElementStatusSummary(elementStatusSummary);
      phaseStepSubWorkflowExecutionData.setStepsInParallel(stepsInParallel);
      phaseStepSubWorkflowExecutionData.setStateName(stateName);
      phaseStepSubWorkflowExecutionData.setDefaultFailureStrategy(defaultFailureStrategy);
      phaseStepSubWorkflowExecutionData.setStartTs(startTs);
      phaseStepSubWorkflowExecutionData.setEndTs(endTs);
      phaseStepSubWorkflowExecutionData.setFailureStrategies(failureStrategies);
      phaseStepSubWorkflowExecutionData.setStatus(status);
      phaseStepSubWorkflowExecutionData.setErrorMsg(errorMsg);
      return phaseStepSubWorkflowExecutionData;
    }
  }
}