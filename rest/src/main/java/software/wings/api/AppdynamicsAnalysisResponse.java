package software.wings.api;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.ExecutionStatusData;

/**
 * Created by rsingh on 5/26/17.
 */
public class AppdynamicsAnalysisResponse extends ExecutionStatusData {
  private AppDynamicsExecutionData appDynamicsExecutionData;

  public AppDynamicsExecutionData getAppDynamicsExecutionData() {
    return appDynamicsExecutionData;
  }

  public void setAppDynamicsExecutionData(AppDynamicsExecutionData appDynamicsExecutionData) {
    this.appDynamicsExecutionData = appDynamicsExecutionData;
  }

  public static final class Builder {
    private AppDynamicsExecutionData appDynamicsExecutionData;
    private ExecutionStatus executionStatus;

    private Builder() {}

    /**
     * An execution status data builder.
     *
     * @return the builder
     */
    public static Builder anAppdynamicsAnalysisResponse() {
      return new Builder();
    }

    public Builder withAppDynamicsExecutionData(AppDynamicsExecutionData appDynamicsExecutionData) {
      this.appDynamicsExecutionData = appDynamicsExecutionData;
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
      return anAppdynamicsAnalysisResponse()
          .withAppDynamicsExecutionData(appDynamicsExecutionData)
          .withExecutionStatus(executionStatus);
    }

    /**
     * Build execution status data.
     *
     * @return the execution status data
     */
    public AppdynamicsAnalysisResponse build() {
      AppdynamicsAnalysisResponse appdynamicsAnalysisResponse = new AppdynamicsAnalysisResponse();
      appdynamicsAnalysisResponse.setAppDynamicsExecutionData(appDynamicsExecutionData);
      appdynamicsAnalysisResponse.setExecutionStatus(executionStatus);
      return appdynamicsAnalysisResponse;
    }
  }
}
