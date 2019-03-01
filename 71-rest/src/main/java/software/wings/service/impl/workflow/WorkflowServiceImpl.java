package software.wings.service.impl.workflow;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.OrchestrationWorkflowType.BASIC;
import static io.harness.beans.OrchestrationWorkflowType.BLUE_GREEN;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.beans.OrchestrationWorkflowType.CANARY;
import static io.harness.beans.OrchestrationWorkflowType.MULTI_SERVICE;
import static io.harness.beans.OrchestrationWorkflowType.ROLLING;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.WORKFLOW_EXECUTION_IN_PROGRESS;
import static io.harness.exception.HintException.MOVE_TO_THE_PARENT_OBJECT;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.expression.ExpressionEvaluator.matchesVariablePattern;
import static io.harness.govern.Switch.noop;
import static io.harness.k8s.manifest.ManifestHelper.primaryServiceNameExpression;
import static io.harness.k8s.manifest.ManifestHelper.stageServiceNameExpression;
import static io.harness.mongo.MongoUtils.setUnset;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.atteo.evo.inflector.English.plural;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.api.DeploymentType.AMI;
import static software.wings.api.DeploymentType.AWS_CODEDEPLOY;
import static software.wings.api.DeploymentType.AWS_LAMBDA;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.HELM;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.PCF;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.EntityType.ARTIFACT;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.COLLECT_ARTIFACT;
import static software.wings.beans.PhaseStepType.K8S_PHASE_STEP;
import static software.wings.beans.PhaseStepType.PREPARE_STEPS;
import static software.wings.beans.PhaseStepType.WRAP_UP;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.common.Constants.K8S_CANARY_PHASE_NAME;
import static software.wings.common.Constants.K8S_PRIMARY_PHASE_NAME;
import static software.wings.common.Constants.ROLLBACK_PROVISIONERS;
import static software.wings.common.Constants.WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE;
import static software.wings.sm.StateType.ARTIFACT_COLLECTION;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_DEPLOY;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_SETUP;
import static software.wings.sm.StateType.AWS_CODEDEPLOY_STATE;
import static software.wings.sm.StateType.AWS_LAMBDA_STATE;
import static software.wings.sm.StateType.CLOUD_FORMATION_CREATE_STACK;
import static software.wings.sm.StateType.ECS_DAEMON_SERVICE_SETUP;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;
import static software.wings.sm.StateType.HTTP;
import static software.wings.sm.StateType.K8S_BLUE_GREEN_DEPLOY;
import static software.wings.sm.StateType.K8S_CANARY_DEPLOY;
import static software.wings.sm.StateType.K8S_DELETE;
import static software.wings.sm.StateType.K8S_DEPLOYMENT_ROLLING;
import static software.wings.sm.StateType.K8S_DEPLOYMENT_ROLLING_ROLLBACK;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY;
import static software.wings.sm.StateType.KUBERNETES_SETUP;
import static software.wings.sm.StateType.KUBERNETES_SWAP_SERVICE_SELECTORS;
import static software.wings.sm.StateType.PCF_RESIZE;
import static software.wings.sm.StateType.PCF_SETUP;
import static software.wings.sm.StateType.SHELL_SCRIPT;
import static software.wings.sm.StateType.TERRAFORM_ROLLBACK;
import static software.wings.sm.StateType.values;
import static software.wings.utils.Validator.notNullCheck;

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
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.ExplanationException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.LimitEnforcementUtils;
import io.harness.limits.checker.StaticLimitCheckerWithDecrement;
import io.harness.observer.Rejection;
import io.harness.persistence.HIterator;
import io.harness.queue.Queue;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentType;
import software.wings.api.InstanceElement;
import software.wings.app.StaticConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.BuildWorkflow;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.Event.Type;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureStrategy;
import software.wings.beans.FailureType;
import software.wings.beans.Graph;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Pipeline;
import software.wings.beans.RepairActionCode;
import software.wings.beans.RoleType;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowCreationFlags;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.deployment.DeploymentMetadata.DeploymentMetadataBuilder;
import software.wings.beans.deployment.DeploymentMetadata.Include;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.stats.CloneMetadata;
import software.wings.beans.trigger.Trigger;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.prune.PruneEntityListener;
import software.wings.prune.PruneEvent;
import software.wings.service.impl.ServiceClassLocator;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.ownership.OwnedByWorkflow;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.sm.states.k8s.K8sStateHelper;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilCategory;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class WorkflowServiceImpl.
 *
 * @author Rishi
 */
