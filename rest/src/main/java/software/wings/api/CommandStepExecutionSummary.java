package software.wings.api;

import software.wings.sm.StepExecutionSummary;

/**
 * Created by rishi on 4/4/17.
 */
public class CommandStepExecutionSummary extends StepExecutionSummary {
  private String serviceId;
  private String newContainerServiceName;
  private String oldContainerServiceName;
  private int instanceCount;
  private String clusterName;

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getNewContainerServiceName() {
    return newContainerServiceName;
  }

  public void setNewContainerServiceName(String newContainerServiceName) {
    this.newContainerServiceName = newContainerServiceName;
  }

  public String getOldContainerServiceName() {
    return oldContainerServiceName;
  }

  public void setOldContainerServiceName(String oldContainerServiceName) {
    this.oldContainerServiceName = oldContainerServiceName;
  }

  public int getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }
}
