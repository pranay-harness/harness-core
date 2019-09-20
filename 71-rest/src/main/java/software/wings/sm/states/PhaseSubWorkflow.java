package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder.aPhaseExecutionData;

import com.google.inject.Inject;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.SweepingOutput;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.serializer.KryoUtils;
import io.harness.serializer.MapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
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
import software.wings.beans.Environment;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.PhaseSubWorkflowHelperService;
import software.wings.service.impl.SweepingOutputServiceImpl;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
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
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 1/12/17.
 */
@Slf4j
public class PhaseSubWorkflow extends SubWorkflowState {
  public static final String SERVICE_ID = "serviceId";
  public static final String INFRA_MAPPING_ID = "infraMappingId";
  public static final String INFRA_DEFINITION_ID = "infraDefinitionId";

  public PhaseSubWorkflow(String name) {
    super(name, StateType.PHASE.name());
  }

  @Transient @Inject private transient WorkflowExecutionService workflowExecutionService;

  private String uuid;
  private String serviceId;
  private String infraMappingId;
  private String infraDefinitionId;

  // Only for rollback phase steps
  @SchemaIgnore private String phaseNameForRollback;

  @Inject @Transient private transient ServiceResourceService serviceResourceService;

  @Inject @Transient private transient InfrastructureDefinitionService infrastructureDefinitionService;

  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient private transient TemplateExpressionProcessor templateExpressionProcessor;

  @Inject @Transient private transient ArtifactService artifactService;

  @Inject @Transient private transient ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  @Inject @Transient private transient SweepingOutputService sweepingOutputService;

  @Inject @Transient private transient FeatureFlagService featureFlagService;
  @Inject @Transient private transient PhaseSubWorkflowHelperService phaseSubWorkflowHelperService;

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();

    TemplateExpression serviceTemplateExpression = null, infraDefinitionTemplateExpression = null,
                       infraMappingTemplateExpression = null;
    List<TemplateExpression> templateExpressions = this.getTemplateExpressions();

    if (templateExpressions != null) {
      for (TemplateExpression templateExpression : templateExpressions) {
        String fieldName = templateExpression.getFieldName();
        if (fieldName != null && fieldName.equals(SERVICE_ID)) {
          serviceTemplateExpression = templateExpression;
        } else if (fieldName != null && fieldName.equals(INFRA_MAPPING_ID)) {
          infraMappingTemplateExpression = templateExpression;
        } else if (fieldName != null && fieldName.equals(INFRA_DEFINITION_ID)) {
          infraDefinitionTemplateExpression = templateExpression;
        }
      }
    }

    Service service =
        phaseSubWorkflowHelperService.getService(serviceId, serviceTemplateExpression, app.getAppId(), context);

    InfrastructureDefinition infrastructureDefinition = phaseSubWorkflowHelperService.getInfraDefinition(
        infraDefinitionId, infraDefinitionTemplateExpression, app.getAppId(), context);

    Environment env = ((ExecutionContextImpl) context).getEnv();

    InfrastructureMapping infrastructureMapping = phaseSubWorkflowHelperService.getInfraMapping(
        infraMappingId, infraMappingTemplateExpression, app, context, service, infrastructureDefinition);

    phaseSubWorkflowHelperService.validateEntitiesRelationship(service, infrastructureDefinition, infrastructureMapping,
        env, serviceTemplateExpression, infraMappingTemplateExpression);

