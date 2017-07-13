package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import software.wings.beans.CountsByStatuses;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 8/4/16.
 */
public class AppDynamicsExecutionData extends StateExecutionData {
  private String correlationId;
  private String stateExecutionInstanceId;
  private String appDynamicsConfigId;
  private int timeDuration;
  private long appDynamicsApplicationId;
  private long appdynamicsTierId;
  private Set<String> canaryNewHostNames;
  private List<String> btNames;

  public String getAppDynamicsConfigId() {
    return appDynamicsConfigId;
  }

  public void setAppDynamicsConfigId(String appDynamicsConfigId) {
    this.appDynamicsConfigId = appDynamicsConfigId;
  }

  public long getAppDynamicsApplicationId() {
    return appDynamicsApplicationId;
  }

  public int getTimeDuration() {
    return timeDuration;
  }

  public void setTimeDuration(int timeDuration) {
    this.timeDuration = timeDuration;
  }

  public void setAppDynamicsApplicationId(long appDynamicsApplicationId) {
    this.appDynamicsApplicationId = appDynamicsApplicationId;
  }

  public long getAppdynamicsTierId() {
    return appdynamicsTierId;
  }

  public void setAppdynamicsTierId(long appdynamicsTierId) {
    this.appdynamicsTierId = appdynamicsTierId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public String getStateExecutionInstanceId() {
    return stateExecutionInstanceId;
  }

  public void setStateExecutionInstanceId(String stateExecutionInstanceId) {
    this.stateExecutionInstanceId = stateExecutionInstanceId;
  }

  public Set<String> getCanaryNewHostNames() {
    return canaryNewHostNames;
  }

  public void setCanaryNewHostNames(Set<String> canaryNewHostNames) {
    this.canaryNewHostNames = canaryNewHostNames;
  }

  public List<String> getBtNames() {
    return btNames;
  }

  public void setBtNames(List<String> btNames) {
    this.btNames = btNames;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof AppDynamicsExecutionData))
      return false;

    AppDynamicsExecutionData that = (AppDynamicsExecutionData) o;

