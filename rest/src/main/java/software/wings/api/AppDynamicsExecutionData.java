package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import com.google.common.base.MoreObjects;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

import java.util.Map;
import java.util.Objects;

/**
 * Created by anubhaw on 8/4/16.
 */
public class AppDynamicsExecutionData extends StateExecutionData {
  private String appIdentifier;
  private String metricPath;
  private String assertionStatement;
  private String assertionStatus;
  private int httpResponseCode;
  private String response;

  /**
   * Gets assertion statement.
   *
   * @return the assertion statement
   */
  public String getAssertionStatement() {
    return assertionStatement;
  }

  /**
   * Sets assertion statement.
   *
   * @param assertionStatement the assertion statement
   */
  public void setAssertionStatement(String assertionStatement) {
    this.assertionStatement = assertionStatement;
  }

  /**
   * Gets assertion status.
   *
   * @return the assertion status
   */
  public String getAssertionStatus() {
    return assertionStatus;
  }

  /**
   * Sets assertion status.
   *
   * @param assertionStatus the assertion status
   */
  public void setAssertionStatus(String assertionStatus) {
    this.assertionStatus = assertionStatus;
  }

  /**
   * Gets http response code.
   *
   * @return the http response code
   */
  public int getHttpResponseCode() {
    return httpResponseCode;
  }

  /**
   * Sets http response code.
   *
   * @param httpResponseCode the http response code
   */
  public void setHttpResponseCode(int httpResponseCode) {
    this.httpResponseCode = httpResponseCode;
  }

  /**
   * Gets response.
   *
   * @return the response
   */
  public String getResponse() {
    return response;
  }

  /**
   * Sets response.
   *
   * @param response the response
   */
  public void setResponse(String response) {
    this.response = response;
  }

  /**
   * Gets app identifier.
   *
   * @return the app identifier
   */
  public String getAppIdentifier() {
    return appIdentifier;
  }

  /**
   * Sets app identifier.
   *
   * @param appIdentifier the app identifier
   */
  public void setAppIdentifier(String appIdentifier) {
    this.appIdentifier = appIdentifier;
  }

  /**
   * Gets metric path.
   *
   * @return the metric path
   */
  public String getMetricPath() {
    return metricPath;
  }

  /**
   * Sets metric path.
   *
   * @param metricPath the metric path
   */
  public void setMetricPath(String metricPath) {
    this.metricPath = metricPath;
  }

  @Override
  public int hashCode() {
    return Objects.hash(assertionStatement, assertionStatus, httpResponseCode, response);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final AppDynamicsExecutionData other = (AppDynamicsExecutionData) obj;
    return Objects.equals(this.assertionStatement, other.assertionStatement)
        && Objects.equals(this.assertionStatus, other.assertionStatus)
        && Objects.equals(this.httpResponseCode, other.httpResponseCode)
        && Objects.equals(this.response, other.response);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("assertionStatement", assertionStatement)
        .add("assertionStatus", assertionStatus)
        .add("httpResponseCode", httpResponseCode)
        .add("response", response)
        .toString();
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "httpResponseCode",
        anExecutionDataValue().withValue(httpResponseCode).withDisplayName("Response Code").build());
    putNotNull(executionDetails, "assertionStatement",
        anExecutionDataValue().withValue(assertionStatement).withDisplayName("Assertion").build());
    putNotNull(executionDetails, "assertionStatus",
        anExecutionDataValue().withValue(assertionStatus).withDisplayName("Assertion Result").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "httpResponseCode",
        anExecutionDataValue().withValue(httpResponseCode).withDisplayName("Response Code").build());
    putNotNull(executionDetails, "assertionStatement",
        anExecutionDataValue().withValue(assertionStatement).withDisplayName("Assertion").build());
    putNotNull(executionDetails, "assertionStatus",
        anExecutionDataValue().withValue(assertionStatus).withDisplayName("Assertion Result").build());
    putNotNull(
        executionDetails, "response", anExecutionDataValue().withValue(response).withDisplayName("response").build());
    putNotNull(executionDetails, "appIdentifier",
        anExecutionDataValue().withValue(appIdentifier).withDisplayName("App Identifier").build());
    putNotNull(executionDetails, "metricPath",
        anExecutionDataValue().withValue(metricPath).withDisplayName("Metric Path").build());
    return executionDetails;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String appIdentifier;
    private String metricPath;
    private String assertionStatement;
    private String assertionStatus;
    private int httpResponseCode;
    private String response;
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

    /**
     * With app identifier builder.
     *
     * @param appIdentifier the app identifier
     * @return the builder
     */
    public Builder withAppIdentifier(String appIdentifier) {
      this.appIdentifier = appIdentifier;
      return this;
    }

    /**
     * With metric path builder.
     *
     * @param metricPath the metric path
     * @return the builder
     */
    public Builder withMetricPath(String metricPath) {
      this.metricPath = metricPath;
      return this;
    }

    /**
     * With assertion statement builder.
     *
     * @param assertionStatement the assertion statement
     * @return the builder
     */
    public Builder withAssertionStatement(String assertionStatement) {
      this.assertionStatement = assertionStatement;
      return this;
    }

    /**
     * With assertion status builder.
     *
     * @param assertionStatus the assertion status
     * @return the builder
     */
    public Builder withAssertionStatus(String assertionStatus) {
      this.assertionStatus = assertionStatus;
      return this;
    }

    /**
     * With http response code builder.
     *
     * @param httpResponseCode the http response code
     * @return the builder
     */
    public Builder withHttpResponseCode(int httpResponseCode) {
      this.httpResponseCode = httpResponseCode;
      return this;
    }

    /**
     * With response builder.
     *
     * @param response the response
     * @return the builder
     */
    public Builder withResponse(String response) {
      this.response = response;
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
          .withAppIdentifier(appIdentifier)
          .withMetricPath(metricPath)
          .withAssertionStatement(assertionStatement)
          .withAssertionStatus(assertionStatus)
          .withHttpResponseCode(httpResponseCode)
          .withResponse(response)
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
      appDynamicsExecutionData.setAppIdentifier(appIdentifier);
      appDynamicsExecutionData.setMetricPath(metricPath);
      appDynamicsExecutionData.setAssertionStatement(assertionStatement);
      appDynamicsExecutionData.setAssertionStatus(assertionStatus);
      appDynamicsExecutionData.setHttpResponseCode(httpResponseCode);
      appDynamicsExecutionData.setResponse(response);
      appDynamicsExecutionData.setStateName(stateName);
      appDynamicsExecutionData.setStartTs(startTs);
      appDynamicsExecutionData.setEndTs(endTs);
      appDynamicsExecutionData.setStatus(status);
      appDynamicsExecutionData.setErrorMsg(errorMsg);
      return appDynamicsExecutionData;
    }
  }
}
