package software.wings.sm.states;

import static software.wings.api.PhaseSubWorkflowExecutionData.PhaseSubWorkflowExecutionDataBuilder.aPhaseSubWorkflowExecutionData;

import software.wings.beans.DeploymentType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;

/**
 * Created by rishi on 1/12/17.
 */
public class PhaseSubWorkflow extends SubWorkflowState {
  public PhaseSubWorkflow(String name) {
    super(name, StateType.PHASE.name());
  }

  private String serviceId;
  private String computeProviderId;
  private DeploymentType deploymentType;

  // Only relevant for custom kubernetes environment
  private String deploymentMasterId;

  @Override
  public ExecutionResponse execute(ExecutionContext contextIntf) {
    ExecutionResponse response = super.execute(contextIntf);
    response.setStateExecutionData(aPhaseSubWorkflowExecutionData()
                                       .withComputeProviderId(computeProviderId)
                                       .withDeploymentType(deploymentType)
                                       .withServiceId(serviceId)
                                       .withDeploymentMasterId(deploymentMasterId)
                                       .build());
    return response;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getComputeProviderId() {
    return computeProviderId;
  }

  public void setComputeProviderId(String computeProviderId) {
    this.computeProviderId = computeProviderId;
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
}
