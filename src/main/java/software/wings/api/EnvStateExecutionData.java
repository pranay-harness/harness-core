package software.wings.api;

import software.wings.sm.StateExecutionData;

/**
 * Created by anubhaw on 10/26/16.
 */
public class EnvStateExecutionData extends StateExecutionData {
  private String workflowId;
  private String workflowExecutionId;
  private String envId;

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getWorkflowExecutionId() {
    return workflowExecutionId;
  }

  public void setWorkflowExecutionId(String workflowExecutionId) {
    this.workflowExecutionId = workflowExecutionId;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }
}
