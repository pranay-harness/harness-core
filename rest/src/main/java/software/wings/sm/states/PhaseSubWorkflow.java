package software.wings.sm.states;

import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder.aPhaseExecutionData;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.dl.PageRequest;
import software.wings.exception.WingsException;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.settings.SettingValue;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ElementNotifyResponseData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.MapperUtils;
import software.wings.utils.Validator;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  @Inject @Transient private transient TemplateExpressionProcessor templateExpressionProcessor;

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();

    Service service;
    InfrastructureMapping infrastructureMapping = null;
    String serviceIdExpression = null;
    String infraMappingIdExpression = null;

    List<TemplateExpression> templateExpressions = getTemplateExpressions();
    if (templateExpressions != null && templateExpressions.isEmpty()) {
      for (TemplateExpression templateExpression : templateExpressions) {
        String fieldName = templateExpression.getFieldName();
        if (fieldName != null && fieldName.equals("serviceId")) {
          serviceIdExpression = templateExpression.getExpression();
        } else if (fieldName != null && fieldName.equals("infraMappingId")) {
          infraMappingIdExpression = templateExpression.getExpression();
        }
      }
    }
    if (serviceIdExpression != null) {
      if (infraMappingIdExpression == null) {
        throw new WingsException("Service templatized so service infrastructure should be templatized");
      }
      service = templateExpressionProcessor.resolveService(context, app, serviceIdExpression);
    } else {
      service = serviceResourceService.get(app.getAppId(), serviceId, false);
    }
    if (infraMappingIdExpression != null) {
      infrastructureMapping =
          templateExpressionProcessor.resolveInfraMapping(context, app, service.getUuid(), infraMappingIdExpression);
    } else {
      infrastructureMapping = infrastructureMappingService.get(app.getAppId(), infraMappingId);
    }
    Validator.notNullCheck("InfrastructureMapping", infrastructureMapping);

    ExecutionResponse response = getSpawningExecutionResponse(context, service, infrastructureMapping);

    PhaseExecutionData phaseExecutionData =
        aPhaseExecutionData()
            .withComputeProviderId(infrastructureMapping.getComputeProviderSettingId())
            .withComputeProviderName(infrastructureMapping.getComputeProviderName())
            .withComputeProviderType(
                SettingValue.SettingVariableTypes.valueOf(infrastructureMapping.getComputeProviderType())
                    .getDisplayName())
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withInfraMappingName(infrastructureMapping.getDisplayName())
            .withDeploymentType(DeploymentType.valueOf(infrastructureMapping.getDeploymentType()).getDisplayName())
            .withServiceId(service.getUuid())
            .withServiceName(service.getName())
            .build();
    if (infrastructureMapping instanceof ContainerInfrastructureMapping) {
      phaseExecutionData.setClusterName(((ContainerInfrastructureMapping) infrastructureMapping).getClusterName());
    }
    response.setStateExecutionData(phaseExecutionData);
    return response;
  }

  private ExecutionResponse getSpawningExecutionResponse(
      ExecutionContext context, Service service, InfrastructureMapping infrastructureMapping) {
    ExecutionContextImpl contextImpl = (ExecutionContextImpl) context;
    StateExecutionInstance stateExecutionInstance = contextImpl.getStateExecutionInstance();
    List<String> correlationIds = new ArrayList<>();

    SpawningExecutionResponse executionResponse = new SpawningExecutionResponse();

    StateExecutionInstance childStateExecutionInstance =
        getSpawningInstance(stateExecutionInstance, service, infrastructureMapping);
    executionResponse.add(childStateExecutionInstance);
    correlationIds.add(stateExecutionInstance.getUuid());

    executionResponse.setAsync(true);
    executionResponse.setCorrelationIds(correlationIds);
    return executionResponse;
  }

  private InfrastructureMapping resolveInfraMapping(
      ExecutionContext context, Application app, String serviceId, String expression) {
    String displayName = context.renderExpression(expression);
    PageRequest<InfrastructureMapping> pageRequest =
        aPageRequest().addFilter("appId", EQ, app.getUuid()).addFilter("serviceId", EQ, serviceId).build();
    List<InfrastructureMapping> infraMappings = infrastructureMappingService.list(pageRequest);
    if (infraMappings == null || infraMappings.isEmpty()) {
      return null;
    }
    Optional<InfrastructureMapping> infraMapping =
        infraMappings.stream().filter(infrastructureMapping -> infrastructureMapping.equals(displayName)).findFirst();
    if (infraMapping.isPresent()) {
      return infraMapping.get();
    }
    return null;
  }

  private Service resolveService(ExecutionContext context, Application app, String expression) {
    String serviceName = context.renderExpression(expression);
    PageRequest<Service> pageRequest =
        aPageRequest().addFilter("appId", EQ, app.getUuid()).addFilter("name", EQ, serviceName).build();
    List<Service> services = serviceResourceService.list(pageRequest, false, false);
    if (services != null && !services.isEmpty()) {
      return services.get(0);
    }
    return null;
  }

  private StateExecutionInstance getSpawningInstance(
      StateExecutionInstance stateExecutionInstance, Service service, InfrastructureMapping infrastructureMapping) {
    StateExecutionInstance spawningInstance = super.getSpawningInstance(stateExecutionInstance);
    ServiceElement serviceElement = new ServiceElement();
    MapperUtils.mapObject(service, serviceElement);
    PhaseElement phaseElement = aPhaseElement()
                                    .withUuid(getId())
                                    .withServiceElement(serviceElement)
                                    .withDeploymentType(infrastructureMapping.getDeploymentType())
                                    .withInfraMappingId(infrastructureMapping.getUuid())
                                    .withPhaseNameForRollback(phaseNameForRollback)
                                    .build();
    spawningInstance.getContextElements().push(phaseElement);
    spawningInstance.setContextElement(phaseElement);

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
