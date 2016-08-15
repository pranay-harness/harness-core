package software.wings.beans;

import software.wings.sm.ExecutionStatus;

/**
 * Created by rishi on 8/15/16.
 */
public class InstanceStatusSummary {
  private Integer instancesCount;
  private Integer retryCount;
  private String stateName;
  private String stateCategory;
  private ExecutionStatus priorExecutionStatus;
  private ExecutionStatus finalExecutionStatus;

  public Integer getInstancesCount() {
    return instancesCount;
  }

  public void setInstancesCount(Integer instancesCount) {
    this.instancesCount = instancesCount;
  }

  public ExecutionStatus getPriorExecutionStatus() {
    return priorExecutionStatus;
  }

  public void setPriorExecutionStatus(ExecutionStatus priorExecutionStatus) {
    this.priorExecutionStatus = priorExecutionStatus;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(Integer retryCount) {
    this.retryCount = retryCount;
  }

  public String getStateName() {
    return stateName;
  }

  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  public String getStateCategory() {
    return stateCategory;
  }

  public void setStateCategory(String stateCategory) {
    this.stateCategory = stateCategory;
  }

  public ExecutionStatus getFinalExecutionStatus() {
    return finalExecutionStatus;
  }

  public void setFinalExecutionStatus(ExecutionStatus finalExecutionStatus) {
    this.finalExecutionStatus = finalExecutionStatus;
  }

  public String getReport() {
    return String.valueOf(finalExecutionStatus);
  }

  public void setReport(String report) {}

  public static final class Builder {
    private Integer instancesCount;
    private Integer retryCount;
    private String stateName;
    private String stateCategory;
    private ExecutionStatus priorExecutionStatus;
    private ExecutionStatus finalExecutionStatus;

    private Builder() {}

    public static Builder anInstanceStatusSummary() {
      return new Builder();
    }

    public Builder withInstancesCount(Integer instancesCount) {
      this.instancesCount = instancesCount;
      return this;
    }

    public Builder withRetryCount(Integer retryCount) {
      this.retryCount = retryCount;
      return this;
    }

    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public Builder withStateCategory(String stateCategory) {
      this.stateCategory = stateCategory;
      return this;
    }

    public Builder withPriorExecutionStatus(ExecutionStatus priorExecutionStatus) {
      this.priorExecutionStatus = priorExecutionStatus;
      return this;
    }

    public Builder withFinalExecutionStatus(ExecutionStatus finalExecutionStatus) {
      this.finalExecutionStatus = finalExecutionStatus;
      return this;
    }

    public InstanceStatusSummary build() {
      InstanceStatusSummary instanceStatusSummary = new InstanceStatusSummary();
      instanceStatusSummary.setInstancesCount(instancesCount);
      instanceStatusSummary.setRetryCount(retryCount);
      instanceStatusSummary.setStateName(stateName);
      instanceStatusSummary.setStateCategory(stateCategory);
      instanceStatusSummary.setPriorExecutionStatus(priorExecutionStatus);
      instanceStatusSummary.setFinalExecutionStatus(finalExecutionStatus);
      return instanceStatusSummary;
    }
  }
}
