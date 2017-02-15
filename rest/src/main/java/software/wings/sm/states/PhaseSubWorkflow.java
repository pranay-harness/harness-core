package software.wings.sm.states;

import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.PhaseSubWorkflowExecutionData.PhaseSubWorkflowExecutionDataBuilder.aPhaseSubWorkflowExecutionData;

import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.MapperUtils;
import software.wings.utils.Validator;

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
  private String infraMappingId;

  // Only for rollback phase steps
  @SchemaIgnore private boolean rollback;
  @SchemaIgnore private String rollbackPhaseName;

  @Inject @Transient private transient ServiceResourceService serviceResourceService;

  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();

    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(app.getAppId(), infraMappingId);
    Validator.notNullCheck("InfrastructureMapping", infrastructureMapping);

    ExecutionResponse response = super.execute(context);
    response.setStateExecutionData(
        aPhaseSubWorkflowExecutionData()
            .withComputeProviderId(infrastructureMapping.getComputeProviderSettingId())
            .withInfraMappingId(infraMappingId)
            .withDeploymentType(DeploymentType.valueOf(infrastructureMapping.getDeploymentType()))
            .withServiceId(serviceId)
            .build());
    return response;
  }

  @Override
  protected StateExecutionInstance getSpawningInstance(StateExecutionInstance stateExecutionInstance) {
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(stateExecutionInstance.getAppId(), infraMappingId);
    Validator.notNullCheck("InfrastructureMapping", infrastructureMapping);

    StateExecutionInstance spawningInstance = super.getSpawningInstance(stateExecutionInstance);
    ServiceElement serviceElement = new ServiceElement();
    Service service = serviceResourceService.get(stateExecutionInstance.getAppId(), serviceId, false);
    MapperUtils.mapObject(service, serviceElement);
    PhaseElement phaseElement = aPhaseElement()
                                    .withUuid(uuid)
                                    .withServiceElement(serviceElement)
                                    .withDeploymentType(infrastructureMapping.getDeploymentType())
                                    .withInfraMappingId(infraMappingId)
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

  @SchemaIgnore
  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  @SchemaIgnore
  public boolean isRollback() {
    return rollback;
  }

  public void setRollback(boolean rollback) {
    this.rollback = rollback;
  }

  @SchemaIgnore
  public String getRollbackPhaseName() {
    return rollbackPhaseName;
  }

  public void setRollbackPhaseName(String rollbackPhaseName) {
    this.rollbackPhaseName = rollbackPhaseName;
  }

  public String getInfraMappingId() {
    return infraMappingId;
  }

  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }
}