@SuppressWarnings("ALL")
@Singleton
@ValidateOnExecution
public class WorkflowServiceImpl implements WorkflowService, DataProvider {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);

  private static final List<String> kubernetesArtifactNeededStateTypes =
      Arrays.asList(KUBERNETES_SETUP.name(), KUBERNETES_DEPLOY.name());

  private static final List<String> k8sV2ArtifactNeededStateTypes =
      Arrays.asList(K8S_DEPLOYMENT_ROLLING.name(), K8S_CANARY_DEPLOY.name(), K8S_BLUE_GREEN_DEPLOY.name());

  private static final List<String> ecsArtifactNeededStateTypes =
      Arrays.asList(ECS_SERVICE_DEPLOY.name(), ECS_SERVICE_SETUP.name(), ECS_DAEMON_SERVICE_SETUP.name());

  private static final List<String> amiArtifactNeededStateTypes =
      Arrays.asList(AWS_AMI_SERVICE_SETUP.name(), AWS_AMI_SERVICE_DEPLOY.name());

  private static final List<String> codeDeployArtifactNeededStateTypes = Arrays.asList(AWS_CODEDEPLOY_STATE.name());

  private static final List<String> awsLambdaArtifactNeededStateTypes = Arrays.asList(AWS_LAMBDA_STATE.name());

  private static final List<String> pcfArtifactNeededStateTypes = Arrays.asList(PCF_SETUP.name(), PCF_RESIZE.name());

  private static final Comparator<Stencil> stencilDefaultSorter = (o1, o2) -> {
    int comp = o1.getStencilCategory().getDisplayOrder().compareTo(o2.getStencilCategory().getDisplayOrder());
    if (comp != 0) {
      return comp;
    }
    comp = o1.getDisplayOrder().compareTo(o2.getDisplayOrder());
    if (comp != 0) {
      return comp;
    }
    return o1.getType().compareTo(o2.getType());
  };

  @Inject private WingsPersistence wingsPersistence;
  @Inject private StencilPostProcessor stencilPostProcessor;
  @Inject private StaticConfiguration staticConfiguration;

  @Inject private AccountService accountService;
  @Inject private AppService appService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private EntityVersionService entityVersionService;
  @Inject private ExecutorService executorService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private PipelineService pipelineService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private SettingsService settingsService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private TriggerService triggerService;
  @Inject private EnvironmentService environmentService;
  @Inject private WorkflowServiceHelper workflowServiceHelper;
  @Inject private WorkflowServiceTemplateHelper workflowServiceTemplateHelper;
  @Inject private HostService hostService;
  @Inject private YamlPushService yamlPushService;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private LimitCheckerFactory limitCheckerFactory;
  @Inject private K8sStateHelper k8sStateHelper;

  @Inject private Queue<PruneEvent> pruneQueue;

  private Map<StateTypeScope, List<StateTypeDescriptor>> cachedStencils;
  private Map<String, StateTypeDescriptor> cachedStencilMap;

  /**
   * {@inheritDoc}
   */
  @Override
  public StateMachine createStateMachine(StateMachine stateMachine) {
    stateMachine.validate();
    return wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
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
    return getStencils(appId, workflowId, phaseId, stateTypeScopes);
  }

  private Map<StateTypeScope, List<Stencil>> getStencils(
      String appId, String workflowId, String phaseId, StateTypeScope[] stateTypeScopes) {
    Map<StateTypeScope, List<StateTypeDescriptor>> stencilsMap = loadStateTypes(appService.getAccountIdByAppId(appId));
    return getStateTypeScopeListMap(appId, workflowId, phaseId, stateTypeScopes, stencilsMap);
  }

  private Map<StateTypeScope, List<Stencil>> getStateTypeScopeListMap(String appId, String workflowId, String phaseId,
      StateTypeScope[] stateTypeScopes, Map<StateTypeScope, List<StateTypeDescriptor>> stencilsMap) {
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
        buildWorkflow = BUILD.equals(orchestrationWorkflow.getOrchestrationWorkflowType());
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
          String infraMappingId = workflowPhase.getInfraMappingId();
          if (workflowPhase.checkInfraTemplatized()) {
            DeploymentType workflowPhaseDeploymentType = workflowPhase.getDeploymentType();
            if (workflowPhaseDeploymentType != null) {
              predicate = stencil -> stencil.matches(workflowPhaseDeploymentType);
            }
          } else if (infraMappingId != null) {
            // TODO: This check can be removed once refacotor the code to have single class for AWS_NODE_SELECT,
            // DC_NODE_SELECT and ROLLING_SELECT_NODE
            InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
            if (infrastructureMapping != null) {
              predicate = stencil -> stencil.matches(infrastructureMapping);
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
    maps = maps.entrySet().stream().collect(toMap(Entry::getKey,
        stateTypeScopeListEntry
        -> stateTypeScopeListEntry.getValue().stream().filter(buildWorkflowPredicate).collect(toList())));
    return maps;
  }

  private Map<StateTypeScope, List<Stencil>> getStateTypeForApp(
      String appId, Map<StateTypeScope, List<StateTypeDescriptor>> stencilsMap, Map<String, String> entityMap) {
    Map<StateTypeScope, List<Stencil>> mapByScope;
    mapByScope = stencilsMap.entrySet().stream().collect(toMap(Entry::getKey,
        stateTypeScopeListEntry
        -> stencilPostProcessor.postProcess(stateTypeScopeListEntry.getValue(), appId, entityMap)));
    return mapByScope;
  }

  private Map<StateTypeScope, List<StateTypeDescriptor>> loadStateTypes(String accountId) {
    if (cachedStencils != null) {
      return cachedStencils;
    }

    List<StateTypeDescriptor> stencils = Arrays.asList(values());

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
   */
  @Override
  public Map<String, StateTypeDescriptor> stencilMap() {
    if (cachedStencilMap == null) {
      stencils(null, null, null);
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
    return listWorkflows(pageRequest, 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Workflow> listWorkflows(PageRequest<Workflow> pageRequest, Integer previousExecutionsCount) {
    PageResponse<Workflow> workflows = listWorkflowsWithoutOrchestration(pageRequest);
    if (workflows != null && workflows.getResponse() != null) {
      for (Workflow workflow : workflows.getResponse()) {
        try {
          loadOrchestrationWorkflow(workflow, workflow.getDefaultVersion());
        } catch (Exception e) {
          logger.error(format("Failed to load Orchestration workflow %s", workflow.getUuid()), e);
        }
      }
    }
    if (previousExecutionsCount != null && previousExecutionsCount > 0) {
      for (Workflow workflow : workflows) {
        try {
          PageRequest<WorkflowExecution> workflowExecutionPageRequest =
              aPageRequest()
                  .withLimit(previousExecutionsCount.toString())
                  .addFilter("workflowId", EQ, workflow.getUuid())
                  .addFilter("appId", EQ, workflow.getAppId())
                  .build();

          workflow.setWorkflowExecutions(
              workflowExecutionService.listExecutions(workflowExecutionPageRequest, false, false, false, false)
                  .getResponse());
        } catch (Exception e) {
          logger.error(format("Failed to fetch recent executions for workflow %s", workflow.getUuid()), e);
        }
      }
    }
    return workflows;
  }

  @Override
  public Workflow readWorkflow(String appId, String workflowId) {
    return readWorkflow(appId, workflowId, null);
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
  public Workflow readWorkflowWithoutOrchestration(String appId, String workflowId) {
    return wingsPersistence.getWithAppId(Workflow.class, appId, workflowId);
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
    Workflow workflow =
        wingsPersistence.createQuery(Workflow.class).filter("appId", appId).filter("name", workflowName).get();
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
      workflow.setOrchestrationWorkflow(stateMachine.getOrchestrationWorkflow());
      OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
      if (orchestrationWorkflow != null) {
        orchestrationWorkflow.onLoad();
        workflow.setDeploymentTypes(workflowServiceHelper.obtainDeploymentTypes(orchestrationWorkflow));
        workflow.setTemplatized(orchestrationWorkflow.checkTemplatized());
        if (withServices) {
          populateServices(workflow);
        }
      }
    }
  }

  private void createK8sWorkflow(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();

    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    addLinkedPreOrPostDeploymentSteps(canaryOrchestrationWorkflow);

    if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(CANARY)) {
      addK8sCanaryWorkflowPhase(workflow);
      addK8sRollingWorkflowPhase(workflow, "Primary");
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(ROLLING)) {
      addK8sRollingWorkflowPhase(workflow, "Rolling");
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BLUE_GREEN)) {
      addK8sBlueGreenWorkflowPhase(workflow);
    } else {
      throw new InvalidRequestException(String.format("WorkflowType %s not supported for Kubernetes V2",
                                            orchestrationWorkflow.getOrchestrationWorkflowType()),
          USER);
    }
  }

  private void addK8sRollingWorkflowPhase(Workflow workflow, String phaseName) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    WorkflowPhase workflowPhase = aWorkflowPhase()
                                      .name(phaseName)
                                      .infraMappingId(workflow.getInfraMappingId())
                                      .serviceId(workflow.getServiceId())
                                      .build();
    workflowServiceHelper.setCloudProvider(workflow.getAppId(), workflowPhase);

    addK8sRollingWorkflowPhaseSteps(workflowPhase);

    workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(workflowPhase);
    ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases().add(workflowPhase);

    WorkflowPhase rollbackPhase = createRollbackPhase(workflowPhase);

    addK8sRollingRollbackWorkflowPhaseSteps(rollbackPhase);

    workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(rollbackPhase);
    ((CanaryOrchestrationWorkflow) orchestrationWorkflow)
        .getRollbackWorkflowPhaseIdMap()
        .put(workflowPhase.getUuid(), rollbackPhase);
  }

  private void addK8sEmptyPhaseStep(WorkflowPhase workflowPhase) {
    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();
    phaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.DEPLOY).build());
  }

  private void addK8sEmptyRollbackPhaseStep(WorkflowPhase rollbackPhase) {
    List<PhaseStep> rollbackPhaseSteps = rollbackPhase.getPhaseSteps();
    rollbackPhaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.DEPLOY)
                               .withPhaseStepNameForRollback(Constants.DEPLOY)
                               .withRollback(true)
                               .build());
  }

  private void addK8sRollingWorkflowPhaseSteps(WorkflowPhase workflowPhase) {
    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    Map<String, Object> defaultSetupProperties = new HashMap<>();
    phaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.DEPLOY)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(K8S_DEPLOYMENT_ROLLING.name())
                                    .name(Constants.K8S_DEPLOYMENT_ROLLING)
                                    .properties(defaultSetupProperties)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.VERIFY).build());

    phaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.WRAP_UP).build());
  }

  private void addK8sRollingRollbackWorkflowPhaseSteps(WorkflowPhase rollbackPhase) {
    List<PhaseStep> rollbackPhaseSteps = rollbackPhase.getPhaseSteps();

    rollbackPhaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.DEPLOY)
                               .addStep(GraphNode.builder()
                                            .id(generateUuid())
                                            .type(K8S_DEPLOYMENT_ROLLING_ROLLBACK.name())
                                            .name(Constants.K8S_DEPLOYMENT_ROLLING_ROLLBAK)
                                            .rollback(true)
                                            .build())
                               .withPhaseStepNameForRollback(Constants.DEPLOY)
                               .withStatusForRollback(ExecutionStatus.SUCCESS)
                               .withRollback(true)
                               .build());

    rollbackPhaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.WRAP_UP)
                               .withPhaseStepNameForRollback(Constants.WRAP_UP)
                               .withRollback(true)
                               .build());
  }

  private void addK8sBlueGreenWorkflowPhase(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    WorkflowPhase workflowPhase = aWorkflowPhase()
                                      .name("Blue/Green")
                                      .infraMappingId(workflow.getInfraMappingId())
                                      .serviceId(workflow.getServiceId())
                                      .build();
    workflowServiceHelper.setCloudProvider(workflow.getAppId(), workflowPhase);

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    Map<String, Object> defaultSetupProperties = new HashMap<>();
    phaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.DEPLOY)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(K8S_BLUE_GREEN_DEPLOY.name())
                                    .name(Constants.K8S_STAGE_DEPLOY)
                                    .properties(defaultSetupProperties)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.VERIFY).build());

    Map<String, Object> defaultRouteUpdateProperties = new HashMap<>();
    defaultRouteUpdateProperties.put("service1", primaryServiceNameExpression);
    defaultRouteUpdateProperties.put("service2", stageServiceNameExpression);
    phaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.ROUTE_UPDATE)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(KUBERNETES_SWAP_SERVICE_SELECTORS.name())
                                    .name(Constants.KUBERNETES_SWAP_SERVICES_PRIMARY_STAGE)
                                    .properties(defaultRouteUpdateProperties)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.WRAP_UP).build());

    workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(workflowPhase);
    ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases().add(workflowPhase);

    WorkflowPhase rollbackPhase = createRollbackPhase(workflowPhase);
    List<PhaseStep> rollbackPhaseSteps = rollbackPhase.getPhaseSteps();

    rollbackPhaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.ROUTE_UPDATE)
                               .withPhaseStepNameForRollback(Constants.ROUTE_UPDATE)
                               .withStatusForRollback(ExecutionStatus.SUCCESS)
                               .withRollback(true)
                               .build());

    rollbackPhaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.WRAP_UP)
                               .withPhaseStepNameForRollback(Constants.WRAP_UP)
                               .withRollback(true)
                               .build());

    workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(rollbackPhase);
    ((CanaryOrchestrationWorkflow) orchestrationWorkflow)
        .getRollbackWorkflowPhaseIdMap()
        .put(workflowPhase.getUuid(), rollbackPhase);
  }

  private WorkflowPhase createRollbackPhase(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .name(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .rollback(true)
        .serviceId(workflowPhase.getServiceId())
        .computeProviderId(workflowPhase.getComputeProviderId())
        .infraMappingName(workflowPhase.getInfraMappingName())
        .phaseNameForRollback(workflowPhase.getName())
        .deploymentType(workflowPhase.getDeploymentType())
        .infraMappingId(workflowPhase.getInfraMappingId())
        .build();
  }

  private void addK8sCanaryWorkflowPhase(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    WorkflowPhase workflowPhase = aWorkflowPhase()
                                      .name("Canary")
                                      .infraMappingId(workflow.getInfraMappingId())
                                      .serviceId(workflow.getServiceId())
                                      .build();
    workflowServiceHelper.setCloudProvider(workflow.getAppId(), workflowPhase);

    addK8sCanaryWorkflowPhaseSteps(workflowPhase);

    workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(workflowPhase);
    ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases().add(workflowPhase);

    WorkflowPhase rollbackPhase = createRollbackPhase(workflowPhase);

    addK8sCanaryRollbackWorkflowPhaseSteps(rollbackPhase);

    workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(rollbackPhase);
    ((CanaryOrchestrationWorkflow) orchestrationWorkflow)
        .getRollbackWorkflowPhaseIdMap()
        .put(workflowPhase.getUuid(), rollbackPhase);
  }

  private void addK8sCanaryWorkflowPhaseSteps(WorkflowPhase workflowPhase) {
    Map<String, Object> defaultCanaryDeployProperties = new HashMap<>();
    defaultCanaryDeployProperties.put("instances", "1");
    defaultCanaryDeployProperties.put("instanceUnitType", "COUNT");

    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    phaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.DEPLOY)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(K8S_CANARY_DEPLOY.name())
                                    .name(Constants.K8S_CANARY_DEPLOY)
                                    .properties(defaultCanaryDeployProperties)
                                    .build())
                       .build());

    phaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.VERIFY).build());

    Map<String, Object> defaultCanaryDeleteProperties = new HashMap<>();
    defaultCanaryDeleteProperties.put("resources", "${k8s.canaryWorkload}");
    defaultCanaryDeleteProperties.put("instanceUnitType", "COUNT");

    phaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.WRAP_UP)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(K8S_DELETE.name())
                                    .name("Canary Delete")
                                    .properties(defaultCanaryDeleteProperties)
                                    .build())
                       .build());
  }

  private void addK8sCanaryRollbackWorkflowPhaseSteps(WorkflowPhase rollbackPhase) {
    List<PhaseStep> rollbackPhaseSteps = rollbackPhase.getPhaseSteps();

    rollbackPhaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.DEPLOY)
                               .withPhaseStepNameForRollback(Constants.DEPLOY)
                               .withStatusForRollback(ExecutionStatus.SUCCESS)
                               .withRollback(true)
                               .build());

    rollbackPhaseSteps.add(aPhaseStep(K8S_PHASE_STEP, Constants.WRAP_UP)
                               .withPhaseStepNameForRollback(Constants.WRAP_UP)
                               .withRollback(true)
                               .build());
  }

  @Override
  @ValidationGroups(Create.class)
  public Workflow createWorkflow(Workflow workflow) {
    String accountId = appService.getAccountIdByAppId(workflow.getAppId());

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

    validateOrchestrationWorkflow(workflow);
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    workflow.setDefaultVersion(1);
    List<String> linkedTemplateUuids = new ArrayList<>();
    if (orchestrationWorkflow != null) {
      if (StringUtils.isNotEmpty(workflow.getServiceId())
          && workflowServiceHelper.isK8sV2Service(workflow.getAppId(), workflow.getServiceId())) {
        createK8sWorkflow(workflow);
      } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(CANARY)
          || orchestrationWorkflow.getOrchestrationWorkflowType().equals(MULTI_SERVICE)) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
        addLinkedPreOrPostDeploymentSteps(canaryOrchestrationWorkflow);
        if (isNotEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
          List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
          canaryOrchestrationWorkflow.setWorkflowPhases(new ArrayList<>());
          workflowPhases.forEach(workflowPhase -> attachWorkflowPhase(workflow, workflowPhase));
        }
      } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)
          || orchestrationWorkflow.getOrchestrationWorkflowType().equals(ROLLING)
          || orchestrationWorkflow.getOrchestrationWorkflowType().equals(BLUE_GREEN)) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
        addLinkedPreOrPostDeploymentSteps(canaryOrchestrationWorkflow);
        WorkflowPhase workflowPhase;
        if (isEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
          workflowPhase = aWorkflowPhase()
                              .infraMappingId(workflow.getInfraMappingId())
                              .serviceId(workflow.getServiceId())
                              .daemonSet(isDaemonSet(workflow.getAppId(), workflow.getServiceId()))
                              .statefulSet(isStatefulSet(workflow.getAppId(), workflow.getServiceId()))
                              .build();
          attachWorkflowPhase(workflow, workflowPhase);
        }
      } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BUILD)) {
        BuildWorkflow buildWorkflow = (BuildWorkflow) orchestrationWorkflow;
        addLinkedPreOrPostDeploymentSteps(buildWorkflow);
        if (isEmpty(buildWorkflow.getWorkflowPhases())) {
          WorkflowPhase workflowPhase = aWorkflowPhase().build();
          attachWorkflowPhase(workflow, workflowPhase);
        }
      }
      if (isEmpty(orchestrationWorkflow.getNotificationRules())) {
        createDefaultNotificationRule(workflow);
      }

      if (!orchestrationWorkflow.getOrchestrationWorkflowType().equals(BUILD)
          && orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
        if (isEmpty(canaryOrchestrationWorkflow.getFailureStrategies())) {
          createDefaultFailureStrategy(workflow);
        }
      }

      // Ensure artifact check
      ensureArtifactCheck(workflow.getAppId(), orchestrationWorkflow);

      // Add environment expressions
      WorkflowServiceTemplateHelper.transformEnvTemplateExpressions(workflow, orchestrationWorkflow);
      orchestrationWorkflow.onSave();

      //      updateRequiredEntityTypes(workflow.getAppId(), orchestrationWorkflow);
      StateMachine stateMachine = new StateMachine(workflow, workflow.getDefaultVersion(),
          ((CustomOrchestrationWorkflow) orchestrationWorkflow).getGraph(), stencilMap());
      stateMachine = wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
      linkedTemplateUuids = workflow.getOrchestrationWorkflow().getLinkedTemplateUuids();

      workflow.setOrchestration(orchestrationWorkflow);
    }

    String key = wingsPersistence.save(workflow);

    // create initial version
    entityVersionService.newEntityVersion(
        workflow.getAppId(), WORKFLOW, key, workflow.getName(), EntityVersion.ChangeType.CREATED, workflow.getNotes());

    Workflow newWorkflow = readWorkflow(workflow.getAppId(), key, workflow.getDefaultVersion());
    updateKeywordsAndLinkedTemplateUuids(newWorkflow, linkedTemplateUuids);

    String accountId = appService.getAccountIdByAppId(workflow.getAppId());
    yamlPushService.pushYamlChangeSet(accountId, null, newWorkflow, Type.CREATE, workflow.isSyncFromGit(), false);

    eventPublishHelper.publishWorkflowCreatedEvent(newWorkflow.getUuid(), accountId);
    return newWorkflow;
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

  private void addLinkedPreOrPostDeploymentSteps(CanaryOrchestrationWorkflow canaryOrchestrationWorkflow) {
    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(
        canaryOrchestrationWorkflow.getPreDeploymentSteps(), null);
    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(
        canaryOrchestrationWorkflow.getPostDeploymentSteps(), null);
  }

  private void updateKeywordsAndLinkedTemplateUuids(Workflow workflow, List<String> linkedTemplateUuids) {
    if (isNotEmpty(linkedTemplateUuids)) {
      linkedTemplateUuids = linkedTemplateUuids.stream().distinct().collect(toList());
    }

    List<String> keywords = workflowServiceHelper.getKeywords(workflow);
    wingsPersistence.update(wingsPersistence.createQuery(Workflow.class)
                                .filter(Constants.APP_ID, workflow.getAppId())
                                .filter(Constants.UUID, workflow.getUuid()),
        wingsPersistence.createUpdateOperations(Workflow.class)
            .set("keywords", keywords)
            .set("linkedTemplateUuids", linkedTemplateUuids));
    workflow.setKeywords(keywords);
    workflow.setLinkedTemplateUuids(linkedTemplateUuids);
  }

  @Override
  public String getHPAYamlStringWithCustomMetric(
      Integer minAutoscaleInstances, Integer maxAutoscaleInstances, Integer targetCpuUtilizationPercentage) {
    return workflowServiceHelper.getHPAYamlStringWithCustomMetric(
        minAutoscaleInstances, maxAutoscaleInstances, targetCpuUtilizationPercentage);
  }

  @Override
  public boolean ensureArtifactCheck(String appId, OrchestrationWorkflow orchestrationWorkflow) {
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
    if (workflowServiceHelper.needArtifactCheckStep(appId, canaryOrchestrationWorkflow)) {
      return workflowServiceHelper.ensureArtifactCheckInPreDeployment(canaryOrchestrationWorkflow);
    }
    return false;
  }

  @Override
  public Workflow updateWorkflow(Workflow workflow) {
    return updateWorkflow(workflow, workflow.getOrchestrationWorkflow());
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
    return updateWorkflow(workflow, workflow.getOrchestrationWorkflow());
  }

  @Override
  public Workflow updateWorkflow(Workflow workflow, OrchestrationWorkflow orchestrationWorkflow) {
    workflowServiceHelper.validateServiceandInframapping(
        workflow.getAppId(), workflow.getServiceId(), workflow.getInfraMappingId());
    return updateWorkflow(workflow, orchestrationWorkflow, true, false, false, false);
  }

  @Override
  public Workflow updateWorkflow(Workflow workflow, OrchestrationWorkflow orchestrationWorkflow,
      boolean inframappingChanged, boolean envChanged, boolean cloned) {
    return updateWorkflow(workflow, orchestrationWorkflow, true, inframappingChanged, envChanged, cloned);
  }

  private Workflow updateWorkflow(Workflow workflow, OrchestrationWorkflow orchestrationWorkflow,
      boolean onSaveCallNeeded, boolean inframappingChanged, boolean envChanged, boolean cloned) {
    Workflow savedWorkflow = readWorkflow(workflow.getAppId(), workflow.getUuid());

    UpdateOperations<Workflow> ops = wingsPersistence.createUpdateOperations(Workflow.class);
    setUnset(ops, "description", workflow.getDescription());
    setUnset(ops, "name", workflow.getName());
    List<TemplateExpression> templateExpressions = workflow.getTemplateExpressions();

    String workflowName = workflow.getName();
    String serviceId = workflow.getServiceId();
    String envId = workflow.getEnvId();
    String inframappingId = workflow.getInfraMappingId();
    boolean isRename = !workflow.getName().equals(savedWorkflow.getName());
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
        templateExpressions, workflow.getAppId(), serviceId, inframappingId, envChanged, inframappingChanged);

    setUnset(ops, "templateExpressions", templateExpressions);

    List<String> linkedTemplateUuids = new ArrayList<>();
    if (orchestrationWorkflow != null) {
      if (onSaveCallNeeded) {
        orchestrationWorkflow.onSave();
        if (envChanged) {
          workflow.setEnvId(envId);
          setUnset(ops, "envId", envId);
        }
        //        updateRequiredEntityTypes(workflow.getAppId(), orchestrationWorkflow);
      }
      if (!cloned) {
        EntityVersion entityVersion = entityVersionService.newEntityVersion(workflow.getAppId(), WORKFLOW,
            workflow.getUuid(), workflow.getName(), EntityVersion.ChangeType.UPDATED, workflow.getNotes());
        workflow.setDefaultVersion(entityVersion.getVersion());
      }

      StateMachine stateMachine = new StateMachine(workflow, workflow.getDefaultVersion(),
          ((CustomOrchestrationWorkflow) orchestrationWorkflow).getGraph(), stencilMap());
      stateMachine = wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
      setUnset(ops, "defaultVersion", workflow.getDefaultVersion());
      linkedTemplateUuids = workflow.getOrchestrationWorkflow().getLinkedTemplateUuids();

      setUnset(ops, "orchestration", workflow.getOrchestrationWorkflow());
    }

    wingsPersistence.update(wingsPersistence.createQuery(Workflow.class)
                                .filter("appId", workflow.getAppId())
                                .filter(ID_KEY, workflow.getUuid()),
        ops);

    Workflow finalWorkflow = readWorkflow(workflow.getAppId(), workflow.getUuid(), workflow.getDefaultVersion());

    String accountId = appService.getAccountIdByAppId(finalWorkflow.getAppId());
    yamlPushService.pushYamlChangeSet(accountId, savedWorkflow, finalWorkflow, Type.UPDATE, isSyncFromGit, isRename);

    if (workflowName != null) {
      if (!workflowName.equals(finalWorkflow.getName())) {
        executorService.submit(() -> triggerService.updateByApp(finalWorkflow.getAppId()));
      }
    }
    updateKeywordsAndLinkedTemplateUuids(finalWorkflow, linkedTemplateUuids);
    return finalWorkflow;
  }

  private void populateServices(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    workflow.setServices(
        serviceResourceService.fetchServicesByUuids(workflow.getAppId(), orchestrationWorkflow.getServiceIds()));
    workflow.setTemplatizedServiceIds(orchestrationWorkflow.getTemplatizedServiceIds());
  }

  private void generateNewWorkflowPhaseStepsForArtifactCollection(WorkflowPhase workflowPhase) {
    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();

    phaseSteps.add(aPhaseStep(PREPARE_STEPS, Constants.PREPARE_STEPS).build());

    phaseSteps.add(aPhaseStep(COLLECT_ARTIFACT, Constants.COLLECT_ARTIFACT)
                       .addStep(GraphNode.builder()
                                    .id(generateUuid())
                                    .type(ARTIFACT_COLLECTION.name())
                                    .name(Constants.ARTIFACT_COLLECTION)
                                    .build())
                       .build());
    phaseSteps.add(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  /**
   * Sets service Id to Phase
   *
   * @param serviceId
   * @param phase
   */
  private void setServiceId(String serviceId, WorkflowPhase phase) {
    if (serviceId != null) {
      phase.setServiceId(serviceId);
    }
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
      throw new WingsException(WORKFLOW_EXECUTION_IN_PROGRESS, USER)
          .addParam("message", format("Workflow: [%s] couldn't be deleted", workflow.getName()));
    }

    List<Trigger> triggers = triggerService.getTriggersHasWorkflowAction(workflow.getAppId(), workflow.getUuid());
    if (isEmpty(triggers)) {
      return;
    }
    List<String> triggerNames = triggers.stream().map(Trigger::getName).collect(toList());

    throw new InvalidRequestException(
        format("Workflow associated as a trigger action to triggers [%s]", Joiner.on(", ").join(triggerNames)), USER);
  }

  private boolean pruneWorkflow(String appId, String workflowId) {
    pruneQueue.send(new PruneEvent(Workflow.class, appId, workflowId));
    if (!wingsPersistence.delete(Workflow.class, appId, workflowId)) {
      return false;
    }

    return true;
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
        .filter(StateMachine.APP_ID_KEY, appId)
        .filter(StateMachine.ORIGIN_ID_KEY, originId)
        .order(Sort.descending(StateMachine.CREATED_AT_KEY))
        .get();
  }

  @Override
  public StateMachine readStateMachine(String appId, String originId, Integer version) {
    return wingsPersistence.createQuery(StateMachine.class)
        .filter(StateMachine.APP_ID_KEY, appId)
        .filter(StateMachine.ORIGIN_ID_KEY, originId)
        .filter(StateMachine.ORIGIN_VERSION_KEY, version)
        .get();
  }

  @Override
  public void pruneByApplication(String appId) {
    // prune workflows
    List<Key<Workflow>> workflowKeys = wingsPersistence.createQuery(Workflow.class).filter("appId", appId).asKeyList();
    for (Key key : workflowKeys) {
      pruneWorkflow(appId, (String) key.getId());
    }

    // prune state machines
    wingsPersistence.delete(wingsPersistence.createQuery(StateMachine.class).filter("appId", appId));
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
    List<Workflow> workflows = wingsPersistence.createQuery(Workflow.class).filter("appId", appId).asList();
    return workflows.stream().collect(toMap(Workflow::getUuid, o -> o.getName()));
  }

  StateType getCorrespondingRollbackState(GraphNode step) {
    if (step.getType().equals(CLOUD_FORMATION_CREATE_STACK.name())) {
      return StateType.CLOUD_FORMATION_ROLLBACK_STACK;
    }

    if (step.getType().equals(StateType.TERRAFORM_PROVISION.name())) {
      return TERRAFORM_ROLLBACK;
    }

    return null;
  }

  private PhaseStep generateRollbackProvisioners(PhaseStep preDeploymentSteps) {
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
    PhaseStep rollbackProvisionerStep = new PhaseStep(PhaseStepType.ROLLBACK_PROVISIONERS, ROLLBACK_PROVISIONERS);
    rollbackProvisionerStep.setUuid(generateUuid());
    provisionerSteps.forEach(step -> {
      Map<String, Object> propertiesMap = Maps.newHashMap();
      propertiesMap.put("provisionerId", step.getProperties().get("provisionerId"));
      propertiesMap.put("timeoutMillis", step.getProperties().get("timeoutMillis"));
      rollbackProvisionerNodes.add(GraphNode.builder()
                                       .type(getCorrespondingRollbackState(step).name())
                                       .rollback(true)
                                       .name("Rollback " + step.getName())
                                       .properties(propertiesMap)
                                       .build());
    });
    rollbackProvisionerStep.setRollback(true);
    rollbackProvisionerStep.setSteps(rollbackProvisionerNodes);
    return rollbackProvisionerStep;
  }

  @Override
  public PhaseStep updatePreDeployment(String appId, String workflowId, PhaseStep phaseStep) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow, USER);

    // Update linked PhaseStep template
    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(
        phaseStep, orchestrationWorkflow.getPreDeploymentSteps());
    orchestrationWorkflow.setPreDeploymentSteps(phaseStep);
    orchestrationWorkflow.setRollbackProvisioners(generateRollbackProvisioners(phaseStep));

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getPreDeploymentSteps();
  }

  @Override
  public PhaseStep updatePostDeployment(String appId, String workflowId, PhaseStep phaseStep) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow, USER);
    // Update linked PhaseStep template
    workflowServiceTemplateHelper.updateLinkedPhaseStepTemplate(
        phaseStep, orchestrationWorkflow.getPostDeploymentSteps());
    orchestrationWorkflow.setPostDeploymentSteps(phaseStep);

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getPostDeploymentSteps();
  }

  @Override
  public WorkflowPhase createWorkflowPhase(String appId, String workflowId, WorkflowPhase workflowPhase) {
    notNullCheck("workflow", workflowPhase, USER);

    workflowServiceHelper.validateServiceandInframapping(
        appId, workflowPhase.getServiceId(), workflowPhase.getInfraMappingId());

    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();

    notNullCheck("orchestrationWorkflow", orchestrationWorkflow, USER);

    workflowPhase.setDaemonSet(isDaemonSet(appId, workflowPhase.getServiceId()));
    workflowPhase.setStatefulSet(isStatefulSet(appId, workflowPhase.getServiceId()));
    attachWorkflowPhase(workflow, workflowPhase);

    if (artifactCheckRequiredForDeployment(workflowPhase, orchestrationWorkflow)) {
      workflowServiceHelper.ensureArtifactCheckInPreDeployment(
          (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow());
    }

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
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
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow, USER);

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
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();

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
  public List<String> getResolvedInfraMappingIds(Workflow workflow, Map<String, String> workflowVariables) {
    return workflowServiceHelper.getResolvedInfraMappingIds(workflow, workflowVariables);
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
    notNullCheck("OrchestrationWorkflow", orchestrationWorkflow, USER);
    if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
      return workflowServiceHelper.workflowHasSshDeploymentPhase(canaryOrchestrationWorkflow);
    }
    return false;
  }

  private void attachWorkflowPhase(Workflow workflow, WorkflowPhase workflowPhase) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.needCloudProvider()) {
      workflowServiceHelper.setCloudProvider(workflow.getAppId(), workflowPhase);
    }

    // No need to generate phase steps if it's already created
    if (isNotEmpty(workflowPhase.getPhaseSteps()) && orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases().add(workflowPhase);
      return;
    }

    if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(CANARY)
        || orchestrationWorkflow.getOrchestrationWorkflowType().equals(MULTI_SERVICE)) {
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
      boolean serviceRepeat = canaryOrchestrationWorkflow.serviceRepeat(workflowPhase);
      boolean createCanaryPhase = !serviceRepeat;
      boolean createPrimaryPhase =
          serviceRepeat && !canaryOrchestrationWorkflow.containsPhaseWithName(K8S_PRIMARY_PHASE_NAME);

      if (workflowServiceHelper.isK8sV2Service(workflow.getAppId(), workflowPhase.getServiceId())) {
        if (createPrimaryPhase) {
          if (isBlank(workflowPhase.getName())) {
            workflowPhase.setName(K8S_PRIMARY_PHASE_NAME);
          }
          addK8sRollingWorkflowPhaseSteps(workflowPhase);
        } else if (createCanaryPhase) {
          if (isBlank(workflowPhase.getName())) {
            workflowPhase.setName(K8S_CANARY_PHASE_NAME);
          }
          addK8sCanaryWorkflowPhaseSteps(workflowPhase);
        } else {
          addK8sEmptyPhaseStep(workflowPhase);
        }

        workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(workflowPhase);
        canaryOrchestrationWorkflow.getWorkflowPhases().add(workflowPhase);

        WorkflowPhase rollbackWorkflowPhase = createRollbackPhase(workflowPhase);

        if (createPrimaryPhase) {
          addK8sRollingRollbackWorkflowPhaseSteps(rollbackWorkflowPhase);
        } else if (createCanaryPhase) {
          addK8sCanaryRollbackWorkflowPhaseSteps(rollbackWorkflowPhase);
        } else {
          addK8sEmptyRollbackPhaseStep(rollbackWorkflowPhase);
        }

        workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(rollbackWorkflowPhase);
        canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
      } else {
        generateNewWorkflowPhaseSteps(workflow.getAppId(), workflow.getEnvId(), workflowPhase, serviceRepeat,
            orchestrationWorkflow.getOrchestrationWorkflowType(), workflow.getCreationFlags());
        workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(workflowPhase);
        canaryOrchestrationWorkflow.getWorkflowPhases().add(workflowPhase);

        WorkflowPhase rollbackWorkflowPhase = generateRollbackWorkflowPhase(workflow.getAppId(), workflowPhase,
            !serviceRepeat, orchestrationWorkflow.getOrchestrationWorkflowType(), workflow.getCreationFlags());
        workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(rollbackWorkflowPhase);
        canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
      }
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)
        || orchestrationWorkflow.getOrchestrationWorkflowType().equals(ROLLING)
        || orchestrationWorkflow.getOrchestrationWorkflowType().equals(BLUE_GREEN)) {
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
      generateNewWorkflowPhaseSteps(workflow.getAppId(), workflow.getEnvId(), workflowPhase, false,
          orchestrationWorkflow.getOrchestrationWorkflowType(), workflow.getCreationFlags());

      workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(workflowPhase);
      canaryOrchestrationWorkflow.getWorkflowPhases().add(workflowPhase);

      WorkflowPhase rollbackWorkflowPhase = generateRollbackWorkflowPhase(workflow.getAppId(), workflowPhase, true,
          orchestrationWorkflow.getOrchestrationWorkflowType(), workflow.getCreationFlags());
      workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(rollbackWorkflowPhase);
      canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BUILD)) {
      BuildWorkflow buildWorkflow = (BuildWorkflow) orchestrationWorkflow;
      workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(workflowPhase);
      generateNewWorkflowPhaseStepsForArtifactCollection(workflowPhase);
      buildWorkflow.getWorkflowPhases().add(workflowPhase);
    }
  }

  @Override
  @ValidationGroups(Update.class)
  public WorkflowPhase updateWorkflowPhase(
      @NotEmpty String appId, @NotEmpty String workflowId, @Valid WorkflowPhase workflowPhase) {
    if (workflowPhase.isRollback()
        || workflowPhase.getPhaseSteps().stream().anyMatch(
               phaseStep -> phaseStep.isRollback() || phaseStep.getSteps().stream().anyMatch(GraphNode::isRollback))) {
      // This might seem as user error, but since this is controlled from the our UI lets get alerted for it
      throw new InvalidRequestException("The direct workflow phase should not have rollback flag set!", USER_SRE);
    }

    Workflow workflow = readWorkflow(appId, workflowId);
    if (workflow == null) {
      throw new InvalidArgumentsException(Pair.of("application", appId), Pair.of("workflow", workflowId),
          new ExplanationException("This might be caused from someone else deleted "
                  + "the application and/or the workflow while you worked on it.",
              MOVE_TO_THE_PARENT_OBJECT));
    }
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow, USER);

    if (orchestrationWorkflow.getWorkflowPhaseIdMap().get(workflowPhase.getUuid()) == null) {
      throw new InvalidArgumentsException(Pair.of("workflow", workflowId), Pair.of("workflowPhase", appId),
          new ExplanationException("This might be caused from someone else modified "
                  + "the workflow resulting in removing the phase that you worked on.",
              MOVE_TO_THE_PARENT_OBJECT));
    }

    String serviceId = workflowPhase.getServiceId();
    String infraMappingId = workflowPhase.getInfraMappingId();
    if (!orchestrationWorkflow.getOrchestrationWorkflowType().equals(BUILD)) {
      Service service = serviceResourceService.get(appId, workflowPhase.getServiceId(), false);
      if (service == null && !workflowPhase.checkServiceTemplatized()) {
        throw new InvalidRequestException("Service [" + workflowPhase.getServiceId() + "] does not exist", USER);
      }
      InfrastructureMapping infrastructureMapping = null;
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
      }
      if (infrastructureMapping != null) {
        setCloudProviderForPhase(infrastructureMapping, workflowPhase);
      }

      WorkflowPhase rollbackWorkflowPhase =
          orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());
      if (rollbackWorkflowPhase != null) {
        rollbackWorkflowPhase.setServiceId(serviceId);
        rollbackWorkflowPhase.setInfraMappingId(infraMappingId);
        if (infrastructureMapping != null) {
          setCloudProviderForPhase(infrastructureMapping, rollbackWorkflowPhase);
        }
        orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
      }
    }
    boolean found = false;
    boolean inframappingChanged = false;
    String oldInfraMappingId = null;
    String oldServiceId = null;
    for (int i = 0; i < orchestrationWorkflow.getWorkflowPhases().size(); i++) {
      WorkflowPhase oldWorkflowPhase = orchestrationWorkflow.getWorkflowPhases().get(i);
      if (oldWorkflowPhase.getUuid().equals(workflowPhase.getUuid())) {
        oldInfraMappingId = oldWorkflowPhase.getInfraMappingId();
        oldServiceId = oldWorkflowPhase.getServiceId();
        orchestrationWorkflow.getWorkflowPhases().remove(i);
        orchestrationWorkflow.getWorkflowPhases().add(i, workflowPhase);
        orchestrationWorkflow.getWorkflowPhaseIdMap().put(workflowPhase.getUuid(), workflowPhase);
        found = true;
        // Update the workflow phase
        workflowServiceTemplateHelper.updateLinkedWorkflowPhaseTemplate(workflowPhase, oldWorkflowPhase);
        break;
      }
    }
    if (!BUILD.equals(orchestrationWorkflow.getOrchestrationWorkflowType())) {
      workflowServiceHelper.validateServiceCompatibility(appId, serviceId, oldServiceId);
      if (!workflowPhase.checkInfraTemplatized()) {
        if (!infraMappingId.equals(oldInfraMappingId)) {
          inframappingChanged = true;
        }
      }
      // Propagate template expressions to workflow level
      if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)
          || orchestrationWorkflow.getOrchestrationWorkflowType().equals(ROLLING)
          || orchestrationWorkflow.getOrchestrationWorkflowType().equals(BLUE_GREEN)) {
        WorkflowServiceTemplateHelper.setTemplateExpresssionsFromPhase(workflow, workflowPhase);
      } else {
        WorkflowServiceTemplateHelper.validateTemplateExpressions(workflowPhase.getTemplateExpressions());
      }
    }

    if (!found) {
      throw new InvalidRequestException("No matching Workflow Phase", USER);
    }

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow, inframappingChanged, false, false)
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

  @Override
  public WorkflowPhase updateWorkflowPhaseRollback(
      String appId, String workflowId, String phaseId, WorkflowPhase rollbackWorkflowPhase) {
    if (!rollbackWorkflowPhase.isRollback()
        || rollbackWorkflowPhase.getPhaseSteps().stream().anyMatch(phaseStep
               -> !phaseStep.isRollback() || phaseStep.getSteps().stream().anyMatch(step -> !step.isRollback()))) {
      // This might seem as user error, but since this is controlled from the our UI lets get alerted for it
      throw new InvalidRequestException("The rollback workflow phase should have rollback flag set!", USER_SRE);
    }

    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();

    validateWorkflowPhase(phaseId, orchestrationWorkflow);

    WorkflowPhase oldRollbackWorkflowPhase = orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseId);
    workflowServiceTemplateHelper.updateLinkedWorkflowPhaseTemplate(rollbackWorkflowPhase, oldRollbackWorkflowPhase);

    orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(phaseId, rollbackWorkflowPhase);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
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
    updateWorkflow(workflow, orchestrationWorkflow);
  }

  private void validateWorkflowPhase(String phaseId, CanaryOrchestrationWorkflow orchestrationWorkflow) {
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow, USER);
    notNullCheck("WorkflowPhase", orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId), USER);
  }

  @Override
  public GraphNode updateGraphNode(String appId, String workflowId, String subworkflowId, GraphNode node) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow, USER);

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
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getGraph()
        .getSubworkflows()
        .get(subworkflowId)
        .getNodes()
        .stream()
        .filter(n -> node.getId().equals(n.getId()))
        .findFirst()
        .get();
  }

  @Override
  public Workflow cloneWorkflow(String appId, Workflow originalWorkflow, Workflow workflow) {
    String accountId = appService.getAccountIdByAppId(workflow.getAppId());

    StaticLimitCheckerWithDecrement checker = (StaticLimitCheckerWithDecrement) limitCheckerFactory.getInstance(
        new Action(accountId, ActionType.CREATE_WORKFLOW));

    return LimitEnforcementUtils.withLimitCheck(
        checker, () -> cloneWorkflowInternal(appId, originalWorkflow, workflow));
  }

  private Workflow cloneWorkflowInternal(String appId, Workflow originalWorkflow, Workflow workflow) {
    Workflow clonedWorkflow = cloneWorkflow(workflow, originalWorkflow);

    clonedWorkflow.setDefaultVersion(1);
    String key = wingsPersistence.save(clonedWorkflow);
    entityVersionService.newEntityVersion(
        appId, WORKFLOW, key, clonedWorkflow.getName(), EntityVersion.ChangeType.CREATED, workflow.getNotes());

    Workflow savedWorkflow = readWorkflow(appId, key, clonedWorkflow.getDefaultVersion());

    if (originalWorkflow.getOrchestrationWorkflow() != null) {
      savedWorkflow.setOrchestrationWorkflow(originalWorkflow.getOrchestrationWorkflow().cloneInternal());
    }
    return updateWorkflow(savedWorkflow, savedWorkflow.getOrchestrationWorkflow(), false, false, false, true);
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
    return updateWorkflow(savedWorkflow, savedWorkflow.getOrchestrationWorkflow(), false, true, true, true);
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
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow);

    orchestrationWorkflow.setNotificationRules(notificationRules);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getNotificationRules();
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
    notNullCheck("Workflow was deleted", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow, USER);

    orchestrationWorkflow.setFailureStrategies(failureStrategies);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getFailureStrategies();
  }

  @Override
  public List<Variable> updateUserVariables(String appId, String workflowId, List<Variable> userVariables) {
    if (isNotEmpty(userVariables)) {
      userVariables.forEach(variable -> ManagerExpressionEvaluator.isValidVariableName(variable.getName()));
      validateWorkflowVariables(userVariables);
    }
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("Workflow was deleted", workflow, USER);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow, USER);

    orchestrationWorkflow.setUserVariables(userVariables);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getUserVariables();
  }

  private void validateWorkflowVariables(List<Variable> userVariables) {
    Set<String> variableNames = new HashSet<>();
    for (Variable variable : userVariables) {
      if (variable.isFixed()) {
        if (isBlank(variable.getValue())) {
          throw new InvalidRequestException(
              "Workflow Variable value is mandatory for Fixed Variable Name [" + variable.getName() + "]", USER);
        }
      }
      if (!variableNames.add(variable.getName())) {
        throw new InvalidRequestException("Duplicate variable names are not allowed.", USER);
      }
    }
  }

  @Override
  public DeploymentMetadata fetchDeploymentMetadata(String appId, Workflow workflow,
      Map<String, String> workflowVariables, List<String> artifactRequiredServiceIds, List<String> envIds,
      Include... includes) {
    DeploymentMetadataBuilder deploymentMetadataBuilder = DeploymentMetadata.builder();

    List<Include> includeList = isEmpty(includes) ? Arrays.asList(Include.values()) : Arrays.asList(includes);

    if (includeList.contains(Include.ARTIFACT_SERVICE)) {
      if (artifactRequiredServiceIds == null) {
        artifactRequiredServiceIds = new ArrayList<>();
      }
      fetchArtifactNeededServiceIds(appId, workflow, workflowVariables, artifactRequiredServiceIds);
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

      updateRequiredEntityTypes(
          appId, null, canaryOrchestrationWorkflow.getPreDeploymentSteps(), requiredEntityTypes, null, null);

      boolean preDeploymentStepNeededArtifact = requiredEntityTypes.contains(EntityType.ARTIFACT);

      Set<EntityType> phaseRequiredEntityTypes = canaryOrchestrationWorkflow.getWorkflowPhases()
                                                     .stream()
                                                     .flatMap(phase
                                                         -> updateRequiredEntityTypes(appId, phase, workflowVariables,
                                                             artifactNeededServiceIds, preDeploymentStepNeededArtifact)
                                                                .stream())
                                                     .collect(Collectors.toSet());

      requiredEntityTypes.addAll(phaseRequiredEntityTypes);

      Set<EntityType> rollbackRequiredEntityTypes =
          ((CanaryOrchestrationWorkflow) orchestrationWorkflow)
              .getRollbackWorkflowPhaseIdMap()
              .values()
              .stream()
              .flatMap(phase
                  -> updateRequiredEntityTypes(
                      appId, phase, workflowVariables, artifactNeededServiceIds, preDeploymentStepNeededArtifact)
                         .stream())
              .collect(Collectors.toSet());

      if (!requiredEntityTypes.contains(ARTIFACT) && rollbackRequiredEntityTypes.contains(ARTIFACT)) {
        logger.warn(
            "Phase Step do not need artifact. However, Rollback steps needed artifact for the workflow {} of the app {}",
            workflow.getUuid(), appId);
      }
      requiredEntityTypes.addAll(rollbackRequiredEntityTypes);
    }
  }

  private Set<EntityType> updateRequiredEntityTypes(String appId, WorkflowPhase workflowPhase,
      Map<String, String> workflowVaraibles, List<String> artifactNeededServiceIds,
      boolean preDeploymentStepNeededArtifact) {
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
      updateRequiredEntityTypes(appId, serviceId, phaseStep, requiredEntityTypes, workflowPhase, workflowVaraibles);
    }

    if (requiredEntityTypes.contains(EntityType.ARTIFACT)) {
      if (serviceId != null && !artifactNeededServiceIds.contains(serviceId)) {
        artifactNeededServiceIds.add(serviceId);
      }
    }

    return requiredEntityTypes;
  }

  private void updateRequiredEntityTypes(String appId, String serviceId, PhaseStep phaseStep,
      Set<EntityType> requiredEntityTypes, WorkflowPhase workflowPhase, Map<String, String> workflowVariables) {
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

      if ("COMMAND".equals(step.getType())) {
        if (serviceId != null) {
          boolean serviceExists = serviceResourceService.exists(appId, serviceId);
          if (serviceExists) {
            ServiceCommand command = serviceResourceService.getCommandByName(
                appId, serviceId, (String) step.getProperties().get("commandName"));
            if (command != null && command.getCommand() != null && command.getCommand().isArtifactNeeded()) {
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
      } else if (workflowPhase != null && HELM.equals(workflowPhase.getDeploymentType())
          && StateType.HELM_DEPLOY.name().equals(step.getType())) {
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
      } else if (workflowPhase != null && k8sV2ArtifactNeededStateTypes.contains(step.getType())) {
        if (artifactStreamService.artifactStreamsExistForService(appId, serviceId)) {
          artifactNeeded = true;
          break;
        }

        String infraMappingId = getInfraMappingId(workflowPhase, workflowVariables);
        if (isNotEmpty(infraMappingId) && !matchesVariablePattern(infraMappingId)
            && k8sStateHelper.doManifestsUseArtifact(appId, infraMappingId)) {
          artifactNeeded = true;
          break;
        }
      }
    }

    if (artifactNeeded) {
      requiredEntityTypes.add(ARTIFACT);
      phaseStep.setArtifactNeeded(true);
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

  private boolean isArtifactNeeded(Object... args) {
    return Arrays.stream(args).anyMatch(arg
        -> arg != null && (((String) arg).contains("${artifact.") || ((String) arg).contains("${ARTIFACT_FILE_NAME}")));
  }

  private void generateNewWorkflowPhaseSteps(String appId, String envId, WorkflowPhase workflowPhase,
      boolean serviceRepeat, OrchestrationWorkflowType orchestrationWorkflowType, WorkflowCreationFlags creationFlags) {
    DeploymentType deploymentType = workflowPhase.getDeploymentType();
    if (deploymentType == ECS) {
      if (orchestrationWorkflowType == OrchestrationWorkflowType.BLUE_GREEN) {
        if (creationFlags != null && creationFlags.isEcsBgDnsType()) {
          workflowServiceHelper.generateNewWorkflowPhaseStepsForECSBlueGreenRoute53(
              appId, workflowPhase, !serviceRepeat);
        } else {
          workflowServiceHelper.generateNewWorkflowPhaseStepsForECSBlueGreen(appId, workflowPhase, !serviceRepeat);
        }
      } else {
        workflowServiceHelper.generateNewWorkflowPhaseStepsForECS(
            appId, workflowPhase, !serviceRepeat, orchestrationWorkflowType);
      }
    } else if (deploymentType == KUBERNETES) {
      if (orchestrationWorkflowType == OrchestrationWorkflowType.BLUE_GREEN) {
        workflowServiceHelper.generateNewWorkflowPhaseStepsForKubernetesBlueGreen(appId, workflowPhase, !serviceRepeat);
      } else {
        workflowServiceHelper.generateNewWorkflowPhaseStepsForKubernetes(
            appId, workflowPhase, !serviceRepeat, orchestrationWorkflowType);
      }
    } else if (deploymentType == HELM) {
      workflowServiceHelper.generateNewWorkflowPhaseStepsForHelm(appId, workflowPhase, !serviceRepeat);
    } else if (deploymentType == AWS_CODEDEPLOY) {
      workflowServiceHelper.generateNewWorkflowPhaseStepsForAWSCodeDeploy(appId, workflowPhase);
    } else if (deploymentType == AWS_LAMBDA) {
      workflowServiceHelper.generateNewWorkflowPhaseStepsForAWSLambda(appId, envId, workflowPhase);
    } else if (deploymentType == AMI) {
      if (BLUE_GREEN.equals(orchestrationWorkflowType)) {
        workflowServiceHelper.generateNewWorkflowPhaseStepsForAWSAmiBlueGreen(appId, workflowPhase, !serviceRepeat);
      } else {
        workflowServiceHelper.generateNewWorkflowPhaseStepsForAWSAmi(appId, workflowPhase, !serviceRepeat);
      }
    } else if (deploymentType == PCF) {
      if (orchestrationWorkflowType == OrchestrationWorkflowType.BLUE_GREEN) {
        workflowServiceHelper.generateNewWorkflowPhaseStepsForPCFBlueGreen(appId, workflowPhase, !serviceRepeat);
      } else {
        workflowServiceHelper.generateNewWorkflowPhaseStepsForPCF(
            appId, envId, workflowPhase, !serviceRepeat, orchestrationWorkflowType);
      }
    } else {
      workflowServiceHelper.generateNewWorkflowPhaseStepsForSSH(appId, workflowPhase, orchestrationWorkflowType);
    }
  }

  private WorkflowPhase generateRollbackWorkflowPhase(String appId, WorkflowPhase workflowPhase,
      boolean serviceSetupRequired, OrchestrationWorkflowType orchestrationWorkflowType,
      WorkflowCreationFlags creationFlags) {
    DeploymentType deploymentType = workflowPhase.getDeploymentType();
    if (deploymentType == ECS) {
      if (orchestrationWorkflowType == OrchestrationWorkflowType.BLUE_GREEN) {
        if (creationFlags != null && creationFlags.isEcsBgDnsType()) {
          return workflowServiceHelper.generateRollbackWorkflowPhaseForEcsBlueGreenRoute53(
              appId, workflowPhase, orchestrationWorkflowType);
        } else {
          return workflowServiceHelper.generateRollbackWorkflowPhaseForEcsBlueGreen(
              appId, workflowPhase, orchestrationWorkflowType);
        }
      } else {
        return workflowServiceHelper.generateRollbackWorkflowPhaseForEcs(
            appId, workflowPhase, orchestrationWorkflowType);
      }
    } else if (deploymentType == KUBERNETES) {
      if (orchestrationWorkflowType == OrchestrationWorkflowType.BLUE_GREEN) {
        return workflowServiceHelper.generateRollbackWorkflowPhaseForKubernetesBlueGreen(
            workflowPhase, serviceSetupRequired);
      } else {
        return workflowServiceHelper.generateRollbackWorkflowPhaseForKubernetes(workflowPhase, serviceSetupRequired);
      }
    } else if (deploymentType == AWS_CODEDEPLOY) {
      return workflowServiceHelper.generateRollbackWorkflowPhaseForAwsCodeDeploy(workflowPhase);
    } else if (deploymentType == AWS_LAMBDA) {
      return workflowServiceHelper.generateRollbackWorkflowPhaseForAwsLambda(workflowPhase);
    } else if (deploymentType == AMI) {
      if (BLUE_GREEN.equals(orchestrationWorkflowType)) {
        return workflowServiceHelper.generateRollbackWorkflowPhaseForAwsAmiBlueGreen(workflowPhase);
      } else {
        return workflowServiceHelper.generateRollbackWorkflowPhaseForAwsAmi(workflowPhase);
      }
    } else if (deploymentType == HELM) {
      return workflowServiceHelper.generateRollbackWorkflowPhaseForHelm(workflowPhase);
    } else if (deploymentType == PCF) {
      if (orchestrationWorkflowType == OrchestrationWorkflowType.BLUE_GREEN) {
        return workflowServiceHelper.generateRollbackWorkflowPhaseForPCFBlueGreen(workflowPhase, serviceSetupRequired);
      } else {
        return workflowServiceHelper.generateRollbackWorkflowPhaseForPCF(workflowPhase);
      }
    } else {
      return workflowServiceHelper.generateRollbackWorkflowPhaseForSSH(appId, workflowPhase);
    }
  }

  private void createDefaultNotificationRule(Workflow workflow) {
    Application app = appService.get(workflow.getAppId());
    Account account = accountService.get(app.getAccountId());
    List<NotificationGroup> notificationGroups = getNotificationGroupForDefaultNotificationRule(app.getAccountId());

    if (isEmpty(notificationGroups)) {
      logger.warn("Default notification group not created for account {}. Ignoring adding notification group",
          account.getAccountName());
      return;
    }
    List<ExecutionStatus> conditions = asList(ExecutionStatus.FAILED);
    NotificationRule notificationRule = aNotificationRule()
                                            .withConditions(conditions)
                                            .withExecutionScope(ExecutionScope.WORKFLOW)
                                            .withNotificationGroups(notificationGroups)
                                            .build();

    List<NotificationRule> notificationRules = asList(notificationRule);
    workflow.getOrchestrationWorkflow().setNotificationRules(notificationRules);
  }

  /**
   * This method will return defaultNotificationGroup for account. If default notification group is not set,
   * then "Account Administrator" notification group would be returned.

   * @param accountId
   * @return
   */
  private List<NotificationGroup> getNotificationGroupForDefaultNotificationRule(String accountId) {
    List<NotificationGroup> notificationGroups = notificationSetupService.listDefaultNotificationGroup(accountId);

    if (CollectionUtils.isEmpty(notificationGroups)) {
      // TODO: We should be able to get Logged On User Admin role dynamically
      notificationGroups =
          notificationSetupService.listNotificationGroups(accountId, RoleType.ACCOUNT_ADMIN.getDisplayName());
    }

    return notificationGroups;
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
      case MULTI_SERVICE: {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
        canaryOrchestrationWorkflow.setFailureStrategies(failureStrategies);
        break;
      }
      default: { noop(); }
    }
  }

  private void validateOrchestrationWorkflow(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();

    if (orchestrationWorkflow == null) {
      return;
    }

    if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)
        || orchestrationWorkflow.getOrchestrationWorkflowType().equals(ROLLING)
        || orchestrationWorkflow.getOrchestrationWorkflowType().equals(BLUE_GREEN)) {
      if (!orchestrationWorkflow.isServiceTemplatized()) {
        notNullCheck("Invalid serviceId", workflow.getServiceId(), USER);
      }

      if (orchestrationWorkflow.isInfraMappingTemplatized()) {
        return;
      }

      notNullCheck("Invalid inframappingId", workflow.getInfraMappingId(), USER);

      String infraMappingId = workflow.getInfraMappingId();
      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(workflow.getAppId(), infraMappingId);

      notNullCheck("Invalid inframapping", infrastructureMapping, USER);

      if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(ROLLING)) {
        if (!(InfrastructureMappingType.AWS_SSH.name().equals(infrastructureMapping.getInfraMappingType())
                || InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name().equals(
                       infrastructureMapping.getInfraMappingType())
                || workflowServiceHelper.isK8sV2Service(infrastructureMapping.getAppId(), workflow.getServiceId()))) {
          throw new InvalidRequestException(
              "Requested Service/InfrastructureType is not supported using Rolling Deployment", USER);
        }
      } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BLUE_GREEN)) {
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
      List<String> appsIds =
          appService.getAppsByAccountId(accountId).stream().map(app -> app.getAppId()).collect(toList());

      if (!appsIds.isEmpty()) {
        workflows = listWorkflows(aPageRequest()
                                      .withLimit(PageRequest.UNLIMITED)
                                      .addFilter(APP_ID_KEY, Operator.IN, appsIds.toArray())
                                      .build())
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
    return (Rejection) () -> msg;
  }

  @Override
  public List<InstanceElement> getDeployedNodes(String appId, String workflowId) {
    int offSet = 0;
    final PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                           .addFilter("appId", Operator.EQ, appId)
                                                           .addFilter("workflowId", Operator.EQ, workflowId)
                                                           .addFilter("status", Operator.EQ, SUCCESS)
                                                           .addOrder(WorkflowExecution.CREATED_AT_KEY, OrderType.DESC)
                                                           .withOffset(String.valueOf(offSet))
                                                           .withLimit(String.valueOf(PageRequest.DEFAULT_PAGE_SIZE))
                                                           .build();

    PageResponse<WorkflowExecution> workflowExecutions;
    List<InstanceElement> instanceElements = new ArrayList<>();
    do {
      workflowExecutions = workflowExecutionService.listExecutions(pageRequest, false);

      if (isEmpty(workflowExecutions)) {
        logger.info("Did not find a successful execution for {}. ", workflowId);
        return singletonList(
            InstanceElement.Builder.anInstanceElement()
                .withHostName(
                    "No succesful workflow execution found for this workflow. Please run the workflow to get deployed nodes")
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
      offSet = offSet + PageRequest.DEFAULT_PAGE_SIZE;
      pageRequest.setOffset(String.valueOf(offSet));
    } while (workflowExecutions.size() >= PageRequest.DEFAULT_PAGE_SIZE);

    logger.info("No nodes were found in any execution for workflow {}", workflowId);
    return singletonList(
        InstanceElement.Builder.anInstanceElement()
            .withHostName(
                "No workflow execution found with deployed nodes for this workflow. Please run the workflow to get deployed nodes")
            .build());
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
    Validator.notNullCheck("Workflow was deleted", workflow, WingsException.USER);

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
    final PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                           .addFilter("appId", Operator.EQ, appId)
                                                           .addFilter("workflowId", Operator.EQ, workflowId)
                                                           .addFilter("status", Operator.EQ, ExecutionStatus.SUCCESS)
                                                           .addOrder(WorkflowExecution.CREATED_AT_KEY, OrderType.DESC)
                                                           .build();
    if (!isEmpty(serviceId)) {
      pageRequest.addFilter("serviceIds", Operator.CONTAINS, serviceId);
    }
    final PageResponse<WorkflowExecution> workflowExecutions =
        workflowExecutionService.listExecutions(pageRequest, false, true, false, false);
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
    Validator.notNullCheck("Workflow does not exist", workflow, USER);
    return workflow.getName();
  }

  public List<String> obtainWorkflowNamesReferencedByEnvironment(String appId, String envId) {
    List<String> referencedWorkflows = new ArrayList<>();
    try (HIterator<Workflow> workflowHIterator =
             new HIterator<>(wingsPersistence.createQuery(Workflow.class).filter(APP_ID_KEY, appId).fetch())) {
      if (workflowHIterator != null) {
        while (workflowHIterator.hasNext()) {
          Workflow workflow = workflowHIterator.next();
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
