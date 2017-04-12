package software.wings.sm.states;

import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder.aPhaseExecutionData;

import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ElementNotifyResponseData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.MapperUtils;
import software.wings.utils.Validator;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by rishi on 1/12/17.
 */
public class PhaseSubWorkflow extends SubWorkflowState {
  public PhaseSubWorkflow(String name) {
    super(name, StateType.PHASE.name());
  }

  @Transient @Inject private transient WorkflowExecutionService workflowExecutionService;

  private String uuid;
  private String serviceId;
  private String infraMappingId;

  // Only for rollback phase steps
  @SchemaIgnore private String phaseNameForRollback;

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
        aPhaseExecutionData()
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
                                    .withPhaseNameForRollback(phaseNameForRollback)
                                    .build();
    spawningInstance.getContextElements().push(phaseElement);

    return spawningInstance;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionResponse executionResponse = new ExecutionResponse();
    super.handleStatusSummary(workflowExecutionService, context, response, executionResponse);
    response.values().forEach(notifyResponseData -> {
      if (notifyResponseData instanceof ElementNotifyResponseData) {
        List<ContextElement> notifyElements = ((ElementNotifyResponseData) notifyResponseData).getContextElements();
        if (notifyElements != null && !notifyElements.isEmpty()) {
          if (executionResponse.getContextElements() == null) {
            executionResponse.setContextElements(new ArrayList<>());
          }
          executionResponse.getContextElements().addAll(notifyElements);
        }
      }
    });
    PhaseExecutionData phaseExecutionData = (PhaseExecutionData) context.getStateExecutionData();
    phaseExecutionData.setPhaseExecutionSummary(workflowExecutionService.getPhaseExecutionSummary(
        context.getAppId(), context.getWorkflowExecutionId(), context.getStateExecutionInstanceId()));
    executionResponse.setStateExecutionData(phaseExecutionData);
    return executionResponse;
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

  public String getPhaseNameForRollback() {
    return phaseNameForRollback;
  }

  public void setPhaseNameForRollback(String phaseNameForRollback) {
    this.phaseNameForRollback = phaseNameForRollback;
  }

  @SchemaIgnore

  public String getInfraMappingId() {
    return infraMappingId;
  }

  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }
}
