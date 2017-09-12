package software.wings.api;

import software.wings.beans.command.CodeDeployParams;
import software.wings.sm.StepExecutionSummary;

/**
 * Created by rishi on 4/4/17.
 */
public class CommandStepExecutionSummary extends StepExecutionSummary {
  private String serviceId;
  private String newContainerServiceName;
  private String oldContainerServiceName;
  private Integer newServiceRunningInstanceCount;
  private Integer oldServiceRunningInstanceCount;
  private Integer newServicePreviousInstanceCount;
  private Integer oldServicePreviousInstanceCount;
  private String clusterName;

  private CodeDeployParams codeDeployParams;
  private CodeDeployParams oldCodeDeployParams;

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

  public Integer getNewServiceRunningInstanceCount() {
    return newServiceRunningInstanceCount;
  }

  public void setNewServiceRunningInstanceCount(Integer newServiceRunningInstanceCount) {
    this.newServiceRunningInstanceCount = newServiceRunningInstanceCount;
  }

  public Integer getOldServiceRunningInstanceCount() {
    return oldServiceRunningInstanceCount;
  }

  public void setOldServiceRunningInstanceCount(Integer oldServiceRunningInstanceCount) {
    this.oldServiceRunningInstanceCount = oldServiceRunningInstanceCount;
  }

  public Integer getNewServicePreviousInstanceCount() {
    return newServicePreviousInstanceCount;
  }

  public void setNewServicePreviousInstanceCount(Integer newServicePreviousInstanceCount) {
    this.newServicePreviousInstanceCount = newServicePreviousInstanceCount;
  }

  public Integer getOldServicePreviousInstanceCount() {
    return oldServicePreviousInstanceCount;
  }

  public void setOldServicePreviousInstanceCount(Integer oldServicePreviousInstanceCount) {
    this.oldServicePreviousInstanceCount = oldServicePreviousInstanceCount;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public CodeDeployParams getCodeDeployParams() {
    return codeDeployParams;
  }

  public void setCodeDeployParams(CodeDeployParams codeDeployParams) {
    this.codeDeployParams = codeDeployParams;
  }

  public CodeDeployParams getOldCodeDeployParams() {
    return oldCodeDeployParams;
  }

  public void setOldCodeDeployParams(CodeDeployParams oldCodeDeployParams) {
    this.oldCodeDeployParams = oldCodeDeployParams;
  }
}
