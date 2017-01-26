package software.wings.sm.states;

import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.PhaseSubWorkflowExecutionData.PhaseSubWorkflowExecutionDataBuilder.aPhaseSubWorkflowExecutionData;

import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Service;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.utils.MapperUtils;

import javax.inject.Inject;

/**
 * Created by rishi on 1/12/17.
 */
public class PhaseSubWorkflow extends SubWorkflowState {
  public PhaseSubWorkflow(String name) {
    super(name, StateType.PHASE.name());
  }

  private String uuid;
  private String serviceId;
  private String computeProviderId;
  private DeploymentType deploymentType;

  // Only relevant for custom kubernetes environment
  private String deploymentMasterId;

  @Inject @Transient private ServiceResourceService serviceResourceService;

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

  @Override
  protected StateExecutionInstance getSpawningInstance(StateExecutionInstance stateExecutionInstance) {
    StateExecutionInstance spawningInstance = super.getSpawningInstance(stateExecutionInstance);
    ServiceElement serviceElement = new ServiceElement();
    Service service = serviceResourceService.get(stateExecutionInstance.getAppId(), serviceId, false);
    MapperUtils.mapObject(service, serviceElement);
    PhaseElement phaseElement = aPhaseElement()
                                    .withUuid(uuid)
                                    .withServiceElement(serviceElement)
                                    .withComputeProviderId(computeProviderId)
                                    .withDeploymentType(deploymentType)
                                    .withDeploymentMasterId(deploymentMasterId)
                                    .build();
    spawningInstance.getContextElements().push(phaseElement);
    spawningInstance.setContextElement(phaseElement);

    return spawningInstance;
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
