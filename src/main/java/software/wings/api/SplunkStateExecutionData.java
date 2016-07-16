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

  public String getResponse() {
    return response;
  }

  public void setResponse(String response) {
    this.response = response;
  }

  public String getAssertionStatement() {
    return assertionStatement;
  }

  public void setAssertionStatement(String assertionStatement) {
    this.assertionStatement = assertionStatement;
  }

  public String getAssertionStatus() {
    return assertionStatus;
  }

  public void setAssertionStatus(String assertionStatus) {
    this.assertionStatus = assertionStatus;
  }

  public String getQuery() {
    return query;
  }

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
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
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

    public static Builder aSplunkStateExecutionData() {
      return new Builder();
    }

    public Builder withQuery(String query) {
      this.query = query;
      return this;
    }

    public Builder withResponse(String response) {
      this.response = response;
      return this;
    }

    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    public Builder withAssertionStatement(String assertionStatement) {
      this.assertionStatement = assertionStatement;
      return this;
    }

    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public Builder withAssertionStatus(String assertionStatus) {
      this.assertionStatus = assertionStatus;
      return this;
    }

    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public Builder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

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
