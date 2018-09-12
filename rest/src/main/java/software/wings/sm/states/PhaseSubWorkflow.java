package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder.aPhaseExecutionData;

import com.google.inject.Inject;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.exception.WingsException;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseElement.PhaseElementBuilder;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.exception.InvalidRequestException;
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
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.MapperUtils;
import software.wings.utils.Validator;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 1/12/17.
 */
public class PhaseSubWorkflow extends SubWorkflowState {
  public PhaseSubWorkflow(String name) {
    super(name, StateType.PHASE.name());
  }

  @Transient @Inject private transient WorkflowExecutionService workflowExecutionService;
  private static final Logger logger = LoggerFactory.getLogger(PhaseSubWorkflow.class);

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

    Service service = null;
    InfrastructureMapping infrastructureMapping = null;
    String serviceIdExpression = null;
    String infraMappingIdExpression = null;
    List<TemplateExpression> templateExpressions = getTemplateExpressions();
    if (templateExpressions != null) {
      for (TemplateExpression templateExpression : templateExpressions) {
        String fieldName = templateExpression.getFieldName();
        if (fieldName != null && fieldName.equals("serviceId")) {
          serviceIdExpression = templateExpression.getExpression();
          service = templateExpressionProcessor.resolveService(context, app, templateExpression);
        } else if (fieldName != null && fieldName.equals("infraMappingId")) {
          infraMappingIdExpression = templateExpression.getExpression();
          infrastructureMapping =
              templateExpressionProcessor.resolveInfraMapping(context, app.getAppId(), templateExpression);
        }
      }
    }
    if (serviceIdExpression != null) {
      if (infraMappingIdExpression == null) {
        throw new WingsException("Service templatized so service infrastructure should be templatized");
      }
    } else {
      if (serviceId != null) {
        service = serviceResourceService.get(app.getAppId(), serviceId, false);
        Validator.notNullCheck("Service", service);
      }
    }
    if (infraMappingIdExpression == null) {
      if (infraMappingId != null) {
        infrastructureMapping = infrastructureMappingService.get(app.getAppId(), infraMappingId);
        Validator.notNullCheck("InfrastructureMapping", infrastructureMapping);
      }
    }

    ExecutionResponse response =
        getSpawningExecutionResponse(context, workflowStandardParams, service, infrastructureMapping);

    PhaseExecutionDataBuilder phaseExecutionDataBuilder = aPhaseExecutionData();
    if (infrastructureMapping != null) {
      phaseExecutionDataBuilder.withComputeProviderId(infrastructureMapping.getComputeProviderSettingId())
          .withComputeProviderName(infrastructureMapping.getComputeProviderName())
          .withComputeProviderType(
              SettingValue.SettingVariableTypes.valueOf(infrastructureMapping.getComputeProviderType())
                  .getDisplayName())
          .withInfraMappingId(infrastructureMapping.getUuid())
          .withInfraMappingName(infrastructureMapping.getName())
          .withDeploymentType(DeploymentType.valueOf(infrastructureMapping.getDeploymentType()).getDisplayName());
    }
    if (service != null) {
      phaseExecutionDataBuilder.withServiceId(service.getUuid()).withServiceName(service.getName());
    }
    PhaseExecutionData phaseExecutionData = phaseExecutionDataBuilder.build();
    if (infrastructureMapping instanceof ContainerInfrastructureMapping) {
      phaseExecutionData.setClusterName(((ContainerInfrastructureMapping) infrastructureMapping).getClusterName());

      StateExecutionData stateExecutionData = context.getStateExecutionData();
      if (stateExecutionData != null) {
        ContextElement element = stateExecutionData.getElement();
        if (element != null) {
          if (ContextElementType.CONTAINER_SERVICE == element.getElementType()) {
            ContainerServiceElement containerElement = (ContainerServiceElement) element;
            phaseExecutionData.setContainerServiceName(containerElement.getName());
          }
        }
      }
    }

