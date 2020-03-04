package software.wings.service.impl.workflow;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.OrchestrationWorkflowType.BASIC;
import static io.harness.beans.OrchestrationWorkflowType.BLUE_GREEN;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.beans.OrchestrationWorkflowType.ROLLING;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.WORKFLOW_EXECUTION_IN_PROGRESS;
import static io.harness.exception.HintException.MOVE_TO_THE_PARENT_OBJECT;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.expression.ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.govern.Switch.noop;
import static io.harness.govern.Switch.unhandled;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.validation.Validator.notEmptyCheck;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.atteo.evo.inflector.English.plural;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.api.DeploymentType.HELM;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.CanaryWorkflowExecutionAdvisor.ROLLBACK_PROVISIONERS;
import static software.wings.beans.EntityType.ARTIFACT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.beans.FeatureName.INFRA_MAPPING_REFACTOR;
import static software.wings.beans.FeatureName.TEMPLATED_PIPELINES;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.common.Constants.WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE;
import static software.wings.common.InfrastructureConstants.INFRA_ID_EXPRESSION;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_DEPLOY;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_SETUP;
import static software.wings.sm.StateType.AWS_CODEDEPLOY_STATE;
import static software.wings.sm.StateType.AWS_LAMBDA_STATE;
import static software.wings.sm.StateType.CLOUD_FORMATION_CREATE_STACK;
import static software.wings.sm.StateType.COMMAND;
import static software.wings.sm.StateType.ECS_DAEMON_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;
import static software.wings.sm.StateType.HTTP;
import static software.wings.sm.StateType.K8S_APPLY;
import static software.wings.sm.StateType.K8S_BLUE_GREEN_DEPLOY;
import static software.wings.sm.StateType.K8S_CANARY_DEPLOY;
import static software.wings.sm.StateType.K8S_DEPLOYMENT_ROLLING;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY;
import static software.wings.sm.StateType.KUBERNETES_SETUP;
import static software.wings.sm.StateType.PCF_RESIZE;
import static software.wings.sm.StateType.PCF_SETUP;
import static software.wings.sm.StateType.SHELL_SCRIPT;
import static software.wings.sm.StateType.TERRAFORM_ROLLBACK;
import static software.wings.sm.StateType.values;
import static software.wings.sm.states.provision.TerraformProvisionState.INHERIT_APPROVED_PLAN;
import static software.wings.sm.states.provision.TerraformProvisionState.RUN_PLAN_ONLY_KEY;
import static software.wings.stencils.WorkflowStepType.SERVICE_COMMAND;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.data.parser.CsvParser;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.ExplanationException;
import io.harness.exception.FailureType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.LimitEnforcementUtils;
import io.harness.limits.checker.StaticLimitCheckerWithDecrement;
import io.harness.limits.counter.service.CounterSyncer;
import io.harness.observer.Rejection;
import io.harness.persistence.HIterator;
import io.harness.queue.QueuePublisher;
import io.harness.validation.Create;
import io.harness.validation.Update;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElement;
import software.wings.app.StaticConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.Base;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureStrategy;
import software.wings.beans.FeatureName;
import software.wings.beans.Graph;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Pipeline;
import software.wings.beans.RepairActionCode;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceTemplate.ServiceTemplateKeys;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowCategorySteps;
import software.wings.beans.WorkflowCategoryStepsMeta;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowStepMeta;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamSummary;
import software.wings.beans.artifact.ArtifactStreamSummary.ArtifactStreamSummaryBuilder;
import software.wings.beans.artifact.ArtifactSummary;
import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.concurrency.ConcurrencyStrategy;
import software.wings.beans.concurrency.ConcurrencyStrategy.UnitType;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.deployment.DeploymentMetadata.DeploymentMetadataBuilder;
import software.wings.beans.deployment.DeploymentMetadata.Include;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.peronalization.Personalization;
import software.wings.beans.peronalization.Personalization.PersonalizationKeys;
import software.wings.beans.security.UserGroup;
import software.wings.beans.stats.CloneMetadata;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.TriggerKeys;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.infra.InfrastructureDefinition;
import software.wings.prune.PruneEntityListener;
import software.wings.prune.PruneEvent;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.ServiceClassLocator;
import software.wings.service.impl.workflow.creation.WorkflowCreator;
import software.wings.service.impl.workflow.creation.WorkflowCreatorFactory;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.ownership.OwnedByWorkflow;
import software.wings.service.intfc.personalization.PersonalizationService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.trigger.DeploymentTriggerService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachine.StateMachineKeys;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.sm.StepType;
import software.wings.sm.states.k8s.K8sStateHelper;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilCategory;
import software.wings.stencils.StencilPostProcessor;
import software.wings.stencils.WorkflowStepType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class WorkflowServiceImpl.
 *
 * @author Rishi
 */
@Singleton
@ValidateOnExecution
@Slf4j
public class WorkflowServiceImpl implements WorkflowService, DataProvider {
  private static final String VERIFY = "Verify";
  private static final String ROLLBACK_PROVISION_INFRASTRUCTURE = "Rollback Provision Infrastructure";

  private static final List<String> kubernetesArtifactNeededStateTypes =
      Arrays.asList(KUBERNETES_SETUP.name(), KUBERNETES_DEPLOY.name());

  private static final List<String> k8sV2ArtifactNeededStateTypes = Arrays.asList(
      K8S_DEPLOYMENT_ROLLING.name(), K8S_CANARY_DEPLOY.name(), K8S_BLUE_GREEN_DEPLOY.name(), K8S_APPLY.name());

  private static final List<String> ecsArtifactNeededStateTypes =
      Arrays.asList(ECS_SERVICE_DEPLOY.name(), ECS_SERVICE_SETUP.name(), ECS_DAEMON_SERVICE_SETUP.name());

  private static final List<String> amiArtifactNeededStateTypes = Arrays.asList(AWS_AMI_SERVICE_SETUP.name(),
      AWS_AMI_SERVICE_DEPLOY.name(), StateType.SPOTINST_SETUP.name(), StateType.SPOTINST_DEPLOY.name());

  private static final List<String> codeDeployArtifactNeededStateTypes = Arrays.asList(AWS_CODEDEPLOY_STATE.name());

  private static final List<String> awsLambdaArtifactNeededStateTypes = Arrays.asList(AWS_LAMBDA_STATE.name());

  private static final List<String> pcfArtifactNeededStateTypes = Arrays.asList(PCF_SETUP.name(), PCF_RESIZE.name());

  private static final Comparator<Stencil> stencilDefaultSorter =
      Comparator.comparingInt((Stencil o) -> o.getStencilCategory().getDisplayOrder())
          .thenComparingInt(Stencil::getDisplayOrder)
          .thenComparing(Stencil::getType);
  private static final String WORKFLOW_WAS_DELETED = "Workflow was deleted";
  public static final String ORCHESTRATION_WORKFLOW = "OrchestrationWorkflow";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private StencilPostProcessor stencilPostProcessor;
  @Inject private StaticConfiguration staticConfiguration;
  @Inject private UserGroupService userGroupService;
  @Inject private CounterSyncer counterSyncer;
  @Inject private AccountService accountService;
  @Inject private AppService appService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private EntityVersionService entityVersionService;
  @Inject private EnvironmentService environmentService;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private ExecutorService executorService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private HostService hostService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private K8sStateHelper k8sStateHelper;
  @Inject private LimitCheckerFactory limitCheckerFactory;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private PersonalizationService personalizationService;
  @Inject private PipelineService pipelineService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private SettingsService settingsService;
  @Inject private TriggerService triggerService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WorkflowServiceHelper workflowServiceHelper;
  @Inject private WorkflowServiceTemplateHelper workflowServiceTemplateHelper;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private YamlPushService yamlPushService;
  @Inject private TemplateService templateService;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ArtifactService artifactService;

  @Inject private QueuePublisher<PruneEvent> pruneQueue;
  @Inject private HarnessTagService harnessTagService;
  @Inject private ResourceLookupService resourceLookupService;
  @Inject private DeploymentTriggerService deploymentTriggerService;

  private Map<StateTypeScope, List<StateTypeDescriptor>> cachedStencils;
  private Map<String, StateTypeDescriptor> cachedStencilMap;

  @Inject private WorkflowCreatorFactory workflowCreatorFactory;

  /**
   * {@inheritDoc}
   */
  @Override
  public StateMachine createStateMachine(StateMachine stateMachine) {
    stateMachine.validate();
    wingsPersistence.save(stateMachine);
    return stateMachine;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<StateMachine> listStateMachines(PageRequest<StateMachine> req) {
    return wingsPersistence.query(StateMachine.class, req);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<StateTypeScope, List<Stencil>> stencils(
      String appId, String workflowId, String phaseId, StateTypeScope... stateTypeScopes) {
    final Map<StateTypeScope, List<Stencil>> stencils = getStencils(appId, workflowId, phaseId, stateTypeScopes);
    removeStencil(stencils, appId, FeatureName.INFRA_MAPPING_REFACTOR, StateTypeScope.ORCHESTRATION_STENCILS,
        StateType.PCF_PLUGIN);
    return stencils;
  }

  private void removeStencil(Map<StateTypeScope, List<Stencil>> stencils, String appId, FeatureName featureName,
      StateTypeScope stateTypeScope, StateType stateType) {
    if (!featureFlagService.isEnabled(featureName, appService.getAccountIdByAppId(appId))) {
      List<Stencil> stencilList = stencils.get(stateTypeScope);
      if (isNotEmpty(stencilList)) {
        for (Iterator<Stencil> it = stencilList.iterator(); it.hasNext();) {
          Stencil stencil = it.next();
          if (stencil.getType().equals(stateType.toString())) {
            it.remove();
          }
        }
      }
    }
  }

  private Map<StateTypeScope, List<Stencil>> getStencils(
      String appId, String workflowId, String phaseId, StateTypeScope[] stateTypeScopes) {
    Map<StateTypeScope, List<StateTypeDescriptor>> stencilsMap = loadStateTypes();
    return getStateTypeScopeListMap(appId, workflowId, phaseId, stateTypeScopes, stencilsMap);
  }

  private Map<StateTypeScope, List<Stencil>> getStateTypeScopeListMap(String appId, String workflowId, String phaseId,
      StateTypeScope[] stateTypeScopes, Map<StateTypeScope, List<StateTypeDescriptor>> stencilsMap) {
    boolean infraRefactor = featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(appId));
    boolean filterForWorkflow = isNotBlank(workflowId);
    boolean filterForPhase = filterForWorkflow && isNotBlank(phaseId);
    Workflow workflow = null;
    Map<StateTypeScope, List<Stencil>> mapByScope = null;
    WorkflowPhase workflowPhase = null;
    Map<String, String> entityMap = new HashMap<>(1);
    boolean buildWorkflow = false;
    if (filterForWorkflow) {
      workflow = readWorkflow(appId, workflowId);
      if (workflow == null) {
        throw new InvalidRequestException(format("Workflow %s does not exist", workflowId), USER);
      }
      OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
      if (orchestrationWorkflow != null) {
        buildWorkflow = BUILD == orchestrationWorkflow.getOrchestrationWorkflowType();
      }
      String envId = workflow.getEnvId();
      entityMap.put(EntityType.ENVIRONMENT.name(), envId);
      if (filterForPhase) {
        if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
          workflowPhase = ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhaseIdMap().get(phaseId);
        }
        if (workflowPhase == null) {
          throw new InvalidRequestException(
              "Workflow Phase not associated with Workflow [" + workflow.getName() + "]", USER);
        }
        String serviceId = workflowPhase.getServiceId();
        if (serviceId != null) {
          entityMap.put(EntityType.SERVICE.name(), serviceId);
          mapByScope = getStateTypeForApp(appId, stencilsMap, entityMap);
        } else {
          mapByScope = getStateTypeForApp(appId, stencilsMap, entityMap);
        }
      } else {
        entityMap.put("NONE", "NONE");
        // For workflow, anyways skipping the command names. So, sending service Id as "NONE" to make sure that
        // EnumDataProvider can ignore that.
        mapByScope = getStateTypeForApp(appId, stencilsMap, entityMap);
      }

    } else {
      mapByScope = getStateTypeForApp(appId, stencilsMap, entityMap);
    }
    Map<StateTypeScope, List<Stencil>> maps = new HashMap<>();
    if (isEmpty(stateTypeScopes)) {
      maps.putAll(mapByScope);
    } else {
      for (StateTypeScope scope : stateTypeScopes) {
        maps.put(scope, mapByScope.get(scope));
      }
    }
    maps.values().forEach(list -> list.sort(stencilDefaultSorter));

    Predicate<Stencil> predicate = stencil -> true;
    if (filterForWorkflow) {
      if (filterForPhase) {
        if (workflowPhase != null) {
          if (infraRefactor) {
            String infraDefinitionId = workflowPhase.getInfraDefinitionId();
            if (workflowPhase.checkInfraDefinitionTemplatized()) {
              DeploymentType workflowPhaseDeploymentType = workflowPhase.getDeploymentType();
              if (workflowPhaseDeploymentType != null) {
                predicate = stencil -> stencil.matches(workflowPhaseDeploymentType);
              }
            } else if (infraDefinitionId != null) {
              // TODO: This check can be removed once refactor the code to have single class for AWS_NODE_SELECT,
              // DC_NODE_SELECT and ROLLING_SELECT_NODE
              InfrastructureDefinition infrastructureDefinition =
                  infrastructureDefinitionService.get(appId, infraDefinitionId);
              if (infrastructureDefinition != null) {
                predicate = stencil -> stencil.matches(infrastructureDefinition);
              }
            }
          } else {
            String infraMappingId = workflowPhase.getInfraMappingId();
            if (workflowPhase.checkInfraTemplatized()) {
              DeploymentType workflowPhaseDeploymentType = workflowPhase.getDeploymentType();
              if (workflowPhaseDeploymentType != null) {
                predicate = stencil -> stencil.matches(workflowPhaseDeploymentType);
              }
            } else if (infraMappingId != null) {
              // TODO: This check can be removed once refactor the code to have single class for AWS_NODE_SELECT,
              // DC_NODE_SELECT and ROLLING_SELECT_NODE
              InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
              if (infrastructureMapping != null) {
                predicate = stencil -> stencil.matches(infrastructureMapping);
              }
            }
          }
        }
      } else {
        predicate = stencil
            -> stencil.getStencilCategory() != StencilCategory.COMMANDS
            && stencil.getStencilCategory() != StencilCategory.CLOUD;
      }
    }
    Predicate<Stencil> finalPredicate = predicate;
    maps = maps.entrySet().stream().collect(toMap(Entry::getKey,
        stateTypeScopeListEntry
        -> stateTypeScopeListEntry.getValue().stream().filter(finalPredicate).collect(toList())));
    if (!buildWorkflow) {
      maps = filterArtifactCollectionState(maps);
    }
    return maps;
  }

  private Map<StateTypeScope, List<Stencil>> filterArtifactCollectionState(Map<StateTypeScope, List<Stencil>> maps) {
    Predicate<Stencil> buildWorkflowPredicate = stencil -> stencil.getStencilCategory() != StencilCategory.COLLECTIONS;
    return maps.entrySet().stream().collect(toMap(Entry::getKey,
        stateTypeScopeListEntry
        -> stateTypeScopeListEntry.getValue().stream().filter(buildWorkflowPredicate).collect(toList())));
  }

  private Map<StateTypeScope, List<Stencil>> getStateTypeForApp(
      String appId, Map<StateTypeScope, List<StateTypeDescriptor>> stencilsMap, Map<String, String> entityMap) {
    return stencilsMap.entrySet().stream().collect(toMap(Entry::getKey,
        stateTypeScopeListEntry
        -> stencilPostProcessor.postProcess(stateTypeScopeListEntry.getValue(), appId, entityMap)));
  }

  private Map<StateTypeScope, List<StateTypeDescriptor>> loadStateTypes() {
    if (cachedStencils != null) {
      return cachedStencils;
    }

    List<StateTypeDescriptor> stencils = new ArrayList<>(Arrays.asList(values()));

    Map<String, StateTypeDescriptor> mapByType = new HashMap<>();
    Map<StateTypeScope, List<StateTypeDescriptor>> mapByScope = new HashMap<>();
    for (StateTypeDescriptor sd : stencils) {
      if (mapByType.get(sd.getType()) != null) {
        throw new InvalidRequestException("Duplicate implementation for the stencil: " + sd.getType(), USER);
      }
      mapByType.put(sd.getType(), sd);
      sd.getScopes().forEach(scope -> mapByScope.computeIfAbsent(scope, k -> new ArrayList<>()).add(sd));
    }

    this.cachedStencils = mapByScope;
    this.cachedStencilMap = mapByType;
    return mapByScope;
  }

