package software.wings.sm.states;

import software.wings.beans.DeploymentType;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;

/**
 * Created by rishi on 1/12/17.
 */
public class PhaseSubWorkflow extends SubWorkflowState {
  public PhaseSubWorkflow(String name) {
    super(name, StateType.PHASE.name());
  }

  private String serviceId;
  private String computerProviderId;
  private DeploymentType deploymentType;

  // Only relevant for custom kubernetes environment
  private String deploymentMasterId;

  @Override
  protected StateExecutionInstance getSpawningInstance(StateExecutionInstance stateExecutionInstance) {
    StateExecutionInstance spawningInstance = super.getSpawningInstance(stateExecutionInstance);
    spawningInstance.getParams().put("serviceId", serviceId);
    spawningInstance.getParams().put("computerProviderId", computerProviderId);
    spawningInstance.getParams().put("deploymentType", deploymentType);
    if (deploymentMasterId != null) {
      spawningInstance.getParams().put("deploymentMasterId", deploymentMasterId);
    }
    return spawningInstance;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getComputerProviderId() {
    return computerProviderId;
  }

  public void setComputerProviderId(String computerProviderId) {
    this.computerProviderId = computerProviderId;
  }

  public DeploymentType getDeploymentType() {
    return deploymentType;
  }

  public void setDeploymentType(DeploymentType deploymentType) {
    this.deploymentType = deploymentType;
  }

  public String getDeploymentMasterId() {
    return deploymentMasterId;
  }

  public void setDeploymentMasterId(String deploymentMasterId) {
    this.deploymentMasterId = deploymentMasterId;
  }

  public static class PhaseSubWorkflowExecutionData extends StateExecutionData implements NotifyResponseData {
    private String serviceId;
    private String computerProviderId;
    private DeploymentType deploymentType;

    private String deploymentMasterId;

    private List<String> instanceIds;

    public String getServiceId() {
      return serviceId;
    }

    public void setServiceId(String serviceId) {
      this.serviceId = serviceId;
    }

    public String getComputerProviderId() {
      return computerProviderId;
    }

    public void setComputerProviderId(String computerProviderId) {
      this.computerProviderId = computerProviderId;
    }

    public DeploymentType getDeploymentType() {
      return deploymentType;
    }

    public void setDeploymentType(DeploymentType deploymentType) {
      this.deploymentType = deploymentType;
    }

    public String getDeploymentMasterId() {
      return deploymentMasterId;
    }

    public void setDeploymentMasterId(String deploymentMasterId) {
      this.deploymentMasterId = deploymentMasterId;
    }

    public List<String> getInstanceIds() {
      return instanceIds;
    }

    public void setInstanceIds(List<String> instanceIds) {
      this.instanceIds = instanceIds;
    }
  }
}
