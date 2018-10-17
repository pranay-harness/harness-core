package software.wings.api;

import io.harness.delegate.task.protocol.ResponseData;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.FailureStrategy;
import software.wings.beans.PhaseStepType;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.states.ElementStateExecutionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 1/19/17.
 */
public class PhaseStepExecutionData extends ElementStateExecutionData implements ResponseData {
  private PhaseStepType phaseStepType;
  private boolean stepsInParallel;
  private boolean defaultFailureStrategy;
  private List<FailureStrategy> failureStrategies = new ArrayList<>();
  private PhaseStepExecutionSummary phaseStepExecutionSummary;

  public PhaseStepType getPhaseStepType() {
    return phaseStepType;
  }

  public void setPhaseStepType(PhaseStepType phaseStepType) {
    this.phaseStepType = phaseStepType;
  }

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

  public PhaseStepExecutionSummary getPhaseStepExecutionSummary() {
    return phaseStepExecutionSummary;
  }

  public void setPhaseStepExecutionSummary(PhaseStepExecutionSummary phaseStepExecutionSummary) {
    this.phaseStepExecutionSummary = phaseStepExecutionSummary;
  }

  public static final class PhaseStepExecutionDataBuilder {
    private List<ElementExecutionSummary> elementStatusSummary = new ArrayList<>();
    private String stateName;
    private boolean stepsInParallel;
    private Long startTs;
    private boolean defaultFailureStrategy;
    private Long endTs;
    private Map<String, ExecutionStatus> instanceIdStatusMap = new HashMap<>();
    private ExecutionStatus status;
    private List<FailureStrategy> failureStrategies = new ArrayList<>();
    private String errorMsg;
    private Integer waitInterval;
    private PhaseStepExecutionSummary phaseStepExecutionSummary;
    private PhaseStepType phaseStepType;

    private PhaseStepExecutionDataBuilder() {}

    public static PhaseStepExecutionDataBuilder aPhaseStepExecutionData() {
      return new PhaseStepExecutionDataBuilder();
    }

    public PhaseStepExecutionDataBuilder withElementStatusSummary(List<ElementExecutionSummary> elementStatusSummary) {
      this.elementStatusSummary = elementStatusSummary;
      return this;
    }

    public PhaseStepExecutionDataBuilder withPhaseStepType(PhaseStepType phaseStepType) {
      this.phaseStepType = phaseStepType;
      return this;
    }

    public PhaseStepExecutionDataBuilder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public PhaseStepExecutionDataBuilder withStepsInParallel(boolean stepsInParallel) {
      this.stepsInParallel = stepsInParallel;
      return this;
    }

    public PhaseStepExecutionDataBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public PhaseStepExecutionDataBuilder withDefaultFailureStrategy(boolean defaultFailureStrategy) {
      this.defaultFailureStrategy = defaultFailureStrategy;
      return this;
    }

    public PhaseStepExecutionDataBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public PhaseStepExecutionDataBuilder withInstanceIdStatusMap(Map<String, ExecutionStatus> instanceIdStatusMap) {
      this.instanceIdStatusMap = instanceIdStatusMap;
      return this;
    }

    public PhaseStepExecutionDataBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public PhaseStepExecutionDataBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public PhaseStepExecutionDataBuilder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public PhaseStepExecutionDataBuilder withWaitInterval(Integer waitInterval) {
      this.waitInterval = waitInterval;
      return this;
    }

    public PhaseStepExecutionDataBuilder withPhaseStepExecutionSummary(
        PhaseStepExecutionSummary phaseStepExecutionSummary) {
      this.phaseStepExecutionSummary = phaseStepExecutionSummary;
      return this;
    }

    public PhaseStepExecutionData build() {
      PhaseStepExecutionData phaseStepExecutionData = new PhaseStepExecutionData();
      phaseStepExecutionData.setElementStatusSummary(elementStatusSummary);
      phaseStepExecutionData.setStateName(stateName);
      phaseStepExecutionData.setStepsInParallel(stepsInParallel);
      phaseStepExecutionData.setStartTs(startTs);
      phaseStepExecutionData.setDefaultFailureStrategy(defaultFailureStrategy);
      phaseStepExecutionData.setEndTs(endTs);
      phaseStepExecutionData.setInstanceIdStatusMap(instanceIdStatusMap);
      phaseStepExecutionData.setStatus(status);
      phaseStepExecutionData.setFailureStrategies(failureStrategies);
      phaseStepExecutionData.setErrorMsg(errorMsg);
      phaseStepExecutionData.setWaitInterval(waitInterval);
      phaseStepExecutionData.setPhaseStepExecutionSummary(phaseStepExecutionSummary);
      phaseStepExecutionData.setPhaseStepType(phaseStepType);
      return phaseStepExecutionData;
    }
  }
}