  /**
   * {@inheritDoc}
   * @param appId
   */
  @Override
  public Map<String, StateTypeDescriptor> stencilMap(String appId) {
    if (cachedStencilMap == null) {
      stencils(appId, null, null);
    }
    return cachedStencilMap;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Workflow> listWorkflowsWithoutOrchestration(PageRequest<Workflow> pageRequest) {
    return wingsPersistence.query(Workflow.class, pageRequest);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Workflow> listWorkflows(PageRequest<Workflow> pageRequest) {
    PageResponse<Workflow> response = listWorkflows(pageRequest, 0, false, null);
    return response == null ? new PageResponse<>() : response;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Workflow> listWorkflows(
      PageRequest<Workflow> pageRequest, Integer previousExecutionsCount, boolean withTags, String tagFilter) {
    PageResponse<Workflow> workflows =
        resourceLookupService.listWithTagFilters(pageRequest, tagFilter, EntityType.WORKFLOW, withTags);

    if (workflows != null && workflows.getResponse() != null) {
      for (Workflow workflow : workflows.getResponse()) {
        try {
          loadOrchestrationWorkflow(workflow, workflow.getDefaultVersion());
        } catch (Exception e) {
          logger.error("Failed to load Orchestration workflow {}", workflow.getUuid(), e);
        }
      }
    }
    if (workflows != null && previousExecutionsCount != null && previousExecutionsCount > 0) {
      for (Workflow workflow : workflows) {
        try {
          PageRequest<WorkflowExecution> workflowExecutionPageRequest =
              aPageRequest()
                  .withLimit(previousExecutionsCount.toString())
                  .addFilter("workflowId", EQ, workflow.getUuid())
                  .addFilter("appId", EQ, workflow.getAppId())
                  .build();

          final List<WorkflowExecution> workflowExecutions =
              workflowExecutionService.listExecutions(workflowExecutionPageRequest, false, false, false, false)
                  .getResponse();

          workflowExecutions.forEach(we -> we.setStateMachine(null));
          workflow.setWorkflowExecutions(workflowExecutions);
        } catch (Exception e) {
          logger.error("Failed to fetch recent executions for workflow {}", workflow.getUuid(), e);
        }
      }
    }
    return workflows;
  }

  @Override
  public List<Workflow> listWorkflows(String artifactStreamId, String accountId) {
    List<Workflow> workflows = wingsPersistence.createQuery(Workflow.class)
                                   .filter(WorkflowKeys.linkedArtifactStreamIds, artifactStreamId)
                                   .filter(WorkflowKeys.accountId, accountId)
                                   .project(WorkflowKeys.name, true)
                                   .project(WorkflowKeys.uuid, true)
                                   .project(WorkflowKeys.appId, true)
                                   .asList();
    if (isEmpty(workflows)) {
      return new ArrayList<>();
    }
    List<Workflow> allWorkflows = new ArrayList<>();
    boolean infraRefactor = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountId);
    for (Workflow workflow : workflows) {
      allWorkflows.add(readWorkflowWithoutServices(workflow.getAppId(), workflow.getUuid(), infraRefactor));
    }
    return allWorkflows;
  }

  @Override
  public Workflow readWorkflow(String appId, String workflowId) {
    return readWorkflow(appId, workflowId, null);
  }

  @Override
  public boolean workflowExists(String appId, String workflowId) {
    return wingsPersistence.createQuery(Trigger.class)
               .filter(TriggerKeys.appId, appId)
               .filter(TriggerKeys.workflowId, workflowId)
               .getKey()
        != null;
  }

  @Override
  public Workflow readWorkflowWithoutServices(String appId, String workflowId) {
    Workflow workflow = wingsPersistence.getWithAppId(Workflow.class, appId, workflowId);
    if (workflow == null) {
      return null;
    }
    loadOrchestrationWorkflow(workflow, null, false);
    return workflow;
  }

  @Override
  public Workflow readWorkflowWithoutServices(String appId, String workflowId, boolean infraRefactor) {
    Workflow workflow = wingsPersistence.getWithAppId(Workflow.class, appId, workflowId);
    if (workflow == null) {
      return null;
    }
    loadOrchestrationWorkflow(workflow, null, false, infraRefactor);
    return workflow;
  }

  @Override
  public Workflow readWorkflowWithoutOrchestration(String appId, String workflowId) {
    return wingsPersistence.createQuery(Workflow.class)
        .project(WorkflowKeys.orchestration, false)
        .filter(WorkflowKeys.appId, appId)
        .filter(WorkflowKeys.uuid, workflowId)
        .get();
  }

  @Override
  public List<Workflow> listWorkflowsWithoutOrchestration(Collection<String> workflowIds) {
    return wingsPersistence.createQuery(Workflow.class, excludeAuthority)
        .project(WorkflowKeys.orchestration, false)
        .field(WorkflowKeys.uuid)
        .in(workflowIds)
        .asList();
  }

  @Override
  public Workflow readWorkflow(String appId, String workflowId, Integer version) {
    Workflow workflow = wingsPersistence.getWithAppId(Workflow.class, appId, workflowId);
    if (workflow == null) {
      return null;
    }
    loadOrchestrationWorkflow(workflow, version);
    return workflow;
  }

  @Override
  public Workflow readWorkflowByName(String appId, String workflowName) {
    Workflow workflow = wingsPersistence.createQuery(Workflow.class)
                            .filter("appId", appId)
                            .filter(WorkflowKeys.name, workflowName)
                            .get();
    if (workflow != null) {
      loadOrchestrationWorkflow(workflow, workflow.getDefaultVersion());
    }
    return workflow;
  }

  @Override
  public void loadOrchestrationWorkflow(Workflow workflow, Integer version) {
    loadOrchestrationWorkflow(workflow, version, true);
  }

  private void loadOrchestrationWorkflow(Workflow workflow, Integer version, boolean withServices) {
    boolean infraRefactor = featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, workflow.getAccountId());
    loadOrchestrationWorkflow(workflow, version, withServices, infraRefactor);
  }

  private void loadOrchestrationWorkflow(
      Workflow workflow, Integer version, boolean withServices, boolean infraRefactor) {
    StateMachine stateMachine = readStateMachine(
        workflow.getAppId(), workflow.getUuid(), version == null ? workflow.getDefaultVersion() : version);
    if (stateMachine != null) {
      // @TODO This check needs to be removed once on teplatizing, env is set to null.
      if (withServices) {
        if (workflow.checkEnvironmentTemplatized()) {
          if (workflow.getEnvId() != null) {
            boolean environmentExists = environmentService.exist(workflow.getAppId(), workflow.getEnvId());
            workflow.setEnvId(environmentExists ? workflow.getEnvId() : null);
          }
        }
      }
      boolean templatedPipeline = featureFlagService.isEnabled(TEMPLATED_PIPELINES, workflow.getAccountId());
      workflow.setOrchestrationWorkflow(stateMachine.getOrchestrationWorkflow());
      OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
      if (orchestrationWorkflow != null) {
        orchestrationWorkflow.onLoad(infraRefactor, templatedPipeline, workflow);
        workflow.setDeploymentTypes(workflowServiceHelper.obtainDeploymentTypes(orchestrationWorkflow));
        workflow.setTemplatized(orchestrationWorkflow.checkTemplatized());
        if (withServices) {
          populateServices(workflow);
        }
      }
    }
  }

  @Override
  @ValidationGroups(Create.class)
  public Workflow createWorkflow(Workflow workflow) {
    String accountId = appService.getAccountIdByAppId(workflow.getAppId());
    workflow.setAccountId(accountId);

    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new Action(accountId, ActionType.CREATE_WORKFLOW));
    return LimitEnforcementUtils.withLimitCheck(checker, () -> { return createWorkflowInternal(workflow); });
  }

  /**
   * {@inheritDoc}
   */
  private Workflow createWorkflowInternal(Workflow workflow) {
    if (workflow.getUuid() == null) {
      workflow.setUuid(generateUuid());
    }
    final boolean infraRefactor = featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, workflow.getAccountId());

    validateWorkflowNameForDuplicates(workflow);
    validateOrchestrationWorkflow(workflow);
    final OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    workflow.setDefaultVersion(1);
    List<String> linkedTemplateUuids = new ArrayList<>();
    List<String> linkedArtifactStreamIds = new ArrayList<>();

    if (orchestrationWorkflow != null) {
      boolean isV2ServicePresent = StringUtils.isNotEmpty(workflow.getServiceId())
          && workflowServiceHelper.isK8sV2Service(workflow.getAppId(), workflow.getServiceId());
      WorkflowCreator workflowCreator = workflowCreatorFactory.getWorkflowCreatorFactory(
          orchestrationWorkflow.getOrchestrationWorkflowType(), isV2ServicePresent);
      workflow = workflowCreator.createWorkflow(workflow);
      if (isEmpty(orchestrationWorkflow.getNotificationRules())) {
        createDefaultNotificationRule(workflow);
      }
      if (orchestrationWorkflow.getOrchestrationWorkflowType() != BUILD
          && orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
        if (isEmpty(canaryOrchestrationWorkflow.getFailureStrategies())) {
          createDefaultFailureStrategy(workflow);
        }

        if (canaryOrchestrationWorkflow.getConcurrencyStrategy() == null && infraRefactor) {
          canaryOrchestrationWorkflow.setConcurrencyStrategy(ConcurrencyStrategy.builder().build());
        }
      }

      // Ensure artifact check
      ensureArtifactCheck(workflow.getAppId(), orchestrationWorkflow, infraRefactor);

      // Add environment expressions
      WorkflowServiceTemplateHelper.transformEnvTemplateExpressions(workflow, orchestrationWorkflow);

      // Validate Workflow Variables
      validateWorkflowVariables(orchestrationWorkflow);

      orchestrationWorkflow.onSave();
      workflowServiceTemplateHelper.populatePropertiesFromWorkflow(workflow);

      workflowServiceTemplateHelper.setServiceTemplateExpressionMetadata(workflow, orchestrationWorkflow);

      StateMachine stateMachine = new StateMachine(workflow, workflow.getDefaultVersion(),
          ((CustomOrchestrationWorkflow) orchestrationWorkflow).getGraph(), stencilMap(workflow.getAppId()),
          infraRefactor, false);
      wingsPersistence.save(stateMachine);

      linkedTemplateUuids = workflow.getOrchestrationWorkflow().getLinkedTemplateUuids();
      linkedArtifactStreamIds = setLinkedArtifactStreamIdsAtWorkflowLevel(workflow);
      workflow.setOrchestration(orchestrationWorkflow);
    }

    String key = wingsPersistence.save(workflow);

    // create initial version
    entityVersionService.newEntityVersion(
        workflow.getAppId(), WORKFLOW, key, workflow.getName(), EntityVersion.ChangeType.CREATED, workflow.getNotes());

    Workflow newWorkflow = readWorkflow(workflow.getAppId(), key, workflow.getDefaultVersion());
    updateKeywordsAndLinkedTemplateUuids(newWorkflow, linkedTemplateUuids);
    String accountId = appService.getAccountIdByAppId(workflow.getAppId());

