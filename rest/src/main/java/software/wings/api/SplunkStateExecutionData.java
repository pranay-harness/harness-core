package software.wings.api;

import static org.apache.commons.lang3.StringUtils.abbreviate;
import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import software.wings.common.Constants;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

import java.util.Map;

/**
 * Created by peeyushaggarwal on 7/15/16.
 */
public class SplunkStateExecutionData extends StateExecutionData {
  private String query;
  private String response;
  private String assertionStatement;
  private String assertionStatus;

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
   * Gets query.
   *
   * @return the query
   */
  public String getQuery() {
    return query;
  }

  /**
   * Sets query.
   *
   * @param query the query
   */
  public void setQuery(String query) {
    this.query = query;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "query", anExecutionDataValue().withDisplayName("Query").withValue(query).build());
    putNotNull(
        executionDetails, "response", anExecutionDataValue().withDisplayName("Response").withValue(response).build());
    putNotNull(executionDetails, "assertionStatement",
        anExecutionDataValue().withDisplayName("Assertion").withValue(assertionStatement).build());
    putNotNull(executionDetails, "assertionStatus",
        anExecutionDataValue().withDisplayName("Assertion Result").withValue(assertionStatus).build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "query", anExecutionDataValue().withDisplayName("Query").withValue(query).build());
    putNotNull(executionDetails, "response",
        anExecutionDataValue()
            .withDisplayName("Response")
            .withValue(abbreviate(response, Constants.SUMMARY_PAYLOAD_LIMIT))
            .build());
    putNotNull(executionDetails, "assertionStatement",
        anExecutionDataValue().withDisplayName("Assertion").withValue(assertionStatement).build());
    putNotNull(executionDetails, "assertionStatus",
        anExecutionDataValue().withDisplayName("Assertion Result").withValue(assertionStatus).build());
    return executionDetails;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String query;
    private String response;
    private String stateName;
    private String assertionStatement;
    private Long startTs;
    private Long endTs;
    private String assertionStatus;
    private ExecutionStatus status;
    private String errorMsg;

    private Builder() {}

    /**
     * A splunk state execution data builder.
     *
     * @return the builder
     */
    public static Builder aSplunkStateExecutionData() {
      return new Builder();
    }

    /**
     * With query builder.
     *
     * @param query the query
     * @return the builder
     */
    public Builder withQuery(String query) {
      this.query = query;
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
      return aSplunkStateExecutionData()
          .withQuery(query)
          .withResponse(response)
          .withStateName(stateName)
          .withAssertionStatement(assertionStatement)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withAssertionStatus(assertionStatus)
          .withStatus(status)
          .withErrorMsg(errorMsg);
    }

    /**
     * Build splunk state execution data.
     *
     * @return the splunk state execution data
     */
    public SplunkStateExecutionData build() {
      SplunkStateExecutionData splunkStateExecutionData = new SplunkStateExecutionData();
      splunkStateExecutionData.setQuery(query);
      splunkStateExecutionData.setResponse(response);
      splunkStateExecutionData.setStateName(stateName);
      splunkStateExecutionData.setAssertionStatement(assertionStatement);
      splunkStateExecutionData.setStartTs(startTs);
      splunkStateExecutionData.setEndTs(endTs);
      splunkStateExecutionData.setAssertionStatus(assertionStatus);
      splunkStateExecutionData.setStatus(status);
      splunkStateExecutionData.setErrorMsg(errorMsg);
      return splunkStateExecutionData;
    }
  }
}