    if (appDynamicsApplicationId != that.appDynamicsApplicationId)
      return false;
    if (appdynamicsTierId != that.appdynamicsTierId)
      return false;
    if (!correlationId.equals(that.correlationId))
      return false;
    if (!stateExecutionInstanceId.equals(that.stateExecutionInstanceId))
      return false;
    if (!appDynamicsConfigId.equals(that.appDynamicsConfigId))
      return false;
    if (canaryNewHostNames != null ? !canaryNewHostNames.equals(that.canaryNewHostNames)
                                   : that.canaryNewHostNames != null)
      return false;
    return btNames != null ? btNames.equals(that.btNames) : that.btNames == null;
  }

  @Override
  public int hashCode() {
    int result = correlationId.hashCode();
    result = 31 * result + stateExecutionInstanceId.hashCode();
    result = 31 * result + appDynamicsConfigId.hashCode();
    result = 31 * result + (int) (appDynamicsApplicationId ^ (appDynamicsApplicationId >>> 32));
    result = 31 * result + (int) (appdynamicsTierId ^ (appdynamicsTierId >>> 32));
    result = 31 * result + (canaryNewHostNames != null ? canaryNewHostNames.hashCode() : 0);
    result = 31 * result + (btNames != null ? btNames.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "AppDynamicsExecutionData{"
        + "correlationId='" + correlationId + '\'' + ", stateExecutionInstanceId='" + stateExecutionInstanceId + '\''
        + ", appDynamicsConfigId='" + appDynamicsConfigId + '\''
        + ", appDynamicsApplicationId=" + appDynamicsApplicationId + ", appdynamicsTierId=" + appdynamicsTierId
        + ", canaryNewHostNames=" + canaryNewHostNames + ", btNames=" + btNames + '}';
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = getExecutionDetails();
    putNotNull(executionDetails, "stateExecutionInstanceId",
        anExecutionDataValue().withValue(stateExecutionInstanceId).withDisplayName("State Execution Id").build());
    putNotNull(executionDetails, "appDynamicsConfigId",
        anExecutionDataValue().withValue(appDynamicsConfigId).withDisplayName("Appdynamics Config Id").build());
    putNotNull(executionDetails, "appDynamicsApplicationId",
        anExecutionDataValue()
            .withValue(appDynamicsApplicationId)
            .withDisplayName("Appdynamics Application Id")
            .build());
    putNotNull(executionDetails, "appdynamicsTierId",
        anExecutionDataValue().withValue(appdynamicsTierId).withDisplayName("Appdynamics Tier Id").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "errorMsg",
        anExecutionDataValue().withValue(getErrorMsg()).withDisplayName("Message").build());
    putNotNull(
        executionDetails, "total", anExecutionDataValue().withDisplayName("Total").withValue(timeDuration + 1).build());
    final CountsByStatuses breakdown = new CountsByStatuses();
    breakdown.setSuccess(
        Math.min((int) TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - getStartTs()), timeDuration + 1));
    putNotNull(executionDetails, "breakdown",
        anExecutionDataValue().withDisplayName("breakdown").withValue(breakdown).build());
    putNotNull(executionDetails, "timeDuration",
        anExecutionDataValue().withValue(timeDuration).withDisplayName("Analysis duration").build());
    return executionDetails;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String correlationId;
    private String stateExecutionInstanceId;
    private String appDynamicsConfigId;
    private int timeDuration;
    private long appDynamicsApplicationId;
    private long appdynamicsTierId;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;
    private Set<String> canaryNewHostNames;
    private List<String> btNames;

    private Builder() {}

    /**
     * An app dynamics execution data builder.
     *
     * @return the builder
     */
    public static Builder anAppDynamicsExecutionData() {
      return new Builder();
    }

    public Builder withCorrelationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder withAppDynamicsConfigID(String appDynamicsConfigId) {
      this.appDynamicsConfigId = appDynamicsConfigId;
      return this;
    }

    public Builder withAnalysisDuration(int timeDuration) {
      this.timeDuration = timeDuration;
      return this;
    }

    public Builder withAppDynamicsApplicationId(long appDynamicsApplicationId) {
      this.appDynamicsApplicationId = appDynamicsApplicationId;
      return this;
    }

    public Builder withStateExecutionInstanceId(String stateExecutionInstanceId) {
      this.stateExecutionInstanceId = stateExecutionInstanceId;
      return this;
    }

    public Builder withAppdynamicsTierId(long appdynamicsTierId) {
      this.appdynamicsTierId = appdynamicsTierId;
      return this;
    }

    public Builder withCanaryNewHostNames(Set<String> canaryNewHostNames) {
      this.canaryNewHostNames = canaryNewHostNames;
      return this;
    }

    public Builder withBtNames(List<String> btNames) {
      this.btNames = btNames;
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
     * With start ts builder.
     *
     * @param startTs the start ts
     * @return the builder
     */
    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts builder.
     *
     * @param endTs the end ts
     * @return the builder
     */
    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With error msg builder.
     *
     * @param errorMsg the error msg
     * @return the builder
     */
    public Builder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anAppDynamicsExecutionData()
          .withCorrelationId(correlationId)
          .withAppDynamicsConfigID(appDynamicsConfigId)
          .withAppDynamicsApplicationId(appDynamicsApplicationId)
          .withAppdynamicsTierId(appdynamicsTierId)
          .withStateName(stateName)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withStatus(status)
          .withErrorMsg(errorMsg);
    }

    /**
     * Build app dynamics execution data.
     *
     * @return the app dynamics execution data
     */
    public AppDynamicsExecutionData build() {
      AppDynamicsExecutionData appDynamicsExecutionData = new AppDynamicsExecutionData();
      appDynamicsExecutionData.setCorrelationId(correlationId);
      appDynamicsExecutionData.setStateExecutionInstanceId(stateExecutionInstanceId);
      appDynamicsExecutionData.setCanaryNewHostNames(canaryNewHostNames);
      appDynamicsExecutionData.setAppDynamicsConfigId(appDynamicsConfigId);
      appDynamicsExecutionData.setTimeDuration(timeDuration);
      appDynamicsExecutionData.setAppDynamicsApplicationId(appDynamicsApplicationId);
      appDynamicsExecutionData.setAppdynamicsTierId(appdynamicsTierId);
      appDynamicsExecutionData.setStateName(stateName);
      appDynamicsExecutionData.setStartTs(startTs);
      appDynamicsExecutionData.setEndTs(endTs);
      appDynamicsExecutionData.setStatus(status);
      appDynamicsExecutionData.setErrorMsg(errorMsg);
      appDynamicsExecutionData.setBtNames(btNames);
      return appDynamicsExecutionData;
    }
  }
}
