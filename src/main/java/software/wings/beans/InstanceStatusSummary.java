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

  /**
   * Gets instances count.
   *
   * @return the instances count
   */
  public Integer getInstancesCount() {
    return instancesCount;
  }

  /**
   * Sets instances count.
   *
   * @param instancesCount the instances count
   */
  public void setInstancesCount(Integer instancesCount) {
    this.instancesCount = instancesCount;
  }

  /**
   * Gets prior execution status.
   *
   * @return the prior execution status
   */
  public ExecutionStatus getPriorExecutionStatus() {
    return priorExecutionStatus;
  }

  /**
   * Sets prior execution status.
   *
   * @param priorExecutionStatus the prior execution status
   */
  public void setPriorExecutionStatus(ExecutionStatus priorExecutionStatus) {
    this.priorExecutionStatus = priorExecutionStatus;
  }

  /**
   * Gets retry count.
   *
   * @return the retry count
   */
  public Integer getRetryCount() {
    return retryCount;
  }

  /**
   * Sets retry count.
   *
   * @param retryCount the retry count
   */
  public void setRetryCount(Integer retryCount) {
    this.retryCount = retryCount;
  }

  /**
   * Gets state name.
   *
   * @return the state name
   */
  public String getStateName() {
    return stateName;
  }

  /**
   * Sets state name.
   *
   * @param stateName the state name
   */
  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  /**
   * Gets state category.
   *
   * @return the state category
   */
  public String getStateCategory() {
    return stateCategory;
  }

  /**
   * Sets state category.
   *
   * @param stateCategory the state category
   */
  public void setStateCategory(String stateCategory) {
    this.stateCategory = stateCategory;
  }

  /**
   * Gets final execution status.
   *
   * @return the final execution status
   */
  public ExecutionStatus getFinalExecutionStatus() {
    return finalExecutionStatus;
  }

  /**
   * Sets final execution status.
   *
   * @param finalExecutionStatus the final execution status
   */
  public void setFinalExecutionStatus(ExecutionStatus finalExecutionStatus) {
    this.finalExecutionStatus = finalExecutionStatus;
  }

  /**
   * Gets report.
   *
   * @return the report
   */
  public String getReport() {
    return String.valueOf(finalExecutionStatus);
  }

  /**
   * Sets report.
   *
   * @param report the report
   */
  public void setReport(String report) {}

  /**
   * The type Builder.
   */
  public static final class Builder {
    private Integer instancesCount;
    private Integer retryCount;
    private String stateName;
    private String stateCategory;
    private ExecutionStatus priorExecutionStatus;
    private ExecutionStatus finalExecutionStatus;

    private Builder() {}

    /**
     * An instance status summary builder.
     *
     * @return the builder
     */
    public static Builder anInstanceStatusSummary() {
      return new Builder();
    }

    /**
     * With instances count builder.
     *
     * @param instancesCount the instances count
     * @return the builder
     */
    public Builder withInstancesCount(Integer instancesCount) {
      this.instancesCount = instancesCount;
      return this;
    }

    /**
     * With retry count builder.
     *
     * @param retryCount the retry count
     * @return the builder
     */
    public Builder withRetryCount(Integer retryCount) {
      this.retryCount = retryCount;
      return this;
    }

    /**
     * With state name builder.
     *
     * @param stateName the state name
     * @return the builder
     */
    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    /**
     * With state category builder.
     *
     * @param stateCategory the state category
     * @return the builder
     */
    public Builder withStateCategory(String stateCategory) {
      this.stateCategory = stateCategory;
      return this;
    }

    /**
     * With prior execution status builder.
     *
     * @param priorExecutionStatus the prior execution status
     * @return the builder
     */
    public Builder withPriorExecutionStatus(ExecutionStatus priorExecutionStatus) {
      this.priorExecutionStatus = priorExecutionStatus;
      return this;
    }

    /**
     * With final execution status builder.
     *
     * @param finalExecutionStatus the final execution status
     * @return the builder
     */
    public Builder withFinalExecutionStatus(ExecutionStatus finalExecutionStatus) {
      this.finalExecutionStatus = finalExecutionStatus;
      return this;
    }

    /**
     * Build instance status summary.
     *
     * @return the instance status summary
     */
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