    response.setStateExecutionData(phaseExecutionData);
    return response;
  }

  private ExecutionResponse getSpawningExecutionResponse(ExecutionContext context,
      WorkflowStandardParams workflowStandardParams, Service service, InfrastructureMapping infrastructureMapping) {
    ExecutionContextImpl contextImpl = (ExecutionContextImpl) context;
    StateExecutionInstance stateExecutionInstance = contextImpl.getStateExecutionInstance();
    List<String> correlationIds = new ArrayList<>();

    SpawningExecutionResponse executionResponse = new SpawningExecutionResponse();

    StateExecutionInstance childStateExecutionInstance =
        getSpawningInstance(workflowStandardParams, stateExecutionInstance, service, infrastructureMapping);
    executionResponse.add(childStateExecutionInstance);
    correlationIds.add(stateExecutionInstance.getUuid());

    executionResponse.setAsync(true);
    executionResponse.setCorrelationIds(correlationIds);
    return executionResponse;
  }

  private StateExecutionInstance getSpawningInstance(WorkflowStandardParams workflowStandardParams,
      StateExecutionInstance stateExecutionInstance, Service service, InfrastructureMapping infrastructureMapping) {
    StateExecutionInstance spawningInstance = super.getSpawningInstance(stateExecutionInstance);

    PhaseElementBuilder phaseElementBuilder = aPhaseElement()
                                                  .withUuid(getId())
                                                  .withPhaseName(stateExecutionInstance.getDisplayName())
                                                  .withAppId(stateExecutionInstance.getAppId())
                                                  .withPhaseNameForRollback(phaseNameForRollback);

    if (service != null) {
      ServiceElement serviceElement = new ServiceElement();
      MapperUtils.mapObject(service, serviceElement);
      phaseElementBuilder.withServiceElement(serviceElement);
    }

    if (isRollback() && workflowStandardParams.getWorkflowElement() != null
        && workflowStandardParams.getWorkflowElement().getLastGoodDeploymentUuid() != null) {
      WorkflowExecution workflowExecution = workflowExecutionService.getWorkflowExecution(
          workflowStandardParams.getAppId(), workflowStandardParams.getWorkflowElement().getLastGoodDeploymentUuid());

      if (workflowExecution == null) {
        logger.error("ERROR: Last Good Deployment ID is not found - lastGoodDeploymentUuid: {}",
            workflowStandardParams.getWorkflowElement().getLastGoodDeploymentUuid());
        throw new InvalidRequestException("Last Good Deployment ID is not found");
      }
      if (workflowExecution.getExecutionArgs() != null && workflowExecution.getExecutionArgs().getArtifacts() != null) {
        String rollbackArtifactId = null;
        if (service != null) {
          for (Artifact artifact : workflowExecution.getExecutionArgs().getArtifacts()) {
            if (artifact.getServiceIds().contains(service.getUuid())) {
              rollbackArtifactId = artifact.getUuid();
              break;
            }
          }
        }

        if (rollbackArtifactId == null) {
          // This can happen in case of build workflow
          rollbackArtifactId = workflowExecution.getExecutionArgs().getArtifacts().get(0).getUuid();
        }
        phaseElementBuilder.withRollbackArtifactId(rollbackArtifactId);
      }
    }

    if (infrastructureMapping != null) {
      phaseElementBuilder.withDeploymentType(infrastructureMapping.getDeploymentType())
          .withInfraMappingId(infrastructureMapping.getUuid());
    }

    if (stateExecutionInstance.getRollbackPhaseName() != null) {
      phaseElementBuilder.withPhaseNameForRollback(stateExecutionInstance.getRollbackPhaseName());
    }

    if (isNotEmpty(getVariableOverrides())) {
      phaseElementBuilder.withVariableOverrides(getVariableOverrides());
    }

    final PhaseElement phaseElement = phaseElementBuilder.build();

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
        if (isNotEmpty(notifyElements)) {
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
