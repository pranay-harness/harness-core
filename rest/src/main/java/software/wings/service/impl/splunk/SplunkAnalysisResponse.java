package software.wings.service.impl.splunk;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.ExecutionStatusData;

/**
 * Created by rsingh on 5/26/17.
 */
public class SplunkAnalysisResponse extends ExecutionStatusData {
  private SplunkExecutionData splunkExecutionData;

  public SplunkExecutionData getSplunkExecutionData() {
    return splunkExecutionData;
  }

  public void setSplunkExecutionData(SplunkExecutionData splunkExecutionData) {
    this.splunkExecutionData = splunkExecutionData;
  }

  public static final class Builder {
    private SplunkExecutionData splunkExecutionData;
    private ExecutionStatus executionStatus;

    private Builder() {}

    /**
     * An execution status data builder.
     *
     * @return the builder
     */
    public static Builder anSplunkAnalysisResponse() {
      return new Builder();
    }

    public Builder withSplunkExecutionData(SplunkExecutionData splunkExecutionData) {
      this.splunkExecutionData = splunkExecutionData;
      return this;
    }

    public Builder withExecutionStatus(ExecutionStatus executionStatus) {
      this.executionStatus = executionStatus;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anSplunkAnalysisResponse()
          .withSplunkExecutionData(splunkExecutionData)
          .withExecutionStatus(executionStatus);
    }

    /**
     * Build execution status data.
     *
     * @return the execution status data
     */
    public SplunkAnalysisResponse build() {
      SplunkAnalysisResponse splunkAnalysisResponse = new SplunkAnalysisResponse();
      splunkAnalysisResponse.setSplunkExecutionData(splunkExecutionData);
      splunkAnalysisResponse.setExecutionStatus(executionStatus);
      return splunkAnalysisResponse;
    }
  }
}