    if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
      updateLinkedArtifactStreamIds(newWorkflow, linkedArtifactStreamIds);
    }

    if (newWorkflow.getOrchestrationWorkflow() != null) {
      yamlPushService.pushYamlChangeSet(accountId, null, newWorkflow, Type.CREATE, workflow.isSyncFromGit(), false);
    }

    if (!newWorkflow.isSample()) {
      eventPublishHelper.publishWorkflowCreatedEvent(newWorkflow, accountId);
    }

    return newWorkflow;
  }

  private void validateWorkflowNameForDuplicates(Workflow workflow) {
    workflow.setName(workflow.getName().trim());
    notEmptyCheck("workflow name cannot be empty", workflow.getName());
    Workflow workflowWithSameName = wingsPersistence.createQuery(Workflow.class)
                                        .filter(WorkflowKeys.appId, workflow.getAppId())
                                        .filter(WorkflowKeys.name, workflow.getName())
                                        .get();
    if (Objects.nonNull(workflowWithSameName)
        && !StringUtils.equals(workflowWithSameName.getUuid(), workflow.getUuid())) {
      throw new InvalidRequestException("Duplicate name " + workflow.getName(), USER);
    }
  }

  private List<String> setLinkedArtifactStreamIdsAtWorkflowLevel(Workflow workflow) {
    List<String> linkedArtifactStreamIds = new ArrayList<>();
    if (featureFlagService.isEnabled(
            FeatureName.ARTIFACT_STREAM_REFACTOR, appService.getAccountIdByAppId(workflow.getAppId()))) {
      List<Variable> userVariables = workflow.getOrchestrationWorkflow().getUserVariables();
      if (isNotEmpty(userVariables)) {
        for (Variable userVariable : userVariables) {
          if (userVariable.getType() == VariableType.ARTIFACT) {
            if (isNotEmpty(userVariable.getAllowedList())) {
              for (String artifactStreamId : userVariable.getAllowedList()) {
                if (!linkedArtifactStreamIds.contains(artifactStreamId)) {
                  linkedArtifactStreamIds.add(artifactStreamId);
                }
              }
            }
          }
        }
        workflow.setLinkedArtifactStreamIds(linkedArtifactStreamIds);
      }
    }
    return linkedArtifactStreamIds;
  }

  private boolean isDaemonSet(String appId, String serviceId) {
    KubernetesContainerTask containerTask =
        (KubernetesContainerTask) serviceResourceService.getContainerTaskByDeploymentType(
            appId, serviceId, KUBERNETES.name());
    return containerTask != null && containerTask.checkDaemonSet();
  }

  private boolean isStatefulSet(String appId, String serviceId) {
    KubernetesContainerTask containerTask =
        (KubernetesContainerTask) serviceResourceService.getContainerTaskByDeploymentType(
            appId, serviceId, KUBERNETES.name());
    return containerTask != null && containerTask.checkStatefulSet();
  }

  private void updateKeywordsAndLinkedTemplateUuids(Workflow workflow, List<String> linkedTemplateUuids) {
    if (isNotEmpty(linkedTemplateUuids)) {
      linkedTemplateUuids = linkedTemplateUuids.stream().distinct().collect(toList());
    }

    Set<String> keywords = workflowServiceHelper.getKeywords(workflow);
    wingsPersistence.update(wingsPersistence.createQuery(Workflow.class)
                                .filter(Workflow.APP_ID_KEY, workflow.getAppId())
                                .filter(Workflow.ID_KEY, workflow.getUuid()),
        wingsPersistence.createUpdateOperations(Workflow.class)
            .set("keywords", keywords)
            .set("linkedTemplateUuids", linkedTemplateUuids));
    workflow.setKeywords(keywords);
    workflow.setLinkedTemplateUuids(linkedTemplateUuids);
  }

  private void updateLinkedArtifactStreamIds(Workflow workflow, List<String> linkedArtifactStreamIds) {
    if (isNotEmpty(linkedArtifactStreamIds)) {
      linkedArtifactStreamIds = linkedArtifactStreamIds.stream().distinct().collect(toList());
    }

    wingsPersistence.update(wingsPersistence.createQuery(Workflow.class)
                                .filter(Workflow.APP_ID_KEY, workflow.getAppId())
                                .filter(Workflow.ID_KEY, workflow.getUuid()),
        wingsPersistence.createUpdateOperations(Workflow.class)
            .set("linkedArtifactStreamIds", linkedArtifactStreamIds));
    workflow.setLinkedTemplateUuids(linkedArtifactStreamIds);
  }

  @Override
  public String getHPAYamlStringWithCustomMetric(
      Integer minAutoscaleInstances, Integer maxAutoscaleInstances, Integer targetCpuUtilizationPercentage) {
    return workflowServiceHelper.getHPAYamlStringWithCustomMetric(
        minAutoscaleInstances, maxAutoscaleInstances, targetCpuUtilizationPercentage);
  }

  @Override
  public boolean ensureArtifactCheck(String appId, OrchestrationWorkflow orchestrationWorkflow, boolean infraRefactor) {
    if (orchestrationWorkflow == null) {
      return false;
    }
    if (orchestrationWorkflow.getOrchestrationWorkflowType() == BUILD) {
      return false;
    }
    if (!(orchestrationWorkflow instanceof CanaryOrchestrationWorkflow)) {
      return false;
    }
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    if (canaryOrchestrationWorkflow.getWorkflowPhases() == null) {
      return false;
    }
    if (workflowServiceHelper.needArtifactCheckStep(appId, canaryOrchestrationWorkflow, infraRefactor)) {
      return workflowServiceHelper.ensureArtifactCheckInPreDeployment(canaryOrchestrationWorkflow);
    }
    return false;
  }

  @Override
  public Workflow updateWorkflow(Workflow workflow, boolean migration) {
    return updateWorkflow(workflow, workflow.getOrchestrationWorkflow(), migration);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Workflow updateLinkedWorkflow(Workflow workflow, Workflow existingWorkflow, boolean fromYaml) {
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck(
        "Orchestration not associated to the workflow [" + workflow.getName() + "]", orchestrationWorkflow, USER);

    CanaryOrchestrationWorkflow existingOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) existingWorkflow.getOrchestrationWorkflow();
    notNullCheck("Previous orchestration workflow with name [" + workflow.getName() + "] does not exist",
        existingOrchestrationWorkflow, USER);

    // Update Linked Predeployment steps
    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(
        orchestrationWorkflow.getPreDeploymentSteps(), existingOrchestrationWorkflow.getPreDeploymentSteps(), fromYaml);

    // Update Linked Postdeployment steps
    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(orchestrationWorkflow.getPostDeploymentSteps(),
        existingOrchestrationWorkflow.getPostDeploymentSteps(), fromYaml);

    // Update Workflow Phase steps
    workflowServiceTemplateHelper.updateLinkedWorkflowPhases(
        orchestrationWorkflow.getWorkflowPhases(), existingOrchestrationWorkflow.getWorkflowPhases(), fromYaml);
    return updateWorkflow(workflow, workflow.getOrchestrationWorkflow(), false);
  }

  @Override
  public Workflow updateWorkflow(Workflow workflow, OrchestrationWorkflow orchestrationWorkflow, boolean migration) {
    if (featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(workflow.getAppId()))) {
      if (!workflow.checkServiceTemplatized() && !workflow.checkInfraDefinitionTemplatized()) {
        workflowServiceHelper.validateServiceAndInfraDefinition(
            workflow.getAppId(), workflow.getServiceId(), workflow.getInfraDefinitionId());
      }
    } else {
      if (!workflow.checkServiceTemplatized() && !workflow.checkInfraTemplatized()) {
        workflowServiceHelper.validateServiceAndInfraMapping(
            workflow.getAppId(), workflow.getServiceId(), workflow.getInfraMappingId());
      }
    }

    if (!migration) {
      validateWorkflowNameForDuplicates(workflow);
      validateWorkflowVariables(orchestrationWorkflow);
    }
    return updateWorkflow(workflow, orchestrationWorkflow, true, false, false, false, migration);
  }

  @Override
  public Workflow updateWorkflow(Workflow workflow, OrchestrationWorkflow orchestrationWorkflow, boolean infraChanged,
      boolean envChanged, boolean cloned) {
    return updateWorkflow(workflow, orchestrationWorkflow, true, infraChanged, envChanged, cloned, false);
  }

  private Workflow updateWorkflow(Workflow workflow, OrchestrationWorkflow orchestrationWorkflow,
      boolean onSaveCallNeeded, boolean infraChanged, boolean envChanged, boolean cloned, boolean migration) {
    final String accountId = appService.getAccountIdByAppId(workflow.getAppId());
    boolean infraRefactor = featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, accountId);
    WorkflowServiceHelper.cleanupWorkflowStepSkipStrategies(orchestrationWorkflow);
    Workflow savedWorkflow = readWorkflow(workflow.getAppId(), workflow.getUuid());

    UpdateOperations<Workflow> ops = wingsPersistence.createUpdateOperations(Workflow.class);
    setUnset(ops, "description", workflow.getDescription());
    setUnset(ops, "name", workflow.getName());
    List<TemplateExpression> templateExpressions = workflow.getTemplateExpressions();

    String workflowName = workflow.getName();
    String serviceId = workflow.getServiceId();
    String envId = workflow.getEnvId();
    String inframappingId = workflow.getInfraMappingId();
    String infraDefinitionId = workflow.getInfraDefinitionId();
    boolean isRename = (workflow.getName() != null) && !workflow.getName().equals(savedWorkflow.getName());
    boolean isSyncFromGit = workflow.isSyncFromGit();

    if (orchestrationWorkflow == null) {
      workflow = readWorkflow(workflow.getAppId(), workflow.getUuid(), workflow.getDefaultVersion());
      orchestrationWorkflow = workflow.getOrchestrationWorkflow();
      if (envId != null) {
        if (workflow.getEnvId() == null || !workflow.getEnvId().equals(envId)) {
          envChanged = true;
        }
      }
    }

    if (isEmpty(templateExpressions)) {
      templateExpressions = new ArrayList<>();
    }
    orchestrationWorkflow = workflowServiceHelper.propagateWorkflowDataToPhases(orchestrationWorkflow,
        templateExpressions, workflow.getAppId(), serviceId, infraRefactor ? infraDefinitionId : inframappingId,
        envChanged, infraChanged, migration);

    workflowServiceTemplateHelper.setServiceTemplateExpressionMetadata(workflow, orchestrationWorkflow);

    setUnset(ops, "templateExpressions", templateExpressions);

    List<String> linkedTemplateUuids = new ArrayList<>();
    List<String> linkedArtifactStreamIds = new ArrayList<>();
    if (orchestrationWorkflow != null) {
      if (onSaveCallNeeded) {
        orchestrationWorkflow.onSave();
        if (envChanged) {
          workflow.setEnvId(envId);
          setUnset(ops, "envId", envId);
        }
      }
      if (!cloned) {
        EntityVersion entityVersion = entityVersionService.newEntityVersion(workflow.getAppId(), WORKFLOW,
            workflow.getUuid(), workflow.getName(), EntityVersion.ChangeType.UPDATED, workflow.getNotes());
        workflow.setDefaultVersion(entityVersion.getVersion());
      }

      workflowServiceTemplateHelper.populatePropertiesFromWorkflow(workflow);

      StateMachine stateMachine = new StateMachine(workflow, workflow.getDefaultVersion(),
          ((CustomOrchestrationWorkflow) orchestrationWorkflow).getGraph(), stencilMap(workflow.getAppId()),
          infraRefactor, migration);

      stateMachine.validate();

      wingsPersistence.save(stateMachine);

      setUnset(ops, "defaultVersion", workflow.getDefaultVersion());
      linkedTemplateUuids = workflow.getOrchestrationWorkflow().getLinkedTemplateUuids();
      linkedArtifactStreamIds = setLinkedArtifactStreamIdsAtWorkflowLevel(workflow);
      setUnset(ops, "orchestration", workflow.getOrchestrationWorkflow());
    }

    wingsPersistence.update(wingsPersistence.createQuery(Workflow.class)
                                .filter(WorkflowKeys.appId, workflow.getAppId())
                                .filter(ID_KEY, workflow.getUuid()),
        ops);

    Workflow finalWorkflow = readWorkflow(workflow.getAppId(), workflow.getUuid(), workflow.getDefaultVersion());

    if (!migration) {
      yamlPushService.pushYamlChangeSet(accountId, savedWorkflow, finalWorkflow, Type.UPDATE, isSyncFromGit, isRename);
    }

    if (workflowName != null) {
      if (!workflowName.equals(finalWorkflow.getName())) {
        executorService.submit(() -> triggerService.updateByApp(finalWorkflow.getAppId()));
      }
    }
    updateKeywordsAndLinkedTemplateUuids(finalWorkflow, linkedTemplateUuids);
    if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
      updateLinkedArtifactStreamIds(finalWorkflow, linkedArtifactStreamIds);
    }
    return finalWorkflow;
  }

  private void populateServices(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    workflow.setServices(
        serviceResourceService.fetchServicesByUuids(workflow.getAppId(), orchestrationWorkflow.getServiceIds()));
    workflow.setTemplatizedServiceIds(orchestrationWorkflow.getTemplatizedServiceIds());
  }

  private void ensureWorkflowSafeToDelete(Workflow workflow) {
    List<Pipeline> pipelines = pipelineService.listPipelines(
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(Pipeline.APP_ID_KEY, EQ, workflow.getAppId())
            .addFilter("pipelineStages.pipelineStageElements.properties.workflowId", EQ, workflow.getUuid())
            .build());

    if (isNotEmpty(pipelines)) {
      List<String> pipelineNames = pipelines.stream().map(Pipeline::getName).collect(toList());
      String message = format("Workflow is referenced by %d %s [%s].", pipelines.size(),
          plural("pipeline", pipelines.size()), Joiner.on(", ").join(pipelineNames));
      throw new InvalidRequestException(message, USER);
    }

    if (workflowExecutionService.workflowExecutionsRunning(
            workflow.getWorkflowType(), workflow.getAppId(), workflow.getUuid())) {
      throw new InvalidRequestException(
          format("Workflow: [%s] couldn't be deleted", workflow.getName()), WORKFLOW_EXECUTION_IN_PROGRESS, USER);
    }

    List<String> triggerNames;
    if (featureFlagService.isEnabled(FeatureName.TRIGGER_REFACTOR, workflow.getAccountId())) {
      triggerNames = deploymentTriggerService.getTriggersHasWorkflowAction(workflow.getAppId(), workflow.getUuid());
    } else {
      List<Trigger> triggers = triggerService.getTriggersHasWorkflowAction(workflow.getAppId(), workflow.getUuid());
      if (isEmpty(triggers)) {
        return;
      }
      triggerNames = triggers.stream().map(Trigger::getName).collect(toList());
    }

    List<Trigger> triggers = triggerService.getTriggersHasWorkflowAction(workflow.getAppId(), workflow.getUuid());
    if (isEmpty(triggers)) {
      return;
    }

    throw new InvalidRequestException(
        format("Workflow associated as a trigger action to triggers [%s]", Joiner.on(", ").join(triggerNames)), USER);
  }

  private boolean pruneWorkflow(String appId, String workflowId) {
    pruneQueue.send(new PruneEvent(Workflow.class, appId, workflowId));
    return wingsPersistence.delete(Workflow.class, appId, workflowId);
  }

  @Override
  public boolean deleteWorkflow(String appId, String workflowId) {
    String accountId = appService.getAccountIdByAppId(appId);
    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new Action(accountId, ActionType.CREATE_WORKFLOW));

    return LimitEnforcementUtils.withCounterDecrement(
        checker, () -> { return deleteWorkflow(appId, workflowId, false, false); });
  }

  private boolean deleteWorkflow(String appId, String workflowId, boolean forceDelete, boolean syncFromGit) {
    Workflow workflow = wingsPersistence.getWithAppId(Workflow.class, appId, workflowId);
    if (workflow == null) {
      return true;
    }

    if (!forceDelete) {
      ensureWorkflowSafeToDelete(workflow);
    }

    String accountId = appService.getAccountIdByAppId(workflow.getAppId());
    yamlPushService.pushYamlChangeSet(accountId, workflow, null, Type.DELETE, syncFromGit, false);

    return pruneWorkflow(appId, workflowId);
  }

  @Override
  public boolean deleteByYamlGit(String appId, String workflowId, boolean syncFromGit) {
    return deleteWorkflow(appId, workflowId, false, syncFromGit);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StateMachine readLatestStateMachine(String appId, String originId) {
    return wingsPersistence.createQuery(StateMachine.class)
        .filter(StateMachineKeys.appId, appId)
        .filter(StateMachineKeys.originId, originId)
        .order(Sort.descending(StateMachineKeys.createdAt))
        .get();
  }

  @Override
  public StateMachine readStateMachine(String appId, String originId, Integer version) {
    return wingsPersistence.createQuery(StateMachine.class)
        .filter(StateMachineKeys.appId, appId)
        .filter(StateMachineKeys.originId, originId)
        .filter(StateMachineKeys.originVersion, version)
        .get();
  }

  @Override
  public void pruneByApplication(String appId) {
    // prune workflows
    List<Workflow> workflows = wingsPersistence.createQuery(Workflow.class)
                                   .filter(WorkflowKeys.appId, appId)
                                   .project(WorkflowKeys.name, true)
                                   .project(WorkflowKeys.accountId, true)
                                   .project(WorkflowKeys.appId, true)
                                   .project(WorkflowKeys.uuid, true)
                                   .asList();

    String accountId = null;
    for (Workflow workflow : workflows) {
      accountId = workflow.getAccountId();
      if (pruneWorkflow(appId, workflow.getUuid())) {
        auditServiceHelper.reportDeleteForAuditing(appId, workflow);
      }
      harnessTagService.pruneTagLinks(workflow.getAccountId(), workflow.getUuid());
    }

    if (StringUtils.isNotEmpty(accountId)) {
      counterSyncer.syncWorkflowCount(accountId);
    }

    // prune state machines
    wingsPersistence.delete(wingsPersistence.createQuery(StateMachine.class).filter(StateMachineKeys.appId, appId));
  }

  /**
   * Sets static configuration.
   *
   * @param staticConfiguration the static configuration
   */
  public void setStaticConfiguration(StaticConfiguration staticConfiguration) {
    this.staticConfiguration = staticConfiguration;
  }

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    List<Workflow> workflows = wingsPersistence.createQuery(Workflow.class).filter(WorkflowKeys.appId, appId).asList();
    return workflows.stream().collect(toMap(Workflow::getUuid, Workflow::getName));
  }

  StateType getCorrespondingRollbackState(GraphNode step) {
    if (step.getType().equals(CLOUD_FORMATION_CREATE_STACK.name())) {
      return StateType.CLOUD_FORMATION_ROLLBACK_STACK;
    }

    if (step.getType().equals(StateType.TERRAFORM_PROVISION.name())) {
      if (isTerraformPlanState(step)) {
        // We are only running plan in the TF state
        return null;
      } else {
        return TERRAFORM_ROLLBACK;
      }
    }
    return null;
  }

  private boolean isTerraformPlanState(GraphNode step) {
    if (step.getType().equals(StateType.TERRAFORM_PROVISION.name())) {
      Map<String, Object> properties = step.getProperties();
      Object o = properties.get(RUN_PLAN_ONLY_KEY);
      return (o instanceof Boolean) && ((Boolean) o);
    }
    return false;
  }

  private boolean isTerraformInheritState(GraphNode step) {
    if (step.getType().equals(StateType.TERRAFORM_PROVISION.name())) {
      Map<String, Object> properties = step.getProperties();
      Object o = properties.get(INHERIT_APPROVED_PLAN);
      return (o instanceof Boolean) && ((Boolean) o);
    }
    return false;
  }

  private PhaseStep generateRollbackProvisioners(
      PhaseStep preDeploymentSteps, PhaseStepType phaseStepType, String phaseStepName) {
    List<GraphNode> provisionerSteps = preDeploymentSteps.getSteps()
                                           .stream()
                                           .filter(step -> {
                                             return StateType.TERRAFORM_PROVISION.name().equals(step.getType())
                                                 || CLOUD_FORMATION_CREATE_STACK.name().equals(step.getType());
                                           })
                                           .collect(Collectors.toList());
    if (isEmpty(provisionerSteps)) {
      return null;
    }
    List<GraphNode> rollbackProvisionerNodes = Lists.newArrayList();
    Map<String, String> provisionerIdWorkspaceMap = new HashMap<>();
    PhaseStep rollbackProvisionerStep = new PhaseStep(phaseStepType, phaseStepName);
    rollbackProvisionerStep.setUuid(generateUuid());
    provisionerSteps.forEach(step -> {
      StateType stateType = getCorrespondingRollbackState(step);
      if (isTerraformPlanState(step) && step.getProperties().get("workspace") != null) {
        provisionerIdWorkspaceMap.put(
            (String) step.getProperties().get("provisionerId"), (String) step.getProperties().get("workspace"));
      }
      if (stateType != null) {
        Map<String, Object> propertiesMap = Maps.newHashMap();
        propertiesMap.put("provisionerId", step.getProperties().get("provisionerId"));
        propertiesMap.put("timeoutMillis", step.getProperties().get("timeoutMillis"));
        propertiesMap.put("workspace",
            isTerraformInheritState(step) ? provisionerIdWorkspaceMap.get(step.getProperties().get("provisionerId"))
                                          : step.getProperties().get("workspace"));
        rollbackProvisionerNodes.add(GraphNode.builder()
                                         .type(stateType.name())
                                         .rollback(true)
                                         .name("Rollback " + step.getName())
                                         .properties(propertiesMap)
                                         .build());
      }
    });
    rollbackProvisionerStep.setRollback(true);
    rollbackProvisionerStep.setSteps(rollbackProvisionerNodes);
    return rollbackProvisionerStep;
  }

  @Override
  public PhaseStep updatePreDeployment(String appId, String workflowId, PhaseStep phaseStep) {
    WorkflowServiceHelper.cleanupStepSkipStrategies(phaseStep);
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("Workflow", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck(ORCHESTRATION_WORKFLOW, orchestrationWorkflow, USER);

    // Update linked PhaseStep template
    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(
        phaseStep, orchestrationWorkflow.getPreDeploymentSteps());
    orchestrationWorkflow.setPreDeploymentSteps(phaseStep);
    orchestrationWorkflow.setRollbackProvisioners(
        generateRollbackProvisioners(phaseStep, PhaseStepType.ROLLBACK_PROVISIONERS, ROLLBACK_PROVISIONERS));

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow, false).getOrchestrationWorkflow();
    return orchestrationWorkflow.getPreDeploymentSteps();
  }

  @Override
  public PhaseStep updatePostDeployment(String appId, String workflowId, PhaseStep phaseStep) {
    WorkflowServiceHelper.cleanupStepSkipStrategies(phaseStep);
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck(ORCHESTRATION_WORKFLOW, orchestrationWorkflow, USER);
    // Update linked PhaseStep template
    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(
        phaseStep, orchestrationWorkflow.getPostDeploymentSteps());
    orchestrationWorkflow.setPostDeploymentSteps(phaseStep);

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow, false).getOrchestrationWorkflow();
    return orchestrationWorkflow.getPostDeploymentSteps();
  }

  @Override
  public WorkflowPhase createWorkflowPhase(String appId, String workflowId, WorkflowPhase workflowPhase) {
    notNullCheck("workflow", workflowPhase, USER);

    boolean infraRefactor = featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(appId));

    if (infraRefactor) {
      workflowServiceHelper.validateServiceAndInfraDefinition(
          appId, workflowPhase.getServiceId(), workflowPhase.getInfraDefinitionId());
    } else {
      workflowServiceHelper.validateServiceAndInfraMapping(
          appId, workflowPhase.getServiceId(), workflowPhase.getInfraMappingId());
    }

    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();

    notNullCheck(ORCHESTRATION_WORKFLOW, orchestrationWorkflow, USER);

    workflowPhase.setDaemonSet(isDaemonSet(appId, workflowPhase.getServiceId()));
    workflowPhase.setStatefulSet(isStatefulSet(appId, workflowPhase.getServiceId()));

    boolean isV2ServicePresent = StringUtils.isNotEmpty(workflowPhase.getServiceId())
        && workflowServiceHelper.isK8sV2Service(workflow.getAppId(), workflowPhase.getServiceId());

    WorkflowCreator workflowCreator = workflowCreatorFactory.getWorkflowCreatorFactory(
        orchestrationWorkflow.getOrchestrationWorkflowType(), isV2ServicePresent);

    workflowCreator.attachWorkflowPhase(workflow, workflowPhase);

    if (artifactCheckRequiredForDeployment(workflowPhase, orchestrationWorkflow)) {
      workflowServiceHelper.ensureArtifactCheckInPreDeployment(
          (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow());
    }

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow, false).getOrchestrationWorkflow();
    return orchestrationWorkflow.getWorkflowPhaseIdMap().get(workflowPhase.getUuid());
  }

  private boolean artifactCheckRequiredForDeployment(
      WorkflowPhase workflowPhase, CanaryOrchestrationWorkflow orchestrationWorkflow) {
    return (workflowPhase.getDeploymentType() == SSH || workflowPhase.getDeploymentType() == DeploymentType.PCF)
        && orchestrationWorkflow.getOrchestrationWorkflowType() != BUILD;
  }

  @Override
  public WorkflowPhase cloneWorkflowPhase(String appId, String workflowId, WorkflowPhase workflowPhase) {
    String phaseId = workflowPhase.getUuid();
    String phaseName = workflowPhase.getName();
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck(ORCHESTRATION_WORKFLOW, orchestrationWorkflow, USER);

    workflowPhase = orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId);
    notNullCheck("workflowPhase", workflowPhase, USER);

    WorkflowPhase clonedWorkflowPhase = workflowPhase.cloneInternal();
    clonedWorkflowPhase.setName(phaseName);

    orchestrationWorkflow.getWorkflowPhases().add(clonedWorkflowPhase);

    WorkflowPhase rollbackWorkflowPhase =
        orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());

    if (rollbackWorkflowPhase != null) {
      WorkflowPhase clonedRollbackWorkflowPhase = rollbackWorkflowPhase.cloneInternal();
      orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(
          clonedWorkflowPhase.getUuid(), clonedRollbackWorkflowPhase);
    }

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow, false).getOrchestrationWorkflow();

    return orchestrationWorkflow.getWorkflowPhaseIdMap().get(clonedWorkflowPhase.getUuid());
  }

  @Override
  public Map<String, String> getStateDefaults(String appId, String serviceId, StateType stateType) {
    return workflowServiceHelper.getStateDefaults(appId, serviceId, stateType);
  }

  @Override
  public List<Service> getResolvedServices(Workflow workflow, Map<String, String> workflowVariables) {
    return workflowServiceHelper.getResolvedServices(workflow, workflowVariables);
  }

  @Override
  public List<InfrastructureMapping> getResolvedInfraMappings(
      Workflow workflow, Map<String, String> workflowVariables) {
    return workflowServiceHelper.getResolvedInfraMappings(workflow, workflowVariables);
  }

  @Override
  public List<InfrastructureDefinition> getResolvedInfraDefinitions(
      Workflow workflow, Map<String, String> workflowVariables) {
    return workflowServiceHelper.getResolvedInfraDefinitions(workflow, workflowVariables);
  }

  @Override
  public List<String> getResolvedInfraMappingIds(Workflow workflow, Map<String, String> workflowVariables) {
    return workflowServiceHelper.getResolvedInfraMappingIds(workflow, workflowVariables);
  }

  @Override
  public List<String> getResolvedInfraDefinitionIds(Workflow workflow, Map<String, String> workflowVariables) {
    return workflowServiceHelper.getResolvedInfraDefinitionIds(workflow, workflowVariables);
  }

  @Override
  public void pruneDescendingEntities(String appId, String workflowId) {
    List<OwnedByWorkflow> services =
        ServiceClassLocator.descendingServices(this, WorkflowServiceImpl.class, OwnedByWorkflow.class);
    PruneEntityListener.pruneDescendingEntities(services, descending -> descending.pruneByWorkflow(appId, workflowId));
  }

  @Override
  public boolean workflowHasSshDeploymentPhase(String appId, String workflowId) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("Workflow", workflow, USER);
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    notNullCheck(ORCHESTRATION_WORKFLOW, orchestrationWorkflow, USER);
    if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
      return workflowServiceHelper.workflowHasSshDeploymentPhase(canaryOrchestrationWorkflow);
    }
    return false;
  }

  private void preAppendRollbackProvisionInfrastructure(
      WorkflowPhase workflowPhase, WorkflowPhase rollbackWorkflowPhase) {
    if (workflowPhase == null || rollbackWorkflowPhase == null) {
      return;
    }
    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();
    if (isEmpty(phaseSteps)) {
      return;
    }
    if (PhaseStepType.PROVISION_INFRASTRUCTURE != phaseSteps.get(0).getPhaseStepType()) {
      return;
    }
    PhaseStep rollbackProvisionInfrastructure = generateRollbackProvisioners(
        phaseSteps.get(0), PhaseStepType.ROLLBACK_PROVISION_INFRASTRUCTURE, ROLLBACK_PROVISION_INFRASTRUCTURE);
    List<PhaseStep> rollbackPhaseSteps = Lists.newArrayList();
    if (rollbackProvisionInfrastructure != null) {
      rollbackPhaseSteps.add(rollbackProvisionInfrastructure);
    }
    rollbackPhaseSteps.addAll(
        rollbackWorkflowPhase.getPhaseSteps()
            .stream()
            .filter(phaseStep -> phaseStep.getPhaseStepType() != PhaseStepType.ROLLBACK_PROVISION_INFRASTRUCTURE)
            .collect(toList()));
    rollbackWorkflowPhase.getPhaseSteps().clear();
    rollbackWorkflowPhase.getPhaseSteps().addAll(rollbackPhaseSteps);
  }

  @Override
  @ValidationGroups(Update.class)
  public WorkflowPhase updateWorkflowPhase(
      @NotEmpty String appId, @NotEmpty String workflowId, @Valid WorkflowPhase workflowPhase) {
    boolean infraRefactor = featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(appId));
    if (workflowPhase.isRollback()
        || workflowPhase.getPhaseSteps().stream().anyMatch(
               phaseStep -> phaseStep.isRollback() || phaseStep.getSteps().stream().anyMatch(GraphNode::isRollback))) {
      // This might seem as user error, but since this is controlled from the our UI lets get alerted for it
      throw new InvalidRequestException("The direct workflow phase should not have rollback flag set!", USER_SRE);
    }

    WorkflowServiceHelper.cleanupPhaseStepSkipStrategies(workflowPhase);
    Workflow workflow = readWorkflow(appId, workflowId);
    if (workflow == null) {
      throw new InvalidArgumentsException(Pair.of("application", appId), Pair.of("workflow", workflowId),
          new ExplanationException("This might be caused from someone else deleted "
                  + "the application and/or the workflow while you worked on it.",
              MOVE_TO_THE_PARENT_OBJECT));
    }
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck(ORCHESTRATION_WORKFLOW, orchestrationWorkflow, USER);

    if (orchestrationWorkflow.getWorkflowPhaseIdMap().get(workflowPhase.getUuid()) == null) {
      throw new InvalidArgumentsException(Pair.of("workflow", workflowId), Pair.of("workflowPhase", appId),
          new ExplanationException("This might be caused from someone else modified "
                  + "the workflow resulting in removing the phase that you worked on.",
              MOVE_TO_THE_PARENT_OBJECT));
    }

    String serviceId = workflowPhase.getServiceId();
    String infraMappingId = null;
    String infraDefinitionId = null;

    if (infraRefactor) {
      infraDefinitionId = workflowPhase.getInfraDefinitionId();
    } else {
      infraMappingId = workflowPhase.getInfraMappingId();
    }

    if (orchestrationWorkflow.getOrchestrationWorkflowType() != BUILD) {
      Service service = serviceResourceService.get(appId, workflowPhase.getServiceId(), false);
      if (service == null && !workflowPhase.checkServiceTemplatized()) {
        throw new InvalidRequestException("Service [" + workflowPhase.getServiceId() + "] does not exist", USER);
      }
      InfrastructureMapping infrastructureMapping = null;
      InfrastructureDefinition infrastructureDefinition = null;

      if (infraRefactor) {
        if (!workflowPhase.checkInfraDefinitionTemplatized()) {
          if (infraDefinitionId == null) {
            throw new InvalidRequestException(
                format(WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE, workflowPhase.getName()), USER);
          }
          infrastructureDefinition = infrastructureDefinitionService.get(appId, infraDefinitionId);
          notNullCheck("InfraDefinition", infrastructureDefinition, USER);
          if (service.getDeploymentType() != null) {
            if (service.getDeploymentType() != infrastructureDefinition.getDeploymentType()) {
              throw new InvalidRequestException("Infrastructure Definition[" + infrastructureDefinition.getName()
                      + "] not compatible with Service [" + service.getName() + "]",
                  USER);
            }
          }
          setCloudProviderForPhaseInfraRefactor(infrastructureDefinition, workflowPhase);
        }
      } else {
        if (!workflowPhase.checkInfraTemplatized()) {
          if (infraMappingId == null) {
            throw new InvalidRequestException(
                format(WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE, workflowPhase.getName()), USER);
          }
          infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
          notNullCheck("InfraMapping", infrastructureMapping, USER);
          if (!service.getUuid().equals(infrastructureMapping.getServiceId())) {
            throw new InvalidRequestException("Service Infrastructure [" + infrastructureMapping.getName()
                    + "] not mapped to Service [" + service.getName() + "]",
                USER);
          }
          setCloudProviderForPhase(infrastructureMapping, workflowPhase);
        }
      }

      WorkflowPhase rollbackWorkflowPhase =
          orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());
      if (rollbackWorkflowPhase != null) {
        rollbackWorkflowPhase.setServiceId(serviceId);
        if (infraRefactor) {
          rollbackWorkflowPhase.setInfraDefinitionId(infraDefinitionId);
          if (infrastructureDefinition != null) {
            setCloudProviderForPhaseInfraRefactor(infrastructureDefinition, rollbackWorkflowPhase);
          }
        } else {
          rollbackWorkflowPhase.setInfraMappingId(infraMappingId);
          if (infrastructureMapping != null) {
            setCloudProviderForPhase(infrastructureMapping, rollbackWorkflowPhase);
          }
        }
        preAppendRollbackProvisionInfrastructure(workflowPhase, rollbackWorkflowPhase);
        orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
      }
    }

    boolean found = false;
    boolean infraChanged = false;
    String oldInfraMappingId = null;
    String oldServiceId = null;
    String oldInfraDefinitionId = null;
    for (int i = 0; i < orchestrationWorkflow.getWorkflowPhases().size(); i++) {
      WorkflowPhase oldWorkflowPhase = orchestrationWorkflow.getWorkflowPhases().get(i);
      if (oldWorkflowPhase.getUuid().equals(workflowPhase.getUuid())) {
        if (infraRefactor) {
          oldInfraDefinitionId = oldWorkflowPhase.getInfraDefinitionId();
        } else {
          oldInfraMappingId = oldWorkflowPhase.getInfraMappingId();
        }
        oldServiceId = oldWorkflowPhase.getServiceId();
        orchestrationWorkflow.getWorkflowPhases().remove(i);
        workflowPhase.setName(workflowPhase.getName().trim());
        orchestrationWorkflow.getWorkflowPhases().add(i, workflowPhase);
        orchestrationWorkflow.getWorkflowPhaseIdMap().put(workflowPhase.getUuid(), workflowPhase);
        found = true;
        // Update the workflow phase
        workflowServiceTemplateHelper.updateLinkedWorkflowPhaseTemplate(workflowPhase, oldWorkflowPhase);
        break;
      }
    }
    if (BUILD != orchestrationWorkflow.getOrchestrationWorkflowType()) {
      if (!workflowPhase.checkServiceTemplatized()) {
        workflowServiceHelper.validateServiceCompatibility(appId, serviceId, oldServiceId);
      }
      if (infraRefactor) {
        if (!workflowPhase.checkInfraDefinitionTemplatized()) {
          if (!infraDefinitionId.equals(oldInfraDefinitionId)) {
            infraChanged = true;
          }
        }
      } else {
        if (!workflowPhase.checkInfraTemplatized()) {
          if (!infraMappingId.equals(oldInfraMappingId)) {
            infraChanged = true;
          }
        }
      }
      // Propagate template expressions to workflow level
      if (orchestrationWorkflow.getOrchestrationWorkflowType() == BASIC
          || orchestrationWorkflow.getOrchestrationWorkflowType() == ROLLING
          || orchestrationWorkflow.getOrchestrationWorkflowType() == BLUE_GREEN) {
        WorkflowServiceTemplateHelper.setTemplateExpresssionsFromPhase(workflow, workflowPhase, infraRefactor);
      } else {
        if (infraRefactor) {
          WorkflowServiceTemplateHelper.validateTemplateExpressionsInfraRefactor(
              workflowPhase.getTemplateExpressions());
        } else {
          WorkflowServiceTemplateHelper.validateTemplateExpressions(workflowPhase.getTemplateExpressions());
        }
      }
    }

    if (!found) {
      throw new InvalidRequestException("No matching Workflow Phase", USER);
    }

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow, infraChanged, false, false)
            .getOrchestrationWorkflow();
    return orchestrationWorkflow.getWorkflowPhaseIdMap().get(workflowPhase.getUuid());
  }

  private void setCloudProviderForPhase(
      InfrastructureMapping infrastructureMapping, WorkflowPhase rollbackWorkflowPhase) {
    rollbackWorkflowPhase.setComputeProviderId(infrastructureMapping.getComputeProviderSettingId());
    rollbackWorkflowPhase.setInfraMappingName(infrastructureMapping.getName());
    DeploymentType deploymentType =
        serviceResourceService.getDeploymentType(infrastructureMapping, null, infrastructureMapping.getServiceId());
    rollbackWorkflowPhase.setDeploymentType(deploymentType);
  }

  private void setCloudProviderForPhaseInfraRefactor(
      InfrastructureDefinition infrastructureDefinition, WorkflowPhase rollbackWorkflowPhase) {
    rollbackWorkflowPhase.setComputeProviderId(infrastructureDefinition.getInfrastructure().getCloudProviderId());
    rollbackWorkflowPhase.setDeploymentType(infrastructureDefinition.getDeploymentType());
  }

  @Override
  public WorkflowPhase updateWorkflowPhaseRollback(
      String appId, String workflowId, String phaseId, WorkflowPhase rollbackWorkflowPhase) {
    if (!rollbackWorkflowPhase.isRollback()
        || rollbackWorkflowPhase.getPhaseSteps().stream().anyMatch(phaseStep
               -> !phaseStep.isRollback() || phaseStep.getSteps().stream().anyMatch(step -> !step.isRollback()))) {
      // This might seem as user error, but since this is controlled from the our UI lets get alerted for it
      throw new InvalidRequestException("The rollback workflow phase should have rollback flag set!", USER_SRE);
    }

    WorkflowServiceHelper.cleanupPhaseStepSkipStrategies(rollbackWorkflowPhase);
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();

    validateWorkflowPhase(phaseId, orchestrationWorkflow);

    WorkflowPhase oldRollbackWorkflowPhase = orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseId);
    workflowServiceTemplateHelper.updateLinkedWorkflowPhaseTemplate(rollbackWorkflowPhase, oldRollbackWorkflowPhase);

    orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(phaseId, rollbackWorkflowPhase);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow, false).getOrchestrationWorkflow();
    return orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseId);
  }

  @Override
  public void deleteWorkflowPhase(String appId, String workflowId, String phaseId) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow, USER);

    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();

    validateWorkflowPhase(phaseId, orchestrationWorkflow);

    orchestrationWorkflow.getWorkflowPhases().remove(orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId));
    orchestrationWorkflow.getWorkflowPhaseIdMap().remove(phaseId);
    orchestrationWorkflow.getWorkflowPhaseIds().remove(phaseId);
    orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().remove(phaseId);
    updateWorkflow(workflow, orchestrationWorkflow, false);
  }

  private void validateWorkflowPhase(String phaseId, CanaryOrchestrationWorkflow orchestrationWorkflow) {
    notNullCheck(ORCHESTRATION_WORKFLOW, orchestrationWorkflow, USER);
    notNullCheck("WorkflowPhase", orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId), USER);
  }

  @Override
  public GraphNode updateGraphNode(String appId, String workflowId, String subworkflowId, GraphNode node) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck(ORCHESTRATION_WORKFLOW, orchestrationWorkflow, USER);

    Graph graph = orchestrationWorkflow.getGraph().getSubworkflows().get(subworkflowId);

    boolean found = false;
    for (int i = 0; i < graph.getNodes().size(); i++) {
      GraphNode childNode = graph.getNodes().get(i);
      if (childNode.getId().equals(node.getId())) {
        graph.getNodes().remove(i);
        graph.getNodes().add(i, node);
        found = true;
        break;
      }
    }

    if (!found) {
      throw new InvalidRequestException("node not found", USER);
    }

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow, false).getOrchestrationWorkflow();
    final Optional<GraphNode> graphNode = orchestrationWorkflow.getGraph()
                                              .getSubworkflows()
                                              .get(subworkflowId)
                                              .getNodes()
                                              .stream()
                                              .filter(n -> node.getId().equals(n.getId()))
                                              .findFirst();
    return graphNode.isPresent() ? graphNode.get() : null;
  }

  @Override
  @ValidationGroups(Create.class)
  public Workflow cloneWorkflow(String appId, Workflow originalWorkflow, @Valid Workflow workflow) {
    String accountId = appService.getAccountIdByAppId(workflow.getAppId());

    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new Action(accountId, ActionType.CREATE_WORKFLOW));

    return LimitEnforcementUtils.withLimitCheck(
        checker, () -> cloneWorkflowInternal(appId, originalWorkflow, workflow));
  }

  private Workflow cloneWorkflowInternal(String appId, Workflow originalWorkflow, Workflow workflow) {
    Workflow clonedWorkflow = cloneWorkflow(workflow, originalWorkflow);

    validateWorkflowNameForDuplicates(clonedWorkflow);

    clonedWorkflow.setDefaultVersion(1);
    String key = wingsPersistence.save(clonedWorkflow);
    entityVersionService.newEntityVersion(
        appId, WORKFLOW, key, clonedWorkflow.getName(), EntityVersion.ChangeType.CREATED, workflow.getNotes());

    Workflow savedWorkflow = readWorkflow(appId, key, clonedWorkflow.getDefaultVersion());

    if (originalWorkflow.getOrchestrationWorkflow() != null) {
      savedWorkflow.setOrchestrationWorkflow(originalWorkflow.getOrchestrationWorkflow().cloneInternal());
    }
    return updateWorkflow(savedWorkflow, savedWorkflow.getOrchestrationWorkflow(), false, false, false, true, false);
  }

  private Workflow cloneWorkflow(Workflow workflow, Workflow originalWorkflow) {
    Workflow clonedWorkflow = originalWorkflow.cloneInternal();
    clonedWorkflow.setName(workflow.getName());
    clonedWorkflow.setDescription(workflow.getDescription());
    return clonedWorkflow;
  }

  @Override
  public Workflow cloneWorkflow(String appId, Workflow originalWorkflow, CloneMetadata cloneMetadata) {
    notNullCheck("cloneMetadata", cloneMetadata, USER);
    Workflow workflow = cloneMetadata.getWorkflow();
    notNullCheck("workflow", workflow, USER);
    workflow.setAppId(appId);
    String targetAppId = cloneMetadata.getTargetAppId();
    if (targetAppId == null || targetAppId.equals(appId)) {
      return cloneWorkflow(appId, originalWorkflow, workflow);
    }
    logger.info("Cloning workflow across applications. "
        + "Environment, Service Infrastructure and Node selection will not be cloned");
    workflowServiceHelper.validateServiceMapping(appId, targetAppId, cloneMetadata.getServiceMapping());
    Workflow clonedWorkflow = cloneWorkflow(workflow, originalWorkflow);
    clonedWorkflow.setAppId(targetAppId);
    clonedWorkflow.setEnvId(null);
    Workflow savedWorkflow = createWorkflow(clonedWorkflow);
    OrchestrationWorkflow orchestrationWorkflow = originalWorkflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow != null) {
      OrchestrationWorkflow clonedOrchestrationWorkflow = orchestrationWorkflow.cloneInternal();
      // Set service ids
      clonedOrchestrationWorkflow.setCloneMetadata(cloneMetadata.getServiceMapping());
      savedWorkflow.setOrchestrationWorkflow(clonedOrchestrationWorkflow);
    }
    return updateWorkflow(savedWorkflow, savedWorkflow.getOrchestrationWorkflow(), false, true, true, true, false);
  }

  @Override
  public Workflow updateWorkflow(String appId, String workflowId, Integer defaultVersion) {
    Workflow workflow = readWorkflow(appId, workflowId, null);
    wingsPersistence.update(
        workflow, wingsPersistence.createUpdateOperations(Workflow.class).set("defaultVersion", defaultVersion));

    Workflow finalWorkflow = readWorkflow(appId, workflowId, defaultVersion);

    String accountId = appService.getAccountIdByAppId(finalWorkflow.getAppId());
    yamlPushService.pushYamlChangeSet(accountId, workflow, finalWorkflow, Type.UPDATE, false, false);

    return finalWorkflow;
  }

  @Override
  public List<NotificationRule> updateNotificationRules(
      String appId, String workflowId, List<NotificationRule> notificationRules) {
    notificationRules.forEach(WorkflowServiceImpl::validateNotificationRule);

    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck(ORCHESTRATION_WORKFLOW, orchestrationWorkflow);

    orchestrationWorkflow.setNotificationRules(notificationRules);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow, false).getOrchestrationWorkflow();
    return orchestrationWorkflow.getNotificationRules();
  }

  @Override
  public ConcurrencyStrategy updateConcurrencyStrategy(
      String appId, String workflowId, @NotNull @Valid ConcurrencyStrategy concurrencyStrategy) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow);
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    notNullCheck(ORCHESTRATION_WORKFLOW, orchestrationWorkflow);

    if (concurrencyStrategy.getUnitType() == UnitType.INFRA) {
      concurrencyStrategy.setResourceUnit(INFRA_ID_EXPRESSION);
    }
    orchestrationWorkflow.setConcurrencyStrategy(concurrencyStrategy);
    orchestrationWorkflow = updateWorkflow(workflow, orchestrationWorkflow, false).getOrchestrationWorkflow();
    return orchestrationWorkflow.getConcurrencyStrategy();
  }

  private static void validateNotificationRule(NotificationRule notificationRule) {
    if (notificationRule.isUserGroupAsExpression() && StringUtils.isEmpty(notificationRule.getUserGroupExpression())) {
      logger.error("[ILLEGAL_STATE]: isUserGroupAsExpression = true but userGroupExpression is empty.");
    }
  }

  @Override
  public List<FailureStrategy> updateFailureStrategies(
      String appId, String workflowId, List<FailureStrategy> failureStrategies) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck(WORKFLOW_WAS_DELETED, workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck(ORCHESTRATION_WORKFLOW, orchestrationWorkflow, USER);

    orchestrationWorkflow.setFailureStrategies(failureStrategies);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow, false).getOrchestrationWorkflow();
    return orchestrationWorkflow.getFailureStrategies();
  }

  @Override
  public List<Variable> updateUserVariables(String appId, String workflowId, List<Variable> userVariables) {
    if (isNotEmpty(userVariables)) {
      userVariables.forEach(variable -> ManagerExpressionEvaluator.isValidVariableName(variable.getName()));
    }
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("Workflow was deleted", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck(ORCHESTRATION_WORKFLOW, orchestrationWorkflow, USER);

    orchestrationWorkflow.setUserVariables(userVariables);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow, false).getOrchestrationWorkflow();
    return orchestrationWorkflow.getUserVariables();
  }

  private void validateWorkflowVariables(OrchestrationWorkflow orchestrationWorkflow) {
    if (orchestrationWorkflow == null || isEmpty(orchestrationWorkflow.getUserVariables())) {
      return;
    }
    Set<String> variableNames = new HashSet<>();
    List<Variable> userVariables = orchestrationWorkflow.getUserVariables();
    for (Variable variable : userVariables) {
      final String defaultValue = variable.getValue();
      if (variable.isFixed()) {
        if (isBlank(defaultValue)) {
          throw new InvalidRequestException(
              "Workflow Variable value is mandatory for Fixed Variable Name [" + variable.getName() + "]", USER);
        }
      }
      if (!variableNames.add(variable.getName())) {
        throw new InvalidRequestException(
            "Duplicate variable name [" + variable.getName() + "]. Duplicates are not allowed.", USER);
      }
      if (isNotEmpty(variable.getAllowedValues())) {
        variable.setAllowedList(CsvParser.parse(variable.getAllowedValues()));
        if (isNotEmpty(defaultValue)) {
          if (!variable.getAllowedList().contains(defaultValue)) {
            throw new InvalidRequestException(
                "Default value [" + defaultValue + " is not in Allowed Values" + variable.getAllowedList());
          }
        }
      } else {
        variable.setAllowedList(null);
      }
    }
  }

  @Override
  public DeploymentMetadata fetchDeploymentMetadata(String appId, Workflow workflow,
      Map<String, String> workflowVariables, List<String> artifactRequiredServiceIds, List<String> envIds,
      Include... includes) {
    return fetchDeploymentMetadata(
        appId, workflow, workflowVariables, artifactRequiredServiceIds, envIds, false, null, includes);
  }

  @Override
  public DeploymentMetadata fetchDeploymentMetadata(String appId, Workflow workflow,
      Map<String, String> workflowVariables, List<String> artifactRequiredServiceIds, List<String> envIds,
      boolean withDefaultArtifact, WorkflowExecution workflowExecution, Include... includes) {
    DeploymentMetadataBuilder deploymentMetadataBuilder = DeploymentMetadata.builder();

    List<Include> includeList = isEmpty(includes) ? Arrays.asList(Include.values()) : Arrays.asList(includes);

    if (includeList.contains(Include.ARTIFACT_SERVICE)) {
      if (artifactRequiredServiceIds == null) {
        artifactRequiredServiceIds = new ArrayList<>();
      }

      String accountId = appService.getAccountIdByAppId(appId);
      List<ArtifactVariable> artifactVariables = new ArrayList<>();
      if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        fetchArtifactNeededServiceIds(
            appId, workflow, workflowVariables, artifactRequiredServiceIds, artifactVariables);
      } else {
        fetchArtifactNeededServiceIds(appId, workflow, workflowVariables, artifactRequiredServiceIds);
        if (isNotEmpty(artifactRequiredServiceIds)) {
          for (String serviceId : artifactRequiredServiceIds) {
            artifactVariables.add(
                ArtifactVariable.builder()
                    .type(VariableType.ARTIFACT)
                    .name(DEFAULT_ARTIFACT_VARIABLE_NAME)
                    .entityType(SERVICE)
                    .entityId(serviceId)
                    .allowedList(artifactStreamServiceBindingService.listArtifactStreamIds(appId, serviceId))
                    .build());
          }
        }
      }

      // Update artifact variables with display info and artifact stream summaries.
      if (isNotEmpty(artifactVariables)) {
        deploymentMetadataBuilder.artifactVariables(artifactVariables);
        updateArtifactVariables(appId, workflow, artifactVariables, withDefaultArtifact, workflowExecution);
      }

      deploymentMetadataBuilder.artifactRequiredServiceIds(artifactRequiredServiceIds);
    }

    if (includeList.contains(Include.DEPLOYMENT_TYPE)) {
      final List<DeploymentType> deploymentTypes =
          workflowServiceHelper.obtainDeploymentTypes(workflow.getOrchestrationWorkflow());
      deploymentMetadataBuilder.deploymentTypes(deploymentTypes);
    }

    if (includeList.contains(Include.ENVIRONMENT)) {
      if (envIds == null) {
        envIds = new ArrayList<>();
      }
      final String resolvedEnvId = workflowServiceHelper.obtainTemplatedEnvironmentId(workflow, workflowVariables);
      if (resolvedEnvId != null && !envIds.contains(resolvedEnvId)) {
        envIds.add(resolvedEnvId);
      }
      deploymentMetadataBuilder.envIds(envIds);
    }

    return deploymentMetadataBuilder.build();
  }

  @Override
  public void updateArtifactVariables(String appId, Workflow workflow, List<ArtifactVariable> artifactVariables,
      boolean withDefaultArtifact, WorkflowExecution workflowExecution) {
    for (ArtifactVariable artifactVariable : artifactVariables) {
      artifactVariable.setDisplayInfo(getDisplayInfo(appId, workflow, artifactVariable));
      if (isEmpty(artifactVariable.getAllowedList())) {
        continue;
      }
      String accountId = appService.getAccountIdByAppId(appId);

      if (withDefaultArtifact && workflowExecution == null && artifactVariable.getAllowedList().size() == 1
          && featureFlagService.isEnabled(FeatureName.DEFAULT_ARTIFACT, accountId)) {
        // Set default artifact as last collected artifact.
        String artifactStreamId = artifactVariable.getAllowedList().get(0);
        ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
        ArtifactStreamSummaryBuilder artifactStreamSummaryBuilder =
            ArtifactStreamSummary.builder().artifactStreamId(artifactStreamId);
        if (artifactStream != null) {
          Artifact artifact = artifactService.fetchLastCollectedApprovedArtifactSorted(artifactStream);
          if (artifact != null) {
            artifactStreamSummaryBuilder.defaultArtifact(ArtifactSummary.prepareSummaryFromArtifact(artifact));
          }
        }
        artifactVariable.setArtifactStreamSummaries(singletonList(artifactStreamSummaryBuilder.build()));
        continue;
      }

      Artifact artifact = (withDefaultArtifact && workflowExecution != null)
          ? getArtifactVariableDefaultArtifact(artifactVariable, workflowExecution)
          : null;
      ArtifactSummary artifactSummary =
          (artifact != null) ? ArtifactSummary.prepareSummaryFromArtifact(artifact) : null;
      artifactVariable.setArtifactStreamSummaries(artifactVariable.getAllowedList()
                                                      .stream()
                                                      .map(artifactStreamId -> {
                                                        ArtifactStreamSummaryBuilder artifactStreamSummaryBuilder =
                                                            ArtifactStreamSummary.builder().artifactStreamId(
                                                                artifactStreamId);
                                                        if (artifactSummary != null) {
                                                          artifactStreamSummaryBuilder.defaultArtifact(artifactSummary);
                                                        }
                                                        return artifactStreamSummaryBuilder.build();
                                                      })
                                                      .collect(Collectors.toList()));
    }
  }

  @Override
  public Artifact getArtifactVariableDefaultArtifact(
      ArtifactVariable artifactVariable, WorkflowExecution workflowExecution) {
    if (isEmpty(artifactVariable.getAllowedList())) {
      // No artifact streams, so we cannot pre-fill it.
      return null;
    }

    Artifact artifact = getArtifactVariableDefaultArtifactHelper(artifactVariable, workflowExecution);
    if (artifact == null) {
      return null;
    }

    artifact = artifactService.get(artifact.getUuid());
    if (artifact == null || !artifactVariable.getAllowedList().contains(artifact.getArtifactStreamId())) {
      // Ensure artifact has not been deleted.
      return null;
    }
    return artifact;
  }

  private Artifact getArtifactVariableDefaultArtifactHelper(
      ArtifactVariable artifactVariable, WorkflowExecution workflowExecution) {
    List<ArtifactVariable> previousArtifactVariables = workflowExecution.getExecutionArgs().getArtifactVariables();
    if (isNotEmpty(previousArtifactVariables)) {
      // If artifact variables are present use them.
      ArtifactVariable foundArtifactVariable =
          previousArtifactVariables.stream()
              .filter(previousArtifactVariable
                  -> artifactVariable.getName().equals(previousArtifactVariable.getName())
                      && artifactVariable.getEntityType() == previousArtifactVariable.getEntityType()
                      && artifactVariable.getEntityId().equals(previousArtifactVariable.getEntityId()))
              .findFirst()
              .orElse(null);
      if (foundArtifactVariable == null) {
        return null;
      }

      String artifactId = foundArtifactVariable.getValue();
      if (isBlank(artifactId)) {
        return null;
      }

      List<Artifact> artifacts = workflowExecution.getExecutionArgs().getArtifacts();
      if (isEmpty(artifacts)) {
        return null;
      }

      return artifacts.stream().filter(artifact -> artifactId.equals(artifact.getUuid())).findFirst().orElse(null);
    }

    List<Artifact> previousArtifacts = workflowExecution.getExecutionArgs().getArtifacts();
    if (isEmpty(previousArtifacts) || isEmpty(artifactVariable.getAllowedList())) {
      return null;
    }

    return previousArtifacts.stream()
        .filter(artifact -> artifactVariable.getAllowedList().contains(artifact.getArtifactStreamId()))
        .findFirst()
        .orElse(null);
  }

  @Override
  public Map<String, List<String>> getDisplayInfo(String appId, Workflow workflow, ArtifactVariable artifactVariable) {
    Map<String, List<String>> displayInfo = new HashMap<>();
    Environment environment;
    Service service;
    switch (artifactVariable.getEntityType()) {
      case SERVICE:
        service = serviceResourceService.get(appId, artifactVariable.getEntityId());
        if (service == null) {
          return displayInfo;
        }
        updateDisplayInfoMap(displayInfo, "services", service.getName());
        break;
      case ENVIRONMENT:
        updateDisplayInfoForArtifactVariableOverrides(appId, workflow, artifactVariable, displayInfo);
        environment = environmentService.get(appId, artifactVariable.getEntityId());
        if (environment == null) {
          return displayInfo;
        }
        updateDisplayInfoMap(displayInfo, "environments", environment.getName());
        break;
      case WORKFLOW:
        updateDisplayInfoForArtifactVariableOverrides(appId, workflow, artifactVariable, displayInfo);
        if (workflow == null) {
          return displayInfo;
        }
        updateDisplayInfoMap(displayInfo, "workflows", workflow.getName());
        break;
      default:
        unhandled(artifactVariable.getEntityType());
    }

    return displayInfo;
  }

  private void updateDisplayInfoMap(Map<String, List<String>> displayInfo, String key, String value) {
    List<String> values = displayInfo.getOrDefault(key, null);
    if (values == null) {
      values = new ArrayList<>();
      displayInfo.put(key, values);
    }

    if (!values.contains(value)) {
      values.add(value);
    }
  }

  private void mergeDisplayInfoMaps(Map<String, List<String>> displayInfo, Map<String, List<String>> newDisplayInfo) {
    for (Entry<String, List<String>> entry : newDisplayInfo.entrySet()) {
      List<String> newValues = entry.getValue();
      if (!displayInfo.containsKey(entry.getKey())) {
        displayInfo.put(entry.getKey(), newValues);
        continue;
      }

      List<String> values = displayInfo.get(entry.getKey());
      for (String value : newValues) {
        if (!values.contains(value)) {
          values.add(value);
        }
      }
    }
  }

  private void updateDisplayInfoForArtifactVariableOverrides(
      String appId, Workflow workflow, ArtifactVariable artifactVariable, Map<String, List<String>> displayInfo) {
    if (isNotEmpty(artifactVariable.getOverriddenArtifactVariables())) {
      List<ArtifactVariable> overriddenArtifactVariables = artifactVariable.getOverriddenArtifactVariables();
      overriddenArtifactVariables.forEach(
          artifactVariable1 -> mergeDisplayInfoMaps(displayInfo, getDisplayInfo(appId, workflow, artifactVariable1)));
    }
  }

  @Override
  public Set<EntityType> fetchRequiredEntityTypes(String appId, Workflow workflow) {
    List<String> artifactNeededServiceIds = new ArrayList<>();
    Set<EntityType> requiredEntityTypes = new HashSet<>();
    fetchArtifactNeededServiceIds(appId, workflow, null, artifactNeededServiceIds);
    if (isNotEmpty(artifactNeededServiceIds)) {
      // At least one service needs artifact..so add required entity type as ARTIFACT
      requiredEntityTypes.add(ARTIFACT);
    }
    return requiredEntityTypes;
  }

  private void fetchArtifactNeededServiceIds(
      String appId, Workflow workflow, Map<String, String> workflowVariables, List<String> artifactNeededServiceIds) {
    notNullCheck("Workflow does not exist", workflow, USER);
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    notNullCheck("Orchestration workflow not associated", orchestrationWorkflow, USER);

    if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      Set<EntityType> requiredEntityTypes = new HashSet<>();
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
      boolean infraRefactor =
          featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(appId));

      // Service cache.
      Map<String, Service> serviceCache = new HashMap<>();

      updateRequiredEntityTypes(appId, null, canaryOrchestrationWorkflow.getPreDeploymentSteps(), requiredEntityTypes,
          null, null, serviceCache, infraRefactor);

      boolean preDeploymentStepNeededArtifact = requiredEntityTypes.contains(EntityType.ARTIFACT);

      Set<EntityType> phaseRequiredEntityTypes =
          canaryOrchestrationWorkflow.getWorkflowPhases()
              .stream()
              .flatMap(phase
                  -> updateRequiredEntityTypes(appId, phase, workflowVariables, artifactNeededServiceIds, serviceCache,
                      preDeploymentStepNeededArtifact, infraRefactor)
                         .stream())
              .collect(Collectors.toSet());

      requiredEntityTypes.addAll(phaseRequiredEntityTypes);

      Set<EntityType> rollbackRequiredEntityTypes =
          ((CanaryOrchestrationWorkflow) orchestrationWorkflow)
              .getRollbackWorkflowPhaseIdMap()
              .values()
              .stream()
              .flatMap(phase
                  -> updateRequiredEntityTypes(appId, phase, workflowVariables, artifactNeededServiceIds, serviceCache,
                      preDeploymentStepNeededArtifact, infraRefactor)
                         .stream())
              .collect(Collectors.toSet());

      if (!requiredEntityTypes.contains(ARTIFACT) && rollbackRequiredEntityTypes.contains(ARTIFACT)) {
        logger.warn(
            "Phase Step do not need artifact. However, Rollback steps needed artifact for the workflow: [{}] of the app: [{}]",
            workflow.getUuid(), appId);
      }
      requiredEntityTypes.addAll(rollbackRequiredEntityTypes);
    }
  }

  private Set<EntityType> updateRequiredEntityTypes(String appId, WorkflowPhase workflowPhase,
      Map<String, String> workflowVaraibles, List<String> artifactNeededServiceIds, Map<String, Service> serviceCache,
      boolean preDeploymentStepNeededArtifact, boolean infraRefactor) {
    Set<EntityType> requiredEntityTypes = new HashSet<>();

    if (workflowPhase == null || workflowPhase.getPhaseSteps() == null) {
      return requiredEntityTypes;
    }

    String serviceId = null;
    if (workflowPhase.checkServiceTemplatized()) {
      String serviceTemplatizedName = workflowPhase.fetchServiceTemplatizedName();
      if (serviceTemplatizedName != null) {
        serviceId = isEmpty(workflowVaraibles) ? null : workflowVaraibles.get(serviceTemplatizedName);
      }
    } else {
      serviceId = workflowPhase.getServiceId();
    }

    if (serviceId != null) {
      if (artifactNeededServiceIds.contains(serviceId)) {
        requiredEntityTypes.add(EntityType.ARTIFACT);
        return requiredEntityTypes;
      }
      if (matchesVariablePattern(serviceId)) {
        return requiredEntityTypes;
      }
      if (preDeploymentStepNeededArtifact) {
        if (!artifactNeededServiceIds.contains(serviceId)) {
          artifactNeededServiceIds.add(serviceId);
        }
        return requiredEntityTypes;
      }
    }

    for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
      if (phaseStep.getSteps() == null) {
        continue;
      }
      if (requiredEntityTypes.contains(EntityType.ARTIFACT)) {
        // Check if service already included. Then, no need to go over service and steps
        if (artifactNeededServiceIds.contains(serviceId)) {
          return requiredEntityTypes;
        }
      }
      updateRequiredEntityTypes(appId, serviceId, phaseStep, requiredEntityTypes, workflowPhase, workflowVaraibles,
          serviceCache, infraRefactor);
    }

    if (requiredEntityTypes.contains(EntityType.ARTIFACT)) {
      if (serviceId != null && !artifactNeededServiceIds.contains(serviceId)) {
        artifactNeededServiceIds.add(serviceId);
      }
    }

    return requiredEntityTypes;
  }

  private void updateRequiredEntityTypes(String appId, String serviceId, PhaseStep phaseStep,
      Set<EntityType> requiredEntityTypes, WorkflowPhase workflowPhase, Map<String, String> workflowVariables,
      Map<String, Service> serviceCache, boolean infraRefactor) {
    boolean artifactNeeded = false;
    for (GraphNode step : phaseStep.getSteps()) {
      if (step.getTemplateUuid() != null) {
        if (isNotEmpty(step.getTemplateVariables())) {
          List<String> values = step.getTemplateVariables()
                                    .stream()
                                    .filter(variable -> isNotEmpty(variable.getValue()))
                                    .map(Variable::getValue)
                                    .collect(toList());
          if (isNotEmpty(values)) {
            if (isArtifactNeeded(values.toArray())) {
              artifactNeeded = true;
              break;
            }
          }
        }
      }

      if (COMMAND.name().equals(step.getType())) {
        if (step.getTemplateUuid() != null) {
          Command command = (Command) templateService.constructEntityFromTemplate(
              step.getTemplateUuid(), step.getTemplateVersion(), EntityType.COMMAND);
          if (command != null && command.isArtifactNeeded()) {
            artifactNeeded = true;
            break;
          }
        }
        if (serviceId != null && !matchesVariablePattern(serviceId)) {
          Service service = serviceCache.getOrDefault(serviceId, null);
          if (service == null) {
            service = serviceResourceService.getServiceWithServiceCommands(appId, serviceId, false);
            if (service != null) {
              serviceCache.put(serviceId, service);
            }
          }
          if (service != null) {
            ServiceCommand serviceCommand =
                service.getServiceCommands()
                    .stream()
                    .filter(command
                        -> equalsIgnoreCase((String) step.getProperties().get("commandName"), command.getName()))
                    .findFirst()
                    .orElse(null);
            if (serviceCommand != null && serviceCommand.getCommand() != null
                && serviceCommand.getCommand().isArtifactNeeded()) {
              artifactNeeded = true;
              break;
            }
          }
        }
      } else if (HTTP.name().equals(step.getType())
          && (isArtifactNeeded(step.getProperties().get("url"), step.getProperties().get("body"),
                 step.getProperties().get("assertion")))) {
        artifactNeeded = true;
        break;
      } else if (SHELL_SCRIPT.name().equals(step.getType())
          && (isArtifactNeeded(step.getProperties().get("scriptString")))) {
        artifactNeeded = true;
        break;
      } else if (CLOUD_FORMATION_CREATE_STACK.name().equals(step.getType())) {
        List<Map> variables = (List<Map>) step.getProperties().get("variables");
        if (variables != null) {
          List<String> values = (List<String>) variables.stream()
                                    .flatMap(element -> element.values().stream())
                                    .collect(Collectors.toList());
          if (isArtifactNeeded(values.toArray())) {
            artifactNeeded = true;
          }
        }
      } else if (kubernetesArtifactNeededStateTypes.contains(step.getType())
          || ecsArtifactNeededStateTypes.contains(step.getType())
          || amiArtifactNeededStateTypes.contains(step.getType())
          || codeDeployArtifactNeededStateTypes.contains(step.getType())
          || awsLambdaArtifactNeededStateTypes.contains(step.getType())
          || pcfArtifactNeededStateTypes.contains(step.getType())) {
        // NOTE: If you add new State Type that needs artifact.. it should be listed down here
        artifactNeeded = true;
        break;
      } else if (workflowPhase != null && HELM == workflowPhase.getDeploymentType()
          && StateType.HELM_DEPLOY.name().equals(step.getType())) {
        if (infraRefactor) {
          String infraDefinitionId = getInfraDefinitionId(workflowPhase, workflowVariables);
          if (isNotEmpty(infraDefinitionId) && !matchesVariablePattern(infraDefinitionId)) {
            InfrastructureDefinition infrastructureDefinition =
                infrastructureDefinitionService.get(appId, infraDefinitionId);
            if (infrastructureDefinition != null) {
              ServiceTemplate serviceTemplate =
                  serviceTemplateService.get(appId, serviceId, infrastructureDefinition.getEnvId());
              if (serviceTemplate != null
                  && serviceResourceService.checkArtifactNeededForHelm(appId, serviceTemplate.getUuid())) {
                artifactNeeded = true;
                break;
              }
            }
          }
        } else {
          String infraMappingId = getInfraMappingId(workflowPhase, workflowVariables);
          if (isNotEmpty(infraMappingId) && !matchesVariablePattern(infraMappingId)) {
            InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
            if (infrastructureMapping != null) {
              if (serviceResourceService.checkArtifactNeededForHelm(
                      appId, infrastructureMapping.getServiceTemplateId())) {
                artifactNeeded = true;
                break;
              }
            }
          }
        }
      } else if (workflowPhase != null && k8sV2ArtifactNeededStateTypes.contains(step.getType())) {
        if (artifactStreamService.artifactStreamsExistForService(appId, serviceId)) {
          artifactNeeded = true;
          break;
        }

        if (infraRefactor) {
          String infraDefinitionId = getInfraDefinitionId(workflowPhase, workflowVariables);
          if (isNotEmpty(infraDefinitionId) && !matchesVariablePattern(infraDefinitionId)
              && k8sStateHelper.doManifestsUseArtifact(appId, serviceId, infraDefinitionId)) {
            artifactNeeded = true;
            break;
          }
        } else {
          String infraMappingId = getInfraMappingId(workflowPhase, workflowVariables);
          if (isNotEmpty(infraMappingId) && !matchesVariablePattern(infraMappingId)
              && k8sStateHelper.doManifestsUseArtifact(appId, infraMappingId)) {
            artifactNeeded = true;
            break;
          }
        }
      }
    }

    if (artifactNeeded) {
      requiredEntityTypes.add(ARTIFACT);
      phaseStep.setArtifactNeeded(true);
    }
  }

  private void fetchArtifactNeededServiceIds(String appId, Workflow workflow, Map<String, String> workflowVariablesMap,
      List<String> artifactNeededServiceIds, List<ArtifactVariable> artifactVariables) {
    notNullCheck("Workflow does not exist", workflow, USER);
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    notNullCheck("Orchestration workflow not associated", orchestrationWorkflow, USER);

    if (!(orchestrationWorkflow instanceof CanaryOrchestrationWorkflow)) {
      // Early return if not a CanaryOrchestrationWorkflow.
      return;
    }

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    boolean isBuildWorkflow = BUILD == canaryOrchestrationWorkflow.getOrchestrationWorkflowType();
    boolean infraRefactor = featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(appId));

    // Service cache.
    Map<String, Service> serviceCache = new HashMap<>();

    // Map of serviceId to artifact variables used in the workflow.
    Map<String, Set<String>> serviceArtifactVariableNamesMap = new HashMap<>();

    // Process PreDeploymentSteps.
    updateArtifactVariableNames(appId, "", canaryOrchestrationWorkflow.getPreDeploymentSteps(), null, null,
        serviceArtifactVariableNamesMap, serviceCache, infraRefactor);

    // Process WorkflowPhases.
    for (WorkflowPhase phase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
      updateArtifactVariableNames(
          appId, phase, workflowVariablesMap, serviceArtifactVariableNamesMap, serviceCache, infraRefactor);
    }

    // Process PostDeploymentSteps.
    updateArtifactVariableNames(appId, "", canaryOrchestrationWorkflow.getPostDeploymentSteps(), null, null,
        serviceArtifactVariableNamesMap, serviceCache, infraRefactor);

    boolean requiresArtifact = serviceArtifactVariableNamesMap.size() > 0;

    // Process RollbackWorkflowPhases.
    for (WorkflowPhase phase : canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().values()) {
      updateArtifactVariableNames(
          appId, phase, workflowVariablesMap, serviceArtifactVariableNamesMap, serviceCache, infraRefactor);
    }

    if (!requiresArtifact && serviceArtifactVariableNamesMap.size() > 0) {
      logger.warn(
          "Phase Steps do not need artifact. However, Rollback steps needed artifact for the workflow: [{}] of the app: [{}]",
          workflow.getUuid(), appId);
    }

    // Set of workflow artifact variables used in the workflow.
    Set<String> workflowVariableNames = new HashSet<>();
    if (serviceArtifactVariableNamesMap.containsKey("")) {
      Set<String> implicitWorkflowVariableNames = serviceArtifactVariableNamesMap.get("");
      if (isNotEmpty(implicitWorkflowVariableNames)) {
        // These artifact variables weren't defined in the context of any service, hence, they must be as workflow
        // variables.
        workflowVariableNames.addAll(implicitWorkflowVariableNames);
      }
    }

    // List of variables defined at the workflow variable with type ARTIFACT.
    List<Variable> workflowVariables = canaryOrchestrationWorkflow.getUserVariables();
    Map<String, Variable> workflowArtifactVariableNamesMap =
        workflowVariables.stream()
            .filter(variable -> variable.getType() == VariableType.ARTIFACT)
            .collect(Collectors.toMap(Variable::getName, Function.identity()));

    // Filter out invalid workflow artifact variable names which are used while deployment/execution, but it are not
    // defined for the workflow. This will lead to runtime error, so instead we are throwing an error while fetching
    // deployment metadata.
    Set<String> invalidWorkflowVariableNames =
        workflowVariableNames.stream()
            .filter(variableName -> !workflowArtifactVariableNamesMap.containsKey(variableName))
            .collect(Collectors.toSet());
    if (invalidWorkflowVariableNames.size() > 0) {
      // Throw an exception if invalid workflow artifact variable names were found.
      throw new InvalidRequestException(
          format(
              "Artifact variables [%s] are used but not defined for the workflow [%s]. Please ensure these variable names are defined for this workflow.",
              String.join(",", invalidWorkflowVariableNames), workflow.getName()),
          USER);
    }

    serviceArtifactVariableNamesMap = serviceArtifactVariableNamesMap.entrySet()
                                          .stream()
                                          .filter(entry -> isNotEmpty(entry.getKey()) && isNotEmpty(entry.getValue()))
                                          .collect(toMap(Entry::getKey, Entry::getValue));

    // NOTE: Current overriding behaviour order:
    // 1. ArtifactVariables defined as ServiceVariables at Service level - with allowed list = artifactStreamIds
    // 2. ServiceVariable overrides for all services defined at Environment level - with allowed list =
    //    artifactStreamIds
    // 3. ServiceVariable overrides for service templates defined at Environment level - with allowed list =
    //    artifactStreamIds
    // 4. Workflow Variables defined at Workflow level with type ARTIFACT and same name as one of the above
    //    ServiceVariable - with allowed list = artifactStreamIds

    // Compute artifact variables associated to a service with overrides at env level
    // overriding behaviours 1, 2 and 3 covered here.
    Map<String, List<ServiceVariable>> serviceVariablesMap =
        computeServiceVariables(appId, workflow.getEnvId(), serviceArtifactVariableNamesMap.keySet());

    // Loop over serviceIds and add artifact variables used based on the above computed service variables.
    if (!isBuildWorkflow) {
      for (Entry<String, Set<String>> entry : serviceArtifactVariableNamesMap.entrySet()) {
        String serviceId = entry.getKey();
        Set<String> serviceArtifactVariableNames = entry.getValue();

        List<ServiceVariable> serviceVariables = serviceVariablesMap.getOrDefault(serviceId, null);
        if (serviceVariables == null) {
          continue;
        }

        Map<String, ServiceVariable> variableNames =
            serviceVariables.stream().collect(Collectors.toMap(ServiceVariable::getName, Function.identity()));
        int count = 0;
        Set<String> invalidVariableNames = new HashSet<>();
        for (String variableName : serviceArtifactVariableNames) {
          if (!variableNames.containsKey(variableName)) {
            if (workflowArtifactVariableNamesMap.containsKey(variableName)) {
              continue;
            }

            // Artifact variable name `variableName` is used while deployment/execution, but it is not defined for the
            // service. This will lead to runtime error, so instead we are throwing an error while fetching deployment
            // metadata. We collect all the invalid variable names for this service and throw a single error at the end.
            invalidVariableNames.add(variableName);
            continue;
          }
          if (isNotEmpty(invalidVariableNames)) {
            // If we have invalid variable names, we will just throw at the end of the for loop. No need to process
            // further.
            continue;
          }

          ServiceVariable serviceVariable = variableNames.get(variableName);
          List<ArtifactVariable> overriddenArtifactVariables = new ArrayList<>();
          if (serviceVariable.getOverriddenServiceVariable() != null
              && serviceVariable.getEntityType() != EntityType.ENVIRONMENT) {
            ServiceVariable overriddenServiceVariable = serviceVariable.getOverriddenServiceVariable();
            overriddenArtifactVariables.add(ArtifactVariable.builder()
                                                .type(VariableType.ARTIFACT)
                                                .name(variableName)
                                                .entityType(overriddenServiceVariable.getEntityType())
                                                .entityId(overriddenServiceVariable.getEntityId())
                                                .allowedList(overriddenServiceVariable.getAllowedList())
                                                .build());
          }

          EntityType entityType = serviceVariable.getEntityType();
          String entityId = serviceVariable.getEntityId();
          if (entityType == EntityType.SERVICE_TEMPLATE) {
            entityType = EntityType.ENVIRONMENT;
            entityId = serviceVariable.getEnvId();
          }

          ArtifactVariable artifactVariable = ArtifactVariable.builder()
                                                  .type(VariableType.ARTIFACT)
                                                  .name(variableName)
                                                  .entityType(entityType)
                                                  .entityId(entityId)
                                                  .allowedList(serviceVariable.getAllowedList())
                                                  .overriddenArtifactVariables(overriddenArtifactVariables)
                                                  .build();

          artifactVariables.add(artifactVariable);
          count++;
        }

        if (isNotEmpty(invalidVariableNames)) {
          // Throw an exception if invalid artifact variable names were found for the current service.
          Service service = serviceResourceService.get(serviceId);
          notNullCheck("Service does not exist: " + serviceId, service, USER);
          throw new InvalidRequestException(
              format(
                  "Artifact variables [%s] are used but not defined for the service [%s]. Please ensure these variable names are defined for this service.",
                  String.join(",", invalidVariableNames), service.getName()),
              USER);
        }

        if (count > 0) {
          artifactNeededServiceIds.add(serviceId);
        }
      }
    }

    // Copy over all the service variables as well as they might have been overridden by workflow variables
    // overriding behaviour 4 (defined above in the NOTE) covered here.
    workflowVariableNames.addAll(serviceArtifactVariableNamesMap.values()
                                     .stream()
                                     .flatMap(Set::stream)
                                     .filter(workflowArtifactVariableNamesMap::containsKey)
                                     .collect(Collectors.toSet()));

    String workflowId = workflow.getUuid();
    for (String variableName : workflowVariableNames) {
      Variable variable = workflowArtifactVariableNamesMap.get(variableName);

      // Override artifact variables with the same name and store them in the new artifact variable
      // overriding behaviour 4 (defined above in the NOTE) covered here.
      List<ArtifactVariable> overriddenArtifactVariables =
          artifactVariables.stream()
              .filter(artifactVariable -> artifactVariable.getName().equals(variableName))
              .collect(Collectors.toList());
      if (isNotEmpty(overriddenArtifactVariables)) {
        artifactVariables.removeIf(artifactVariable -> artifactVariable.getName().equals(variableName));
      }

      artifactVariables.add(ArtifactVariable.builder()
                                .type(VariableType.ARTIFACT)
                                .name(variableName)
                                .entityType(WORKFLOW)
                                .entityId(workflowId)
                                .allowedList(variable.getAllowedList())
                                .overriddenArtifactVariables(overriddenArtifactVariables)
                                .build());
    }
  }

  public Map<String, List<ServiceVariable>> computeServiceVariables(
      String appId, String envId, Set<String> serviceIds) {
    if (isEmpty(serviceIds)) {
      return new HashMap<>();
    }

    List<ServiceVariable> allServiceVariables =
        serviceVariableService.getServiceVariablesForEntity(appId, envId, OBTAIN_VALUE);
    if (allServiceVariables == null) {
      allServiceVariables = new ArrayList<>();
    }

    // serviceTemplates with list of service variables defined at service level and service template overrides defined
    // at the env level.
    List<ServiceTemplate> serviceTemplates =
        serviceTemplateService.list(aPageRequest()
                                        .addFilter(ServiceTemplateKeys.appId, EQ, appId)
                                        .addFilter(ServiceTemplateKeys.envId, EQ, envId)
                                        .addFilter(ServiceTemplateKeys.serviceId, IN, serviceIds.toArray())
                                        .build(),
            true, OBTAIN_VALUE);

    Map<String, ServiceTemplate> serviceTemplateMap = new HashMap<>();
    for (ServiceTemplate serviceTemplate : serviceTemplates) {
      serviceTemplateMap.put(serviceTemplate.getServiceId(), serviceTemplate);
    }

    Map<String, List<ServiceVariable>> serviceVariablesMap = new HashMap<>();
    for (String serviceId : serviceIds) {
      List<ServiceVariable> serviceVariables = null;
      List<ServiceVariable> templateServiceVariables = null;
      ServiceTemplate serviceTemplate = serviceTemplateMap.getOrDefault(serviceId, null);
      if (serviceTemplate != null) {
        serviceVariables = serviceTemplate.getServiceVariables();
        templateServiceVariables = serviceTemplate.getServiceVariablesOverrides();
      }

      if (serviceVariables == null) {
        serviceVariables = new ArrayList<>();
      }
      if (templateServiceVariables == null) {
        templateServiceVariables = new ArrayList<>();
      }

      serviceVariablesMap.put(serviceId,
          overrideServiceVariables(
              overrideServiceVariables(serviceVariables, allServiceVariables), templateServiceVariables));
    }

    return serviceVariablesMap;
  }

  private List<ServiceVariable> overrideServiceVariables(
      List<ServiceVariable> existingServiceVariables, List<ServiceVariable> newServiceVariables) {
    // Append newServiceVariables to existingServiceVariables overwriting any variables with same names.
    if (existingServiceVariables == null) {
      existingServiceVariables = new ArrayList<>();
    }
    List<ServiceVariable> mergedServiceVariables = existingServiceVariables;
    if (isNotEmpty(newServiceVariables)) {
      mergedServiceVariables = concat(newServiceVariables.stream(), existingServiceVariables.stream())
                                   .filter(new TreeSet<>(comparing(ServiceVariable::getName))::add)
                                   .collect(toList());
    }

    return mergedServiceVariables;
  }

  private void updateArtifactVariableNames(String appId, WorkflowPhase workflowPhase,
      Map<String, String> workflowVariablesMap, Map<String, Set<String>> serviceArtifactVariableNamesMap,
      Map<String, Service> serviceCache, boolean infraRefactor) {
    if (workflowPhase == null || workflowPhase.getPhaseSteps() == null) {
      return;
    }

    String serviceId = null;
    if (workflowPhase.checkServiceTemplatized()) {
      String serviceTemplatizedName = workflowPhase.fetchServiceTemplatizedName();
      if (serviceTemplatizedName != null) {
        serviceId = isEmpty(workflowVariablesMap) ? null : workflowVariablesMap.get(serviceTemplatizedName);
        if (isBlank(serviceId)) {
          return;
        }
      }
    } else {
      serviceId = workflowPhase.getServiceId();
    }

    if (serviceId == null) {
      serviceId = "";
    } else if (matchesVariablePattern(serviceId)) {
      return;
    }

    for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
      if (phaseStep.getSteps() == null) {
        continue;
      }

      // TODO: ASR: Add optimizations if multi-artifact not supported for the current serviceId.
      updateArtifactVariableNames(appId, serviceId, phaseStep, workflowPhase, workflowVariablesMap,
          serviceArtifactVariableNamesMap, serviceCache, infraRefactor);
    }
  }

  private void updateArtifactVariableNames(String appId, String serviceId, PhaseStep phaseStep,
      WorkflowPhase workflowPhase, Map<String, String> workflowVariables,
      Map<String, Set<String>> serviceArtifactVariableNamesMap, Map<String, Service> serviceCache,
      boolean infraRefactor) {
    // NOTE: If serviceId is "", we assume those artifact variables to be workflow variables.
    // NOTE: Here, serviceId should not be an expression.
    Set<String> serviceArtifactVariableNames;
    if (serviceArtifactVariableNamesMap.containsKey(serviceId)) {
      serviceArtifactVariableNames = serviceArtifactVariableNamesMap.get(serviceId);
    } else {
      serviceArtifactVariableNames = new HashSet<>();
      serviceArtifactVariableNamesMap.put(serviceId, serviceArtifactVariableNames);
    }

    for (GraphNode step : phaseStep.getSteps()) {
      if (step.getTemplateUuid() != null) {
        if (isNotEmpty(step.getTemplateVariables())) {
          List<String> values = step.getTemplateVariables()
                                    .stream()
                                    .filter(variable -> isNotEmpty(variable.getValue()))
                                    .map(Variable::getValue)
                                    .collect(toList());
          if (isNotEmpty(values)) {
            updateArtifactVariablesNeededForTemplate(serviceArtifactVariableNames, values.toArray());
          }
        }
      }

      if (COMMAND.name().equals(step.getType())) {
        if (step.getTemplateUuid() != null) {
          Command command = (Command) templateService.constructEntityFromTemplate(
              step.getTemplateUuid(), step.getTemplateVersion(), EntityType.COMMAND);
          if (command != null) {
            command.updateServiceArtifactVariableNames(serviceArtifactVariableNames);
          }
        }
        if (serviceId != null && !matchesVariablePattern(serviceId)) {
          Service service = serviceCache.getOrDefault(serviceId, null);
          if (service == null) {
            service = serviceResourceService.getServiceWithServiceCommands(appId, serviceId, false);
            if (service != null) {
              serviceCache.put(serviceId, service);
            }
          }
          if (service != null) {
            ServiceCommand serviceCommand =
                service.getServiceCommands()
                    .stream()
                    .filter(command
                        -> equalsIgnoreCase((String) step.getProperties().get("commandName"), command.getName()))
                    .findFirst()
                    .orElse(null);
            if (serviceCommand != null && serviceCommand.getCommand() != null) {
              serviceCommand.getCommand().updateServiceArtifactVariableNames(serviceArtifactVariableNames);
            }
          }
        }
      } else if (HTTP.name().equals(step.getType())) {
        updateArtifactVariablesNeeded(serviceArtifactVariableNames, step.getProperties().get("url"),
            step.getProperties().get("body"), step.getProperties().get("assertion"));
      } else if (SHELL_SCRIPT.name().equals(step.getType())) {
        updateArtifactVariablesNeeded(serviceArtifactVariableNames, step.getProperties().get("scriptString"));
      } else if (CLOUD_FORMATION_CREATE_STACK.name().equals(step.getType())) {
        List<Map> variables = (List<Map>) step.getProperties().get("variables");
        if (variables != null) {
          List<String> values = (List<String>) variables.stream()
                                    .flatMap(element -> element.values().stream())
                                    .collect(Collectors.toList());
          updateArtifactVariablesNeeded(serviceArtifactVariableNames, values.toArray());
        }
      } else if (kubernetesArtifactNeededStateTypes.contains(step.getType())
          || ecsArtifactNeededStateTypes.contains(step.getType())
          || amiArtifactNeededStateTypes.contains(step.getType())
          || codeDeployArtifactNeededStateTypes.contains(step.getType())
          || awsLambdaArtifactNeededStateTypes.contains(step.getType())
          || pcfArtifactNeededStateTypes.contains(step.getType())) {
        updateDefaultArtifactVariablesNeeded(serviceArtifactVariableNames);
      } else if (workflowPhase != null && HELM == workflowPhase.getDeploymentType()
          && StateType.HELM_DEPLOY.name().equals(step.getType())) {
        if (infraRefactor) {
          String infraDefinitionId = getInfraDefinitionId(workflowPhase, workflowVariables);
          if (isNotEmpty(infraDefinitionId) && !matchesVariablePattern(infraDefinitionId)) {
            InfrastructureDefinition infraDefiniton = infrastructureDefinitionService.get(appId, infraDefinitionId);
            ServiceTemplate serviceTemplate = serviceTemplateService.get(appId, serviceId, infraDefiniton.getEnvId());
            if (serviceTemplate != null) {
              serviceResourceService.updateArtifactVariableNamesForHelm(
                  appId, serviceTemplate.getUuid(), serviceArtifactVariableNames);
            }
          }
        } else {
          String infraMappingId = getInfraMappingId(workflowPhase, workflowVariables);
          if (isNotEmpty(infraMappingId) && !matchesVariablePattern(infraMappingId)) {
            InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
            if (infrastructureMapping != null) {
              serviceResourceService.updateArtifactVariableNamesForHelm(
                  appId, infrastructureMapping.getServiceTemplateId(), serviceArtifactVariableNames);
            }
          }
        }

      } else if (workflowPhase != null && k8sV2ArtifactNeededStateTypes.contains(step.getType())) {
        if (infraRefactor) {
          String infraDefinitionId = getInfraDefinitionId(workflowPhase, workflowVariables);
          if (isNotEmpty(infraDefinitionId) && !matchesVariablePattern(infraDefinitionId)) {
            k8sStateHelper.updateManifestsArtifactVariableNamesInfraDefinition(
                appId, infraDefinitionId, serviceArtifactVariableNames, serviceId);
          }
        } else {
          String infraMappingId = getInfraMappingId(workflowPhase, workflowVariables);
          if (isNotEmpty(infraMappingId) && !matchesVariablePattern(infraMappingId)) {
            k8sStateHelper.updateManifestsArtifactVariableNames(appId, infraMappingId, serviceArtifactVariableNames);
          }
        }
      }
    }
  }

  private String getInfraMappingId(WorkflowPhase workflowPhase, Map<String, String> workflowVariables) {
    String infraMappingId = null;
    if (workflowPhase.checkInfraTemplatized()) {
      String infraTemplatizedName = workflowPhase.fetchInfraMappingTemplatizedName();
      if (infraTemplatizedName != null) {
        infraMappingId = isEmpty(workflowVariables) ? null : workflowVariables.get(infraTemplatizedName);
      }
    } else {
      infraMappingId = workflowPhase.getInfraMappingId();
    }
    return infraMappingId;
  }

  private String getInfraDefinitionId(WorkflowPhase workflowPhase, Map<String, String> workflowVariables) {
    String infraDefinitionId = null;
    if (workflowPhase.checkInfraDefinitionTemplatized()) {
      String infraDefinitionTemplatizedName = workflowPhase.fetchInfraDefinitionTemplatizedName();
      if (infraDefinitionTemplatizedName != null) {
        infraDefinitionId = isEmpty(workflowVariables) ? null : workflowVariables.get(infraDefinitionTemplatizedName);
      }
    } else {
      infraDefinitionId = workflowPhase.getInfraDefinitionId();
    }
    return infraDefinitionId;
  }

  private boolean isArtifactNeeded(Object... args) {
    return Arrays.stream(args).anyMatch(arg
        -> arg != null && (((String) arg).contains("${artifact.") || ((String) arg).contains("${ARTIFACT_FILE_NAME}")));
  }

  private void updateArtifactVariablesNeeded(Set<String> serviceArtifactVariableNames, Object... args) {
    for (Object arg : args) {
      if (!(arg instanceof String)) {
        continue;
      }

      String str = (String) arg;
      ExpressionEvaluator.updateServiceArtifactVariableNames(str, serviceArtifactVariableNames);
    }
  }

  private void updateArtifactVariablesNeededForTemplate(Set<String> serviceArtifactVariableNames, Object... args) {
    for (Object arg : args) {
      if (!(arg instanceof String)) {
        continue;
      }

      String str = (String) arg;
      ExpressionEvaluator.updateServiceArtifactVariableNames(str, serviceArtifactVariableNames);
    }
  }

  private void updateDefaultArtifactVariablesNeeded(Set<String> serviceArtifactVariableNames) {
    serviceArtifactVariableNames.add(ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME);
  }

  private void createDefaultNotificationRule(Workflow workflow) {
    Application app = appService.get(workflow.getAppId());
    Account account = accountService.get(app.getAccountId());
    UserGroup userGroup = userGroupService.getDefaultUserGroup(app.getAccountId());

    if (null == userGroup) {
      logger.warn(
          "Default user group not created for account {}. Ignoring adding user group", account.getAccountName());
      return;
    }

    List<ExecutionStatus> conditions = asList(ExecutionStatus.FAILED);
    NotificationRule notificationRule = aNotificationRule()
                                            .withConditions(conditions)
                                            .withExecutionScope(ExecutionScope.WORKFLOW)
                                            .withUserGroupIds(Collections.singletonList(userGroup.getUuid()))
                                            .build();

    List<NotificationRule> notificationRules = asList(notificationRule);
    workflow.getOrchestrationWorkflow().setNotificationRules(notificationRules);
  }

  private void createDefaultFailureStrategy(Workflow workflow) {
    List<FailureStrategy> failureStrategies = new ArrayList<>();
    failureStrategies.add(FailureStrategy.builder()
                              .failureTypes(asList(FailureType.APPLICATION_ERROR))
                              .executionScope(ExecutionScope.WORKFLOW)
                              .repairActionCode(RepairActionCode.ROLLBACK_WORKFLOW)
                              .build());
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    switch (orchestrationWorkflow.getOrchestrationWorkflowType()) {
      case BASIC:
      case ROLLING:
      case BLUE_GREEN:
      case CANARY:
      case MULTI_SERVICE:
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
        canaryOrchestrationWorkflow.setFailureStrategies(failureStrategies);
        break;
      default:
        noop();
    }
  }

  private void validateOrchestrationWorkflow(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();

    if (orchestrationWorkflow == null) {
      return;
    }

    if (orchestrationWorkflow.getOrchestrationWorkflowType() == BASIC
        || orchestrationWorkflow.getOrchestrationWorkflowType() == ROLLING
        || orchestrationWorkflow.getOrchestrationWorkflowType() == BLUE_GREEN) {
      if (!orchestrationWorkflow.isServiceTemplatized()) {
        notNullCheck("Invalid serviceId", workflow.getServiceId(), USER);
      }

      if (orchestrationWorkflow.isInfraMappingTemplatized() || orchestrationWorkflow.isInfraDefinitionTemplatized()) {
        return;
      }
      InfrastructureMapping infrastructureMapping;
      if (!featureFlagService.isEnabled(
              FeatureName.INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(workflow.getAppId()))) {
        notNullCheck("Invalid inframappingId", workflow.getInfraMappingId(), USER);

        String infraMappingId = workflow.getInfraMappingId();
        infrastructureMapping = infrastructureMappingService.get(workflow.getAppId(), infraMappingId);

        notNullCheck("Invalid inframapping", infrastructureMapping, USER);

        validateInfraMappingWithService(workflow, orchestrationWorkflow, infrastructureMapping);
      } else {
        notNullCheck("Invalid InfraDefinition Id", workflow.getInfraDefinitionId(), USER);
        InfrastructureDefinition infrastructureDefinition =
            infrastructureDefinitionService.get(workflow.getAppId(), workflow.getInfraDefinitionId());
        notNullCheck("Invalid Infrastructure Definition", infrastructureDefinition, USER);
        infrastructureMapping = infrastructureDefinition.getInfraMapping();
        validateInfraMappingWithService(workflow, orchestrationWorkflow, infrastructureMapping);
      }
    }
  }

  private void validateInfraMappingWithService(
      Workflow workflow, OrchestrationWorkflow orchestrationWorkflow, InfrastructureMapping infrastructureMapping) {
    if (orchestrationWorkflow.getOrchestrationWorkflowType() == ROLLING) {
      if (!(InfrastructureMappingType.AWS_SSH.name().equals(infrastructureMapping.getInfraMappingType())
              || InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name().equals(
                     infrastructureMapping.getInfraMappingType())
              || workflowServiceHelper.isK8sV2Service(infrastructureMapping.getAppId(), workflow.getServiceId()))) {
        throw new InvalidRequestException(
            "Requested Service/InfrastructureType is not supported using Rolling Deployment", USER);
      }
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType() == BLUE_GREEN) {
      if (!(InfrastructureMappingType.DIRECT_KUBERNETES.name().equals(infrastructureMapping.getInfraMappingType())
              || InfrastructureMappingType.GCP_KUBERNETES.name().equals(infrastructureMapping.getInfraMappingType())
              || InfrastructureMappingType.AZURE_KUBERNETES.name().equals(infrastructureMapping.getInfraMappingType())
              || InfrastructureMappingType.PCF_PCF.name().equals(infrastructureMapping.getInfraMappingType())
              || InfrastructureMappingType.AWS_AMI.name().equals(infrastructureMapping.getInfraMappingType())
              || InfrastructureMappingType.AWS_ECS.name().equals(infrastructureMapping.getInfraMappingType()))) {
        throw new InvalidRequestException(
            "Requested Infrastructure Type is not supported using Blue/Green Deployment", USER);
      }
    }
  }

  private int verifyDeleteInEachPhaseStep(
      PhaseStep phaseStep, SettingAttribute settingAttribute, List<String> context, StringBuilder sb) {
    if (phaseStep.getSteps() == null) {
      return 0;
    }
    int count = 0;
    for (GraphNode step : phaseStep.getSteps()) {
      if (step.getProperties() == null) {
        continue;
      }
      for (Object values : step.getProperties().values()) {
        if (!settingAttribute.getUuid().equals(values)) {
          continue;
        }
        sb.append(" (")
            .append(String.join(":", context))
            .append(':')
            .append(phaseStep.getName())
            .append(':')
            .append(step.getName())
            .append(") ");
        ++count;
      }
    }

    return count;
  }

  @Override
  public Rejection settingsServiceDeleting(SettingAttribute settingAttribute) {
    List<Workflow> workflows = new ArrayList<>();
    StringBuilder sb = new StringBuilder();

    if (settingAttribute.getAppId().equals(GLOBAL_APP_ID)) {
      String accountId = settingAttribute.getAccountId();
      List<String> appsIds = appService.getAppsByAccountId(accountId).stream().map(Base::getAppId).collect(toList());

      if (!appsIds.isEmpty()) {
        workflows = listWorkflows(
            aPageRequest().withLimit(PageRequest.UNLIMITED).addFilter(APP_ID_KEY, IN, appsIds.toArray()).build())
                        .getResponse();
      }
    } else {
      workflows = listWorkflows(aPageRequest()
                                    .withLimit(PageRequest.UNLIMITED)
                                    .addFilter(APP_ID_KEY, Operator.EQ, settingAttribute.getAppId())
                                    .build())
                      .getResponse();
    }

    int count = 0;
    for (Workflow workflow : workflows) {
      OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
      if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;

        // predeployment steps
        PhaseStep preDeploymentStep = canaryOrchestrationWorkflow.getPreDeploymentSteps();
        count +=
            verifyDeleteInEachPhaseStep(preDeploymentStep, settingAttribute, Arrays.asList(workflow.getName()), sb);

        // workflow phases
        List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
        for (WorkflowPhase workflowPhase : workflowPhases) {
          for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
            count += verifyDeleteInEachPhaseStep(
                phaseStep, settingAttribute, Arrays.asList(workflow.getName(), workflowPhase.getName()), sb);
          }
        }

        // postDeployment steps
        PhaseStep postDeploymentStep = canaryOrchestrationWorkflow.getPostDeploymentSteps();
        count +=
            verifyDeleteInEachPhaseStep(postDeploymentStep, settingAttribute, Arrays.asList(workflow.getName()), sb);
      }
    }

    if (count == 0) {
      return null;
    }

    final String msg = format("Connector [%s] is referenced by %s [%s]", settingAttribute.getName(),
        plural("workflow", count), sb.toString());
    return () -> msg;
  }

  @Override
  public List<InstanceElement> getDeployedNodes(String appId, String workflowId) {
    int offset = 0;
    final PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                           .addFilter("appId", Operator.EQ, appId)
                                                           .addFilter("workflowId", Operator.EQ, workflowId)
                                                           .addFilter("status", Operator.EQ, SUCCESS)
                                                           .addOrder(WorkflowExecutionKeys.createdAt, OrderType.DESC)
                                                           .withOffset(String.valueOf(offset))
                                                           .withLimit(String.valueOf(PageRequest.DEFAULT_PAGE_SIZE))
                                                           .build();

    PageResponse<WorkflowExecution> workflowExecutions;
    List<InstanceElement> instanceElements = new ArrayList<>();
    do {
      workflowExecutions = workflowExecutionService.listExecutions(pageRequest, false);

      if (isEmpty(workflowExecutions)) {
        logger.info("Did not find a successful execution for {}. ", workflowId);
        return singletonList(InstanceElement.Builder.anInstanceElement()
                                 .hostName("No successful workflow execution found for this workflow. "
                                     + "Please run the workflow to get deployed nodes")
                                 .build());
      }

      for (WorkflowExecution workflowExecution : workflowExecutions) {
        String envId = workflowExecution.getEnvId();
        for (ElementExecutionSummary executionSummary : workflowExecution.getServiceExecutionSummaries()) {
          for (InstanceStatusSummary instanceStatusSummary : executionSummary.getInstanceStatusSummaries()) {
            InstanceElement instanceElement = instanceStatusSummary.getInstanceElement();
            instanceElement.setServiceTemplateElement(null);
            if (instanceElement.getHost() != null) {
              String hostUuid = instanceElement.getHost().getUuid();
              if (!isEmpty(hostUuid)) {
                Host host = hostService.get(appId, envId, hostUuid);
                instanceElement.getHost().setEc2Instance(host.getEc2Instance());
              }
            }
            instanceElements.add(instanceElement);
          }
        }
        if (!isEmpty(instanceElements)) {
          return instanceElements;
        }
      }
      offset = offset + PageRequest.DEFAULT_PAGE_SIZE;
      pageRequest.setOffset(String.valueOf(offset));
    } while (workflowExecutions.size() >= PageRequest.DEFAULT_PAGE_SIZE);

    logger.info("No nodes were found in any execution for workflow {}", workflowId);
    return singletonList(InstanceElement.Builder.anInstanceElement()
                             .hostName("No workflow execution found with deployed nodes for this workflow. "
                                 + "Please run the workflow to get deployed nodes")
                             .build());
  }

  @Override
  public String obtainEnvIdWithoutOrchestration(Workflow workflow, Map<String, String> workflowVariables) {
    return workflowServiceHelper.obtainEnvIdWithoutOrchestration(workflow, workflowVariables);
  }

  @Override
  public String resolveEnvironmentId(Workflow workflow, Map<String, String> workflowVariables) {
    return workflowServiceHelper.resolveEnvironmentId(workflow, workflowVariables);
  }

  @Override
  public String obtainTemplatedEnvironmentId(Workflow workflow, Map<String, String> workflowVariables) {
    return workflowServiceHelper.obtainTemplatedEnvironmentId(workflow, workflowVariables);
  }

  @Override
  public GraphNode readGraphNode(String appId, String workflowId, String nodeId) {
    Workflow workflow = wingsPersistence.getWithAppId(Workflow.class, appId, workflowId);
    notNullCheck("Workflow was deleted", workflow, WingsException.USER);

    loadOrchestrationWorkflow(workflow, workflow.getDefaultVersion(), false);
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (!(orchestrationWorkflow instanceof CanaryOrchestrationWorkflow)) {
      return null;
    }
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    GraphNode graphNode = null;
    // Verify in Predeployment steps
    graphNode = matchesInPhaseStep(canaryOrchestrationWorkflow.getPreDeploymentSteps(), nodeId);
    if (graphNode != null) {
      return graphNode;
    }

    // Verify in PostDeployment Steps
    graphNode = matchesInPhaseStep(canaryOrchestrationWorkflow.getPostDeploymentSteps(), nodeId);
    if (graphNode != null) {
      return graphNode;
    }

    // Verify in workflow phases
    List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
    if (isEmpty(workflowPhases)) {
      return null;
    }
    for (WorkflowPhase workflowPhase : workflowPhases) {
      List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();
      if (isNotEmpty(phaseSteps)) {
        for (PhaseStep phaseStep : phaseSteps) {
          graphNode = matchesInPhaseStep(phaseStep, nodeId);
          if (graphNode != null) {
            return graphNode;
          }
        }
      }
    }
    return null;
  }

  private GraphNode matchesInPhaseStep(PhaseStep phaseStep, String nodeId) {
    if (phaseStep != null && phaseStep.getSteps() != null) {
      return phaseStep.getSteps()
          .stream()
          .filter(graphNode -> graphNode.getId() != null && graphNode.getId().equals(nodeId))
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  @Override
  public List<EntityType> getRequiredEntities(String appId, String workflowId) {
    Workflow workflow = readWorkflow(appId, workflowId);
    Set<EntityType> entityTypes = fetchRequiredEntityTypes(appId, workflow);
    if (isNotEmpty(entityTypes)) {
      return new ArrayList<>(entityTypes);
    }
    return null;
  }

  @Override
  public List<String> getLastSuccessfulWorkflowExecutionIds(String appId, String workflowId, String serviceId) {
    final List<WorkflowExecution> workflowExecutions =
        workflowExecutionService.getLastSuccessfulWorkflowExecutions(appId, workflowId, serviceId);
    final List<String> workflowExecutionIds = new ArrayList<>();
    if (workflowExecutions != null) {
      for (WorkflowExecution workflowExecution : workflowExecutions) {
        workflowExecutionIds.add(workflowExecution.getUuid());
      }
    }
    return workflowExecutionIds;
  }

  @Override
  public boolean isStateValid(String appId, String stateExecutionId) {
    StateExecutionInstance stateExecutionInstance =
        workflowExecutionService.getStateExecutionData(appId, stateExecutionId);
    return stateExecutionInstance != null && !ExecutionStatus.isFinalStatus(stateExecutionInstance.getStatus());
  }

  @Override
  public ExecutionStatus getExecutionStatus(String appId, String stateExecutionId) {
    return workflowExecutionService.getStateExecutionData(appId, stateExecutionId).getStatus();
  }

  @Override
  public WorkflowExecution getWorkflowExecutionForStateExecutionId(final String appId, final String stateExecutionId) {
    StateExecutionInstance stateExecutionData = workflowExecutionService.getStateExecutionData(appId, stateExecutionId);
    return workflowExecutionService.getWorkflowExecutionForVerificationService(
        appId, stateExecutionData.getExecutionUuid());
  }

  @Override
  public String fetchWorkflowName(String appId, String workflowId) {
    Workflow workflow = wingsPersistence.createQuery(Workflow.class)
                            .project(Workflow.NAME_KEY, true)
                            .filter(APP_ID_KEY, appId)
                            .filter(Pipeline.ID_KEY, workflowId)
                            .get();
    notNullCheck("Workflow does not exist", workflow, USER);
    return workflow.getName();
  }

  @Override
  public List<String> obtainWorkflowNamesReferencedByEnvironment(String appId, String envId) {
    List<String> referencedWorkflows = new ArrayList<>();
    try (HIterator<Workflow> workflowHIterator =
             new HIterator<>(wingsPersistence.createQuery(Workflow.class).filter(APP_ID_KEY, appId).fetch())) {
      if (workflowHIterator != null) {
        for (Workflow workflow : workflowHIterator) {
          if (workflow.getEnvId() != null && !workflow.checkEnvironmentTemplatized()
              && workflow.getEnvId().equals(envId)) {
            referencedWorkflows.add(workflow.getName());
          }
        }
      }
    }
    return referencedWorkflows;
  }

  @Override
  public List<String> obtainWorkflowNamesReferencedByServiceInfrastructure(String appId, String infraMappingId) {
    List<Workflow> workflows =
        listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", Operator.EQ, appId).build()).getResponse();

    return workflows.stream()
        .filter(wfl -> {
          if (wfl.getOrchestrationWorkflow() != null
              && wfl.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
            return ((CanaryOrchestrationWorkflow) wfl.getOrchestrationWorkflow())
                .getWorkflowPhaseIdMap()
                .values()
                .stream()
                .anyMatch(workflowPhase
                    -> !workflowPhase.checkInfraTemplatized()
                        && infraMappingId.equals(workflowPhase.getInfraMappingId()));
          }
          return false;
        })
        .map(Workflow::getName)
        .collect(toList());
  }

  @Override
  public List<String> obtainWorkflowNamesReferencedByInfrastructureDefinition(String appId, String infraDefinitionId) {
    List<Workflow> workflows =
        listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", Operator.EQ, appId).build()).getResponse();

    return workflows.stream()
        .filter(wfl -> {
          if (wfl.getOrchestrationWorkflow() != null
              && wfl.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
            return ((CanaryOrchestrationWorkflow) wfl.getOrchestrationWorkflow())
                .getWorkflowPhaseIdMap()
                .values()
                .stream()
                .anyMatch(workflowPhase
                    -> !workflowPhase.checkInfraDefinitionTemplatized()
                        && infraDefinitionId.equals(workflowPhase.getInfraDefinitionId()));
          }
          return false;
        })
        .map(Workflow::getName)
        .collect(toList());
  }

  @Override
  public WorkflowCategorySteps calculateCategorySteps(
      Workflow workflow, String userId, String phaseId, String sectionId, int position, boolean rollbackSection) {
    Set<String> favorites = null;
    LinkedList<String> recent = null;

    if (userId != null) {
      final Personalization personalization =
          personalizationService.fetch(workflow.getAccountId(), userId, asList(PersonalizationKeys.steps));

      if (personalization != null && personalization.getSteps() != null) {
        favorites = personalization.getSteps().getFavorites();
        recent = personalization.getSteps().getRecent();
      }
    }
    // filter StepType by PhaseStepType
    List<StepType> stepTypesList = StepType.filterByPhaseStepType(sectionId, rollbackSection);
    // get workflow phase
    WorkflowPhase workflowPhase = null;
    if (phaseId != null && !phaseId.equals("default")) { // for pre-deployment phaseId will be default
      workflowPhase = workflowServiceHelper.getWorkflowPhase(workflow, phaseId);
    }
    // special handling for select node step types
    StepType filteredSelectNode = null;
    if (workflowPhase != null) {
      filteredSelectNode = fetchStepTypeFromInfraMappingTypeForSelectNode(workflowPhase, workflow.getAppId());
    }
    List<StepType> filteredStepTypes = filterSelectNodesStep(stepTypesList, filteredSelectNode);
    StepType[] stepTypes = filteredStepTypes.stream().toArray(StepType[] ::new);
    return calculateCategorySteps(favorites, recent, stepTypes, workflowPhase, workflow.getAppId(),
        workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType());
  }

  public WorkflowCategorySteps calculateCategorySteps(Set<String> favorites, LinkedList<String> recent,
      StepType[] stepTypes, WorkflowPhase workflowPhase, String appId,
      OrchestrationWorkflowType orchestrationWorkflowType) {
    Map<String, WorkflowStepMeta> steps = new HashMap<>();
    DeploymentType workflowPhaseDeploymentType = workflowPhase != null ? workflowPhase.getDeploymentType() : null;
    for (StepType step : stepTypes) {
      if (step.matches(workflowPhaseDeploymentType, orchestrationWorkflowType)) {
        final WorkflowStepMeta stepMeta = WorkflowStepMeta.builder()
                                              .name(step.getName())
                                              .favorite(isNotEmpty(favorites) && favorites.contains(step.getType()))
                                              .available(true)
                                              .build();
        steps.put(step.getType(), stepMeta);
      }
    }

    List<WorkflowCategoryStepsMeta> categories = new ArrayList<>();
    // Add recents
    addRecentsToWorkflowCategories(recent, categories);
    // Add favorites category always even if it is empty.
    addFavoritesToWorkflowCategories(favorites, steps.keySet(), categories);
    // get all categories relevant for workflow
    addWorkflowCategorySteps(steps.keySet(), categories);
    // add service commands to categories
    addServiceCommandsToWorkflowCategories(steps, fetchServiceCommandNames(workflowPhase, appId), categories);

    return WorkflowCategorySteps.builder().steps(steps).categories(categories).build();
  }

  private List<StepType> filterSelectNodesStep(List<StepType> stepTypesList, StepType filteredSelectNode) {
    List<StepType> stepTypes = new ArrayList<>();
    for (StepType stepType : stepTypesList) {
      if (StepType.AWS_NODE_SELECT == stepType || StepType.DC_NODE_SELECT == stepType
          || StepType.AZURE_NODE_SELECT == stepType) {
        if (stepType == filteredSelectNode) {
          stepTypes.add(stepType);
        }
      } else {
        stepTypes.add(stepType);
      }
    }
    return stepTypes;
  }

  private StepType fetchStepTypeFromInfraMappingTypeForSelectNode(WorkflowPhase workflowPhase, String appId) {
    boolean infraRefactor = featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(appId));
    InfrastructureMappingType infrastructureMappingType;
    if (infraRefactor) {
      String infraDefinitionId = workflowPhase.getInfraDefinitionId();
      if (infraDefinitionId != null) {
        InfrastructureDefinition infrastructureDefinition =
            infrastructureDefinitionService.get(appId, infraDefinitionId);
        if (infrastructureDefinition != null) {
          infrastructureMappingType =
              InfrastructureMappingType.valueOf(infrastructureDefinition.getInfrastructure().getInfrastructureType());
          return StepType.infrastructureMappingTypeToStepTypeMap.get(infrastructureMappingType);
        }
      }
    } else {
      String infraMappingId = workflowPhase.getInfraMappingId();
      if (infraMappingId != null) {
        InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
        if (infrastructureMapping != null) {
          infrastructureMappingType = InfrastructureMappingType.valueOf(infrastructureMapping.getInfraMappingType());
          return StepType.infrastructureMappingTypeToStepTypeMap.get(infrastructureMappingType);
        }
      }
    }
    return null;
  }

  private List<String> fetchServiceCommandNames(WorkflowPhase workflowPhase, String appId) {
    if (workflowPhase == null) {
      return new ArrayList<>();
    }
    String serviceId = workflowServiceHelper.getServiceIdFromPhase(workflowPhase);
    return workflowServiceHelper.getServiceCommands(workflowPhase, appId, serviceId);
  }

  public void addWorkflowCategorySteps(Set<String> filteredStepTypes, List<WorkflowCategoryStepsMeta> categories) {
    for (Entry<WorkflowStepType, List<StepType>> entry : StepType.workflowStepTypeListMap.entrySet()) {
      WorkflowStepType workflowStepType = entry.getKey();
      List<StepType> stepTypeList = entry.getValue();
      if (workflowStepType.name().equals(SERVICE_COMMAND.name()) || isEmpty(stepTypeList)) {
        continue;
      }
      List<String> stepIds = new ArrayList<>();
      for (StepType stepType : stepTypeList) {
        if (filteredStepTypes.contains(stepType.getType()) && !stepIds.contains(stepType.getType())) {
          stepIds.add(stepType.getType());
        }
      }
      // not adding category if there are no steps
      if (isNotEmpty(stepIds)) {
        categories.add(WorkflowCategoryStepsMeta.builder()
                           .id(workflowStepType.name())
                           .name(workflowStepType.getDisplayName())
                           .stepIds(stepIds)
                           .build());
      }
    }
  }

  private void addServiceCommandsToWorkflowCategories(
      Map<String, WorkflowStepMeta> steps, List<String> commandNames, List<WorkflowCategoryStepsMeta> categories) {
    // Special handling for ServiceCommands
    // For "Service Commands" Category to show up on UI, the corresponding StepIds must be present in Map<> steps
    if (isNotEmpty(commandNames)) {
      List<String> upped = new ArrayList<>();
      for (String commandName : commandNames) {
        // converting commandName like "Install" to uppercase. This is to make it consistent with other steps
        // and also since UI needs it this way to display the icon in UI
        upped.add(commandName.toUpperCase());
        steps.put(commandName.toUpperCase(),
            WorkflowStepMeta.builder().favorite(false).available(true).name(commandName).build());
      }
      categories.add(WorkflowCategoryStepsMeta.builder()
                         .id(SERVICE_COMMAND.name())
                         .name(SERVICE_COMMAND.getDisplayName())
                         .stepIds(upped)
                         .build());
    }
  }

  private void addFavoritesToWorkflowCategories(
      Set<String> favorites, Set<String> filteredStepTypes, List<WorkflowCategoryStepsMeta> categories) {
    List<String> stepIds = new ArrayList<>();
    if (isNotEmpty(favorites)) {
      for (String stepType : filteredStepTypes) {
        if (favorites.contains(stepType)) {
          stepIds.add(stepType);
        }
      }
    }
    categories.add(
        WorkflowCategoryStepsMeta.builder().id("MY_FAVORITES").name("My Favorites").stepIds(stepIds).build());
  }

  private void addRecentsToWorkflowCategories(LinkedList<String> recent, List<WorkflowCategoryStepsMeta> categories) {
    if (isNotEmpty(recent)) {
      List<String> stepIds = new ArrayList<>();
      recent.descendingIterator().forEachRemaining(stepIds::add);
      categories.add(
          WorkflowCategoryStepsMeta.builder().id("RECENTLY_USED").name("Recently Used").stepIds(stepIds).build());
    }
  }

  @Override
  public List<String> obtainWorkflowNamesReferencedByService(String appId, String serviceId) {
    List<Workflow> workflows =
        listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", Operator.EQ, appId).build()).getResponse();
    return workflows.stream()
        .filter(wfl -> {
          if (wfl.getOrchestrationWorkflow() != null
              && wfl.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
            return ((CanaryOrchestrationWorkflow) wfl.getOrchestrationWorkflow())
                .getWorkflowPhaseIdMap()
                .values()
                .stream()
                .anyMatch(workflowPhase
                    -> !workflowPhase.checkServiceTemplatized() && serviceId.equals(workflowPhase.getServiceId()));
          }
          return false;
        })
        .map(Workflow::getName)
        .collect(toList());
  }
}
