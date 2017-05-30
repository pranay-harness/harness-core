package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

import java.util.Map;

/**
 * Created by anubhaw on 8/4/16.
 */
public class AppDynamicsExecutionData extends StateExecutionData {
  private String correlationId;
  private String appDynamicsConfigId;
  private long appDynamicsApplicationId;
  private long appdynamicsTierId;

  public String getAppDynamicsConfigId() {
    return appDynamicsConfigId;
  }

  public void setAppDynamicsConfigId(String appDynamicsConfigId) {
    this.appDynamicsConfigId = appDynamicsConfigId;
  }

  public long getAppDynamicsApplicationId() {
    return appDynamicsApplicationId;
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
    return appDynamicsConfigId.equals(that.appDynamicsConfigId);
  }

  @Override
  public int hashCode() {
    int result = correlationId.hashCode();
    result = 31 * result + appDynamicsConfigId.hashCode();
    result = 31 * result + (int) (appDynamicsApplicationId ^ (appDynamicsApplicationId >>> 32));
    result = 31 * result + (int) (appdynamicsTierId ^ (appdynamicsTierId >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "AppDynamicsExecutionData{"
        + "correlationId='" + correlationId + '\'' + ", appDynamicsConfigId='" + appDynamicsConfigId + '\''
        + ", appDynamicsApplicationId=" + appDynamicsApplicationId + ", appdynamicsTierId=" + appdynamicsTierId + '}';
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
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

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String correlationId;
    private String appDynamicsConfigId;
    private long appDynamicsApplicationId;
    private long appdynamicsTierId;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;

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

    public Builder withAppDynamicsApplicationId(long appDynamicsApplicationId) {
      this.appDynamicsApplicationId = appDynamicsApplicationId;
      return this;
    }

    public Builder withAppdynamicsTierId(long appdynamicsTierId) {
      this.appdynamicsTierId = appdynamicsTierId;
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
      appDynamicsExecutionData.setAppDynamicsConfigId(appDynamicsConfigId);
      appDynamicsExecutionData.setAppDynamicsApplicationId(appDynamicsApplicationId);
      appDynamicsExecutionData.setAppdynamicsTierId(appdynamicsTierId);
      appDynamicsExecutionData.setStateName(stateName);
      appDynamicsExecutionData.setStartTs(startTs);
      appDynamicsExecutionData.setEndTs(endTs);
      appDynamicsExecutionData.setStatus(status);
      appDynamicsExecutionData.setErrorMsg(errorMsg);
      return appDynamicsExecutionData;
    }
  }
}
