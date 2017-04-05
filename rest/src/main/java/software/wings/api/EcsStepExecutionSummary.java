package software.wings.api;

import software.wings.sm.StepExecutionSummary;

/**
 * Created by rishi on 4/4/17.
 */
public class EcsStepExecutionSummary extends StepExecutionSummary {
  private String ecsServiceName;
  private String ecsOldServiceName;
  private int instanceCount;

  public String getEcsServiceName() {
    return ecsServiceName;
  }

  public void setEcsServiceName(String ecsServiceName) {
    this.ecsServiceName = ecsServiceName;
  }

  public String getEcsOldServiceName() {
    return ecsOldServiceName;
  }

  public void setEcsOldServiceName(String ecsOldServiceName) {
    this.ecsOldServiceName = ecsOldServiceName;
  }

  public int getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }
}