    boolean infraRefactor = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, context.getAccountId());

    if (infraRefactor) {
      if (infrastructureMapping != null) {
        workflowExecutionService.appendInfraMappingId(
            app.getAppId(), context.getWorkflowExecutionId(), infrastructureMapping.getUuid());
      }
      if (workflowStandardParams.getWorkflowElement() != null) {
        workflowExecutionService.updateWorkflowElementWithLastGoodReleaseInfo(
            context.getAppId(), workflowStandardParams.getWorkflowElement(), context.getWorkflowExecutionId());
      }
    }

    ExecutionResponseBuilder executionResponseBuilder = getSpawningExecutionResponse(
        context, workflowStandardParams, service, infrastructureMapping, infrastructureDefinition);

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

    executionResponseBuilder.stateExecutionData(phaseExecutionData);
    return executionResponseBuilder.build();
  }

  private ExecutionResponseBuilder getSpawningExecutionResponse(ExecutionContext context,
      WorkflowStandardParams workflowStandardParams, Service service, InfrastructureMapping infrastructureMapping,
      InfrastructureDefinition infrastructureDefinition) {
    ExecutionContextImpl contextImpl = (ExecutionContextImpl) context;
    StateExecutionInstance stateExecutionInstance = contextImpl.getStateExecutionInstance();
    List<String> correlationIds = new ArrayList<>();

    StateExecutionInstance childStateExecutionInstance = getSpawningInstance(context, workflowStandardParams,
        stateExecutionInstance, service, infrastructureMapping, infrastructureDefinition);
    correlationIds.add(stateExecutionInstance.getUuid());

    return ExecutionResponse.builder()
        .stateExecutionInstance(childStateExecutionInstance)
        .async(true)
        .correlationIds(correlationIds);
  }

  private StateExecutionInstance getSpawningInstance(ExecutionContext context,
      WorkflowStandardParams workflowStandardParams, StateExecutionInstance stateExecutionInstance, Service service,
      InfrastructureMapping infrastructureMapping, InfrastructureDefinition infrastructureDefinition) {
    StateExecutionInstance spawningInstance = super.getSpawningInstance(stateExecutionInstance);

    PhaseElementBuilder phaseElementBuilder = PhaseElement.builder()
                                                  .uuid(getId())
                                                  .phaseName(stateExecutionInstance.getDisplayName())
                                                  .appId(stateExecutionInstance.getAppId())
                                                  .workflowExecutionId(context.getWorkflowExecutionId())
                                                  .phaseNameForRollback(phaseNameForRollback);

    if (service != null) {
      ServiceElement serviceElement = ServiceElement.builder().build();
      MapperUtils.mapObject(service, serviceElement);
      phaseElementBuilder.serviceElement(serviceElement);
    }

    String accountId = context.getAccountId();

    boolean infraRefactor = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, context.getAccountId());

    // Not null check is for build Workflow
    if (infraRefactor && infrastructureDefinition != null) {
      phaseElementBuilder.deploymentType(infrastructureDefinition.getDeploymentType().name());
      phaseElementBuilder.infraDefinitionId(infrastructureDefinition.getUuid());
    } else if (infrastructureMapping != null) {
      DeploymentType deploymentType =
          serviceResourceService.getDeploymentType(infrastructureMapping, null, infrastructureMapping.getServiceId());
      phaseElementBuilder.deploymentType(deploymentType.name()).infraMappingId(infrastructureMapping.getUuid());
    }

    if (stateExecutionInstance.getRollbackPhaseName() != null) {
      phaseElementBuilder.phaseNameForRollback(stateExecutionInstance.getRollbackPhaseName());
    }

    if (isNotEmpty(getVariableOverrides())) {
      phaseElementBuilder.variableOverrides(getVariableOverrides());
    }

    PhaseElement phaseElement = phaseElementBuilder.build();

    spawningInstance.getContextElements().push(phaseElement);
    spawningInstance.setContextElement(phaseElement);

    if (infrastructureMapping != null) {
      infrastructureMappingService.saveInfrastructureMappingToSweepingOutput(stateExecutionInstance.getAppId(),
          context.getWorkflowExecutionId(), phaseElement, infrastructureMapping.getUuid());
    }

    if (!isRollback() && featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
      WorkflowExecution workflowExecution =
          workflowExecutionService.getWorkflowExecution(workflowStandardParams.getAppId(),
              context.getWorkflowExecutionId()); // TODO: performance issue - filter query to get only execution args
                                                 // and artifacts
      saveArtifactsFromVariables(
          context, workflowStandardParams, stateExecutionInstance, service, phaseElement, workflowExecution);
    }

    if (isRollback() && workflowStandardParams.getWorkflowElement() != null) {
      // if last successful deployment found, save it in sweeping output
      if (workflowStandardParams.getWorkflowElement().getLastGoodDeploymentUuid() != null) {
        WorkflowExecution workflowExecution =
            workflowExecutionService.getWorkflowExecution(workflowStandardParams.getAppId(),
                workflowStandardParams.getWorkflowElement()
                    .getLastGoodDeploymentUuid()); // TODO: performance issue -filter query to get only execution args
                                                   // and artifacts

        if (workflowExecution == null) {
          logger.error("ERROR: Last Good Deployment ID is not found - lastGoodDeploymentUuid: {}",
              workflowStandardParams.getWorkflowElement().getLastGoodDeploymentUuid());
          throw new InvalidRequestException("Last Good Deployment ID is not found");
        }
        if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
          saveArtifactsFromVariablesForRollback(
              context, workflowStandardParams, stateExecutionInstance, service, phaseElement, workflowExecution);
        } else {
          phaseElement.setRollbackArtifactId(findRollbackArtifactId(service, workflowExecution));
        }
      } else {
        // save current artifacts in sweeping output
        if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
          WorkflowExecution workflowExecution = workflowExecutionService.getWorkflowExecution(
              workflowStandardParams.getAppId(), context.getWorkflowExecutionId());
          saveArtifactsFromVariables(
              context, workflowStandardParams, stateExecutionInstance, service, phaseElement, workflowExecution);
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

  private void saveArtifactsToSweepingOutput(
      String appId, String phaseExecutionId, Map<String, Artifact> artifactsMap) {
    if (isNotEmpty(artifactsMap)) {
      sweepingOutputService.save(
          SweepingOutputServiceImpl
              .prepareSweepingOutputBuilder(appId, null, null, phaseExecutionId, null, SweepingOutput.Scope.PHASE)
              .name("artifacts")
              .output(KryoUtils.asDeflatedBytes(artifactsMap))
              .build());
    }
  }

  private String findRollbackArtifactId(Service service, WorkflowExecution workflowExecution) {
    String rollbackArtifactId = null;
    if (workflowExecution.getExecutionArgs() != null && workflowExecution.getExecutionArgs().getArtifacts() != null) {
      if (service != null) {
        for (Artifact artifact : workflowExecution.getExecutionArgs().getArtifacts()) {
          List<String> artifactStreamIds = artifactStreamServiceBindingService.listArtifactStreamIds(service);
          if (isNotEmpty(artifactStreamIds) && artifactStreamIds.contains(artifact.getArtifactStreamId())) {
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
    Map<String, Artifact> artifactsMap = new HashMap<>();
    if (isNotEmpty(artifactVariables)) {
      for (ArtifactVariable artifactVariable : artifactVariables) {
        Artifact artifact = null;
        switch (artifactVariable.getEntityType()) {
          case WORKFLOW:
            if (isEmpty(artifactVariable.getOverriddenArtifactVariables())) {
              artifact = getArtifactByUuid(workflowExecution.getArtifacts(), artifactVariable.getValue());
              artifactsMap.put(artifactVariable.getName(), artifact);
            } else {
              for (ArtifactVariable overridingVariable : artifactVariable.getOverriddenArtifactVariables()) {
                if (EntityType.SERVICE.equals(
                        overridingVariable.getEntityType())) { // defined in service and overridden in workflow
                  if (service == null) {
                    throw new InvalidRequestException("Service cannot be empty", USER);
                  }
                  if (artifactVariable.getEntityId().equals(service.getUuid())) {
                    artifact = getArtifactByUuid(workflowExecution.getArtifacts(), artifactVariable.getValue());
                    artifactsMap.put(artifactVariable.getName(), artifact);
                  }
                } else if (EntityType.ENVIRONMENT.equals(overridingVariable.getEntityType())) {
                  if (isEmpty(overridingVariable
                                  .getOverriddenArtifactVariables())) { // direct env variables - all service overrides
                    artifact = getArtifactByUuid(workflowExecution.getArtifacts(), artifactVariable.getValue());
                    artifactsMap.put(artifactVariable.getName(), artifact);
                  } else { // overridden for specific service
                    for (ArtifactVariable variable : overridingVariable.getOverriddenArtifactVariables()) {
                      if (EntityType.SERVICE.equals(variable.getEntityType())) {
                        if (service == null) {
                          throw new InvalidRequestException("Service cannot be empty", USER);
                        }
                        if (artifactVariable.getEntityId().equals(service.getUuid())) {
                          artifact = getArtifactByUuid(workflowExecution.getArtifacts(), artifactVariable.getValue());
                          artifactsMap.put(artifactVariable.getName(), artifact);
                        }
                      }
                    }
                  }
                }
              }
            }
            break;
          case ENVIRONMENT:
            if (isEmpty(artifactVariable
                            .getOverriddenArtifactVariables())) { // direct env variables - all service overrides
              artifact = getArtifactByUuid(workflowExecution.getArtifacts(), artifactVariable.getValue());
              artifactsMap.put(artifactVariable.getName(), artifact);
            } else { // overridden for specific service
              for (ArtifactVariable variable : artifactVariable.getOverriddenArtifactVariables()) {
                if (EntityType.SERVICE.equals(variable.getEntityType())) {
                  if (service == null) {
                    throw new InvalidRequestException("Service cannot be empty", USER);
                  }
                  if (artifactVariable.getEntityId().equals(service.getUuid())) {
                    artifact = getArtifactByUuid(workflowExecution.getArtifacts(), artifactVariable.getValue());
                    artifactsMap.put(artifactVariable.getName(), artifact);
                  }
                }
              }
            }
            break;
          case SERVICE:
            if (service == null) {
              throw new InvalidRequestException("Service cannot be empty", USER);
            }
            if (artifactVariable.getEntityId().equals(service.getUuid())) {
              artifact = getArtifactByUuid(workflowExecution.getArtifacts(), artifactVariable.getValue());
              artifactsMap.put(artifactVariable.getName(), artifact);
            }
            break;
          default:
            throw new InvalidRequestException(
                format("Artifact variable [%s] has invalid Entity Type", artifactVariable.getName()), USER);
        }
        if (artifactVariable.getName().equals(ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME)) {
          saveArtifactToSweepingOutput(stateExecutionInstance.getAppId(), phaseExecutionId, artifactVariable, artifact);
        }
      }
      saveArtifactsToSweepingOutput(stateExecutionInstance.getAppId(), phaseExecutionId, artifactsMap);
    }
  }

  private Artifact getArtifactByUuid(List<Artifact> artifacts, String artifactId) {
    if (artifactId == null) {
      return null;
    }
    if (isNotEmpty(artifacts)) {
      for (Artifact artifact : artifacts) {
        if (artifact.getUuid().equals(artifactId)) {
          // This will fetch artifactFiles as well
          return artifactService.get(artifactId);
        }
      }
    }
    return null;
  }

  private void saveArtifactsFromVariablesForRollback(ExecutionContext context,
      WorkflowStandardParams workflowStandardParams, StateExecutionInstance stateExecutionInstance, Service service,
      PhaseElement phaseElement, WorkflowExecution lastSuccessfulWorkflowExecution) {
    String phaseExecutionId = context.getWorkflowExecutionId() + phaseElement.getUuid() + phaseElement.getPhaseName();
    // go over all artifact variables of service
    // throw exception if variable does not exist in service
    List<ArtifactVariable> artifactVariables = workflowStandardParams.getWorkflowElement().getArtifactVariables();
    List<ArtifactVariable> previousArtifactVariables = null;
    if (lastSuccessfulWorkflowExecution.getExecutionArgs() != null
        && lastSuccessfulWorkflowExecution.getExecutionArgs().getArtifacts() != null) {
      previousArtifactVariables = lastSuccessfulWorkflowExecution.getExecutionArgs().getArtifactVariables();
    }
    previousArtifactVariables = isEmpty(previousArtifactVariables) ? new ArrayList<>() : previousArtifactVariables;
    Map<String, Artifact> artifactsMap = new HashMap<>();
    if (isNotEmpty(artifactVariables)) {
      collectRollbackArtifactsFromVariables(stateExecutionInstance, service, phaseExecutionId, artifactVariables,
          previousArtifactVariables, artifactsMap, lastSuccessfulWorkflowExecution.getArtifacts());
    }
  }

  private void collectRollbackArtifactsFromVariables(StateExecutionInstance stateExecutionInstance, Service service,
      String phaseExecutionId, List<ArtifactVariable> artifactVariables,
      List<ArtifactVariable> previousArtifactVariables, Map<String, Artifact> artifactsMap,
      List<Artifact> previousArtifacts) {
    for (ArtifactVariable artifactVariable : artifactVariables) {
      Artifact artifact = null;
      switch (artifactVariable.getEntityType()) {
        case WORKFLOW:
          if (isEmpty(artifactVariable.getOverriddenArtifactVariables())) {
            artifact = fetchLastSuccessArtifact(previousArtifactVariables, previousArtifacts, artifactVariable);
          } else {
            for (ArtifactVariable overridingVariable : artifactVariable.getOverriddenArtifactVariables()) {
              if (EntityType.SERVICE.equals(
                      overridingVariable.getEntityType())) { // defined in service and overridden in workflow
                if (service == null) {
                  throw new InvalidRequestException("Service cannot be empty", USER);
                }
                if (artifactVariable.getEntityId().equals(service.getUuid())) {
                  artifact = fetchLastSuccessArtifact(previousArtifactVariables, previousArtifacts, artifactVariable);
                }
              } else if (EntityType.ENVIRONMENT.equals(overridingVariable.getEntityType())) {
                if (isEmpty(overridingVariable
                                .getOverriddenArtifactVariables())) { // direct env variables - all service overrides
                  artifact = fetchLastSuccessArtifact(previousArtifactVariables, previousArtifacts, artifactVariable);
                } else { // overridden for specific service
                  for (ArtifactVariable variable : overridingVariable.getOverriddenArtifactVariables()) {
                    if (EntityType.SERVICE.equals(variable.getEntityType())) {
                      if (service == null) {
                        throw new InvalidRequestException("Service cannot be empty", USER);
                      }
                      if (artifactVariable.getEntityId().equals(service.getUuid())) {
                        artifact =
                            fetchLastSuccessArtifact(previousArtifactVariables, previousArtifacts, artifactVariable);
                      }
                    }
                  }
                }
              }
            }
          }
          break;
        case ENVIRONMENT:
          if (isEmpty(
                  artifactVariable.getOverriddenArtifactVariables())) { // direct env variables - all service overrides
            artifact = fetchLastSuccessArtifact(previousArtifactVariables, previousArtifacts, artifactVariable);

          } else { // overridden for specific service
            for (ArtifactVariable variable : artifactVariable.getOverriddenArtifactVariables()) {
              if (EntityType.SERVICE.equals(variable.getEntityType())) {
                if (service == null) {
                  throw new InvalidRequestException("Service cannot be empty", USER);
                }
                if (artifactVariable.getEntityId().equals(service.getUuid())) {
                  artifact = fetchLastSuccessArtifact(previousArtifactVariables, previousArtifacts, artifactVariable);
                }
              }
            }
          }
          break;
        case SERVICE:
          if (service == null) {
            throw new InvalidRequestException("Service cannot be empty", USER);
          }
          if (artifactVariable.getEntityId().equals(service.getUuid())) {
            artifact = fetchLastSuccessArtifact(previousArtifactVariables, previousArtifacts, artifactVariable);
            // todo: save workflow level artifacts overridden by this service at phase level to access artifact like
            // artifact1.buildNo
          } else {
            throw new WingsException(format(
                "Artifact Variable %s not defined in service %s", artifactVariable.getName(), service.getName()));
          }
          break;
        default:
          throw new InvalidRequestException(
              format("Artifact variable [%s] has invalid Entity Type", artifactVariable.getName()), USER);
      }
      artifactsMap.put(artifactVariable.getName(), artifact);
      if (artifactVariable.getName().equals(ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME)) {
        saveArtifactToSweepingOutput(stateExecutionInstance.getAppId(), phaseExecutionId, artifactVariable, artifact);
      }
    }
    saveArtifactsToSweepingOutput(stateExecutionInstance.getAppId(), phaseExecutionId, artifactsMap);
  }

  @Nullable
  private Artifact fetchLastSuccessArtifact(List<ArtifactVariable> previousArtifactVariables,
      List<Artifact> previousArtifacts, ArtifactVariable artifactVariable) {
    return getArtifactByUuid(previousArtifacts,
        getArtifactIdFromPreviousArtifactVariables(artifactVariable, previousArtifactVariables, previousArtifacts));
  }

  private String getArtifactIdFromPreviousArtifactVariables(ArtifactVariable artifactVariable,
      List<ArtifactVariable> lastSuccessArtifactVariables, List<Artifact> lastSuccessArtifacts) {
    String artifactId = null;
    if (isNotEmpty(lastSuccessArtifactVariables)) {
      for (ArtifactVariable variable : lastSuccessArtifactVariables) {
        if (variable.getName().equals(artifactVariable.getName())
            && variable.getEntityId().equals(artifactVariable.getEntityId())) {
          return variable.getValue();
        }
      }
    } else if (isNotEmpty(lastSuccessArtifacts)) {
      artifactId = getArtifactIdFromPreviousArtifacts(artifactVariable, lastSuccessArtifacts);
    }

    return artifactId != null ? artifactId : artifactVariable.getValue();
  }

  private String getArtifactIdFromPreviousArtifacts(
      ArtifactVariable artifactVariable, List<Artifact> lastSuccessArtifacts) {
    if (isEmpty(lastSuccessArtifacts)) {
      return null;
    }
    for (Artifact artifact : lastSuccessArtifacts) {
      if (artifact.getServiceIds().contains(artifactVariable.getEntityId())) {
        return artifact.getUuid();
      }
    }
    return null;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ExecutionResponseBuilder executionResponseBuilder = ExecutionResponse.builder();
    super.handleStatusSummary(workflowExecutionService, context, response, executionResponseBuilder);
    response.values().forEach(notifyResponseData -> {
      if (notifyResponseData instanceof ElementNotifyResponseData) {
        List<ContextElement> notifyElements = ((ElementNotifyResponseData) notifyResponseData).getContextElements();
        if (isNotEmpty(notifyElements)) {
          for (ContextElement element : notifyElements) {
            executionResponseBuilder.contextElement(element);
          }
        }
      }
    });
    PhaseExecutionData phaseExecutionData = (PhaseExecutionData) context.getStateExecutionData();
    phaseExecutionData.setPhaseExecutionSummary(workflowExecutionService.getPhaseExecutionSummary(
        context.getAppId(), context.getWorkflowExecutionId(), context.getStateExecutionInstanceId()));
    executionResponseBuilder.stateExecutionData(phaseExecutionData);
    return executionResponseBuilder.build();
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

  @SchemaIgnore
  public String getInfraDefinitionId() {
    return infraMappingId;
  }

  public void setInfraDefinitionId(String infraDefinitionId) {
    this.infraDefinitionId = infraDefinitionId;
  }
}
