package software.wings.service.impl.analysis;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.ExecutionStatusData;

import java.util.Objects;

/**
 * Created by rsingh on 5/26/17.
 */
public class LogAnalysisResponse extends ExecutionStatusData {
  private LogAnalysisExecutionData logAnalysisExecutionData;

  public LogAnalysisExecutionData getLogAnalysisExecutionData() {
    return logAnalysisExecutionData;
  }

  public void setLogAnalysisExecutionData(LogAnalysisExecutionData logAnalysisExecutionData) {
    this.logAnalysisExecutionData = logAnalysisExecutionData;
  }

  @Override
  public int hashCode() {
    return Objects.hash(logAnalysisExecutionData);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final LogAnalysisResponse other = (LogAnalysisResponse) obj;
    return Objects.equals(this.logAnalysisExecutionData, other.logAnalysisExecutionData);
  }

  public static final class Builder {
    private LogAnalysisExecutionData logAnalysisExecutionData;
    private ExecutionStatus executionStatus;

    private Builder() {}

    /**
     * An execution status data builder.
     *
     * @return the builder
     */
    public static Builder aLogAnalysisResponse() {
      return new Builder();
    }

    public Builder withLogAnalysisExecutionData(LogAnalysisExecutionData logAnalysisExecutionData) {
      this.logAnalysisExecutionData = logAnalysisExecutionData;
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
      return aLogAnalysisResponse()
          .withLogAnalysisExecutionData(logAnalysisExecutionData)
          .withExecutionStatus(executionStatus);
    }

    /**
     * Build execution status data.
     *
     * @return the execution status data
     */
    public LogAnalysisResponse build() {
      LogAnalysisResponse logAnalysisResponse = new LogAnalysisResponse();
      logAnalysisResponse.setLogAnalysisExecutionData(logAnalysisExecutionData);
      logAnalysisResponse.setExecutionStatus(executionStatus);
      return logAnalysisResponse;
    }
  }
}
