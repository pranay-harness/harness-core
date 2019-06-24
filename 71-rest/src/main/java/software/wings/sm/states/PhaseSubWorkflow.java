package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder.aPhaseExecutionData;

import com.google.inject.Inject;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.SweepingOutput;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.serializer.KryoUtils;
import io.harness.serializer.MapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseElement.PhaseElementBuilder;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.service.impl.SweepingOutputServiceImpl;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.settings.SettingValue;
import software.wings.sm.ContextElement;
import software.wings.sm.ElementNotifyResponseData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.SpawningExecutionResponse;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 1/12/17.
 */
@Slf4j
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

  @Inject @Transient private transient ArtifactService artifactService;

  @Inject @Transient private transient SweepingOutputService sweepingOutputService;

  @Inject @Transient private transient FeatureFlagService featureFlagService;

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();

    Service service = null;
    InfrastructureMapping infrastructureMapping = null;
    String serviceIdExpression = null;
    String infraMappingIdExpression = null;
    List<TemplateExpression> templateExpressions = this.getTemplateExpressions();
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
        throw new WingsException("Service templatized so service infrastructure should be templatized", USER);
      }
    } else {
      if (serviceId != null) {
        service = serviceResourceService.get(app.getAppId(), serviceId, false);
        Validator.notNullCheck("Service might have been deleted", service, USER);
      }
    }
    if (infraMappingIdExpression == null) {
      if (infraMappingId != null) {
        infrastructureMapping = infrastructureMappingService.get(app.getAppId(), infraMappingId);
        Validator.notNullCheck("Service Infrastructure might have been deleted", infrastructureMapping, USER);
      }
    }

    if (service != null && infrastructureMapping != null
        && !service.getUuid().equals(infrastructureMapping.getServiceId())) {
      throw new InvalidRequestException("Service [" + service.getName()
          + "] is not associated with the Service Infrastructure [" + infrastructureMapping.getName() + "]");
    }

    ExecutionResponse response =
        getSpawningExecutionResponse(context, workflowStandardParams, service, infrastructureMapping);

    PhaseExecutionDataBuilder phaseExecutionDataBuilder = aPhaseExecutionData();
    if (infrastructureMapping != null) {
      DeploymentType deploymentType =
          serviceResourceService.getDeploymentType(infrastructureMapping, null, infrastructureMapping.getServiceId());

      phaseExecutionDataBuilder.withComputeProviderId(infrastructureMapping.getComputeProviderSettingId())
          .withComputeProviderName(infrastructureMapping.getComputeProviderName())
          .withComputeProviderType(
              SettingValue.SettingVariableTypes.valueOf(infrastructureMapping.getComputeProviderType())
                  .getDisplayName())
          .withInfraMappingId(infrastructureMapping.getUuid())
          .withInfraMappingName(infrastructureMapping.getName())
          .withDeploymentType(deploymentType.getDisplayName());
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
        getSpawningInstance(context, workflowStandardParams, stateExecutionInstance, service, infrastructureMapping);
    executionResponse.add(childStateExecutionInstance);
    correlationIds.add(stateExecutionInstance.getUuid());

    executionResponse.setAsync(true);
    executionResponse.setCorrelationIds(correlationIds);
    return executionResponse;
  }

  private StateExecutionInstance getSpawningInstance(ExecutionContext context,
      WorkflowStandardParams workflowStandardParams, StateExecutionInstance stateExecutionInstance, Service service,
      InfrastructureMapping infrastructureMapping) {
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

    String accountId = context.getAccountId();

    if (infrastructureMapping != null) {
      DeploymentType deploymentType =
          serviceResourceService.getDeploymentType(infrastructureMapping, null, infrastructureMapping.getServiceId());

      phaseElementBuilder.withDeploymentType(deploymentType.name()).withInfraMappingId(infrastructureMapping.getUuid());
    }

    if (stateExecutionInstance.getRollbackPhaseName() != null) {
      phaseElementBuilder.withPhaseNameForRollback(stateExecutionInstance.getRollbackPhaseName());
    }

    if (isNotEmpty(getVariableOverrides())) {
      phaseElementBuilder.withVariableOverrides(getVariableOverrides());
    }

    PhaseElement phaseElement = phaseElementBuilder.build();

    spawningInstance.getContextElements().push(phaseElement);
    spawningInstance.setContextElement(phaseElement);

    if (service != null && !isRollback()) {
      if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        WorkflowExecution workflowExecution =
            workflowExecutionService.getWorkflowExecution(workflowStandardParams.getAppId(),
                context.getWorkflowExecutionId()); // TODO: performance issue -filter query to get only execution args
                                                   // and artifacts
        saveArtifactsFromVariables(
            context, workflowStandardParams, stateExecutionInstance, service, phaseElement, workflowExecution);
      }
    }

    if (isRollback() && workflowStandardParams.getWorkflowElement() != null) {
      // if last successful deployment found, save it in sweeping output
      if (workflowStandardParams.getWorkflowElement().getLastGoodDeploymentUuid() != null) {
        WorkflowExecution workflowExecution =
            workflowExecutionService.getWorkflowExecution(workflowStandardParams.getAppId(),
                workflowStandardParams.getWorkflowElement()
                    .getLastGoodDeploymentUuid()); // //TODO: performance issue -filter query to get only execution args
                                                   // and artifacts

        if (workflowExecution == null) {
          logger.error("ERROR: Last Good Deployment ID is not found - lastGoodDeploymentUuid: {}",
              workflowStandardParams.getWorkflowElement().getLastGoodDeploymentUuid());
          throw new InvalidRequestException("Last Good Deployment ID is not found");
        }
        if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
          saveArtifactsFromVariablesForRollback(context, workflowStandardParams, stateExecutionInstance, service,
              phaseElement, accountId, workflowExecution);
        } else {
          phaseElementBuilder.withRollbackArtifactId(findRollbackArtifactId(service, workflowExecution));
        }
      } else {
        // save current artifacts in sweeping output
        if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
          String phaseExecutionId =
              context.getWorkflowExecutionId() + phaseElement.getUuid() + phaseElement.getPhaseName();
          List<ArtifactVariable> artifactVariables = workflowStandardParams.getWorkflowElement().getArtifactVariables();
          WorkflowExecution workflowExecution = workflowExecutionService.getWorkflowExecution(
              workflowStandardParams.getAppId(), context.getWorkflowExecutionId());
          for (ArtifactVariable artifactVariable : artifactVariables) {
            if (artifactVariable.getEntityType().equals(EntityType.SERVICE)) {
              if (artifactVariable.getEntityId().equals(service.getUuid())) {
                Artifact artifact = getArtifactByUuid(workflowExecution.getArtifacts(), artifactVariable.getValue());
                saveArtifactToSweepingOutput(
                    stateExecutionInstance.getAppId(), phaseExecutionId, artifactVariable, artifact);
              }
            }
          }
        }
      }
    }
    return spawningInstance;
  }

  private void saveArtifactToSweepingOutput(
      String appId, String phaseExecutionId, ArtifactVariable artifactVariable, Artifact artifact) {
    if (artifact != null) {
      sweepingOutputService.save(
          SweepingOutputServiceImpl
              .prepareSweepingOutputBuilder(appId, null, null, phaseExecutionId, null, SweepingOutput.Scope.PHASE)
              .name(artifactVariable.getName())
              .output(KryoUtils.asDeflatedBytes(artifact))
              .build());
    }
  }

  private String findRollbackArtifactId(Service service, WorkflowExecution workflowExecution) {
    String rollbackArtifactId = null;
    if (workflowExecution.getExecutionArgs() != null && workflowExecution.getExecutionArgs().getArtifacts() != null) {
      if (service != null) {
        for (Artifact artifact : workflowExecution.getExecutionArgs().getArtifacts()) {
          if (isNotEmpty(service.getArtifactStreamIds())
              && service.getArtifactStreamIds().contains(artifact.getArtifactStreamId())) {
            rollbackArtifactId = artifact.getUuid();
            break;
          }
        }
      }
      if (rollbackArtifactId == null) {
        // This can happen in case of build workflow
        rollbackArtifactId = workflowExecution.getExecutionArgs().getArtifacts().get(0).getUuid();
      }
    }
    return rollbackArtifactId;
  }

  private void saveArtifactsFromVariables(ExecutionContext context, WorkflowStandardParams workflowStandardParams,
      StateExecutionInstance stateExecutionInstance, Service service, PhaseElement phaseElement,
      WorkflowExecution workflowExecution) {
    // TODO: why are we adding phaseName here? how to handle phase name change?
    String phaseExecutionId = context.getWorkflowExecutionId() + phaseElement.getUuid() + phaseElement.getPhaseName();
    // go over all artifact variables of service
    // throw exception if variable does not exist in service
    List<ArtifactVariable> artifactVariables = workflowStandardParams.getWorkflowElement().getArtifactVariables();
    if (isNotEmpty(artifactVariables)) {
      for (ArtifactVariable artifactVariable : artifactVariables) {
        if (artifactVariable.getEntityType().equals(EntityType.SERVICE)) {
          if (artifactVariable.getEntityId().equals(service.getUuid())) {
            //            Artifact artifact = artifactService.get(accountId, artifactVariable.getValue());
            Artifact artifact = getArtifactByUuid(workflowExecution.getArtifacts(), artifactVariable.getValue());
            saveArtifactToSweepingOutput(
                stateExecutionInstance.getAppId(), phaseExecutionId, artifactVariable, artifact);
          } else {
            throw new WingsException(format(
                "Artifact Variable %s not defined in service %s", artifactVariable.getName(), service.getName()));
          }
        }
      }
    }
  }

  private Artifact getArtifactByUuid(List<Artifact> artifacts, String artifactId) {
    if (artifactId == null) {
      return null;
    }
    if (isNotEmpty(artifacts)) {
      for (Artifact artifact : artifacts) {
        if (artifact.getUuid().equals(artifactId)) {
          return artifact;
        }
      }
    }
    return null;
  }

  private void saveArtifactsFromVariablesForRollback(ExecutionContext context,
      WorkflowStandardParams workflowStandardParams, StateExecutionInstance stateExecutionInstance, Service service,
      PhaseElement phaseElement, String accountId, WorkflowExecution lastSuccessfulWorkflowExecution) {
    String phaseExecutionId = context.getWorkflowExecutionId() + phaseElement.getUuid() + phaseElement.getPhaseName();
    // go over all artifact variables of service
    // throw exception if variable does not exist in service
    List<ArtifactVariable> artifactVariables = workflowStandardParams.getWorkflowElement().getArtifactVariables();
    List<ArtifactVariable> previousArtifactVariables = null;
    if (lastSuccessfulWorkflowExecution.getExecutionArgs() != null
        && lastSuccessfulWorkflowExecution.getExecutionArgs().getArtifacts() != null) {
      previousArtifactVariables = lastSuccessfulWorkflowExecution.getExecutionArgs().getArtifactVariables();
    }
    if (isNotEmpty(artifactVariables) && isNotEmpty(previousArtifactVariables)) {
      for (ArtifactVariable artifactVariable : artifactVariables) {
        if (artifactVariable.getEntityType().equals(EntityType.SERVICE)) {
          if (artifactVariable.getEntityId().equals(service.getUuid())) {
            String artifactId = getArtifactIdFromPreviousArtifactVariables(artifactVariable, previousArtifactVariables);
            //            Artifact artifact = artifactService.get(accountId, artifactVariable.getValue());
            Artifact artifact = getArtifactByUuid(lastSuccessfulWorkflowExecution.getArtifacts(), artifactId);
            saveArtifactToSweepingOutput(
                stateExecutionInstance.getAppId(), phaseExecutionId, artifactVariable, artifact);
            // todo: save workflow level artifacts overridden by this service at phase level to access artifact like
            // artifact1.buildNo
          } else {
            throw new WingsException(format(
                "Artifact Variable %s not defined in service %s", artifactVariable.getName(), service.getName()));
          }
        }
      }
    }
  }

  private String getArtifactIdFromPreviousArtifactVariables(
      ArtifactVariable artifactVariable, List<ArtifactVariable> artifactVariables) {
    for (ArtifactVariable variable : artifactVariables) {
      if (variable.getName().equals(artifactVariable.getName())
          && variable.getEntityId().equals(artifactVariable.getEntityId())) {
        return variable.getValue();
      }
    }
    return null;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
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
