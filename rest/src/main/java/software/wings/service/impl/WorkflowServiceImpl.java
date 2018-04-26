package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.govern.Switch.noop;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
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
import static software.wings.beans.EntityType.ARTIFACT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ErrorCode.WORKFLOW_EXECUTION_IN_PROGRESS;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.OrchestrationWorkflowType.BASIC;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.OrchestrationWorkflowType.CANARY;
import static software.wings.beans.OrchestrationWorkflowType.MULTI_SERVICE;
import static software.wings.beans.OrchestrationWorkflowType.ROLLING;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.AMI_AUTOSCALING_GROUP_SETUP;
import static software.wings.beans.PhaseStepType.AMI_DEPLOY_AUTOSCALING_GROUP;
import static software.wings.beans.PhaseStepType.CLUSTER_SETUP;
import static software.wings.beans.PhaseStepType.COLLECT_ARTIFACT;
import static software.wings.beans.PhaseStepType.CONTAINER_DEPLOY;
import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;
import static software.wings.beans.PhaseStepType.DEPLOY_AWSCODEDEPLOY;
import static software.wings.beans.PhaseStepType.DEPLOY_AWS_LAMBDA;
import static software.wings.beans.PhaseStepType.DEPLOY_SERVICE;
import static software.wings.beans.PhaseStepType.DE_PROVISION_NODE;
import static software.wings.beans.PhaseStepType.DISABLE_SERVICE;
import static software.wings.beans.PhaseStepType.ENABLE_SERVICE;
import static software.wings.beans.PhaseStepType.PCF_RESIZE;
import static software.wings.beans.PhaseStepType.PCF_ROUTE_SWAP;
import static software.wings.beans.PhaseStepType.PCF_SETUP;
import static software.wings.beans.PhaseStepType.PREPARE_STEPS;
import static software.wings.beans.PhaseStepType.PROVISION_NODE;
import static software.wings.beans.PhaseStepType.STOP_SERVICE;
import static software.wings.beans.PhaseStepType.VERIFY_SERVICE;
import static software.wings.beans.PhaseStepType.WRAP_UP;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.common.Constants.ARTIFACT_TYPE;
import static software.wings.common.Constants.ENTITY_TYPE;
import static software.wings.common.Constants.WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.exception.HintException.MOVE_TO_THE_PARENT_OBJECT;
import static software.wings.exception.WingsException.USER;
import static software.wings.exception.WingsException.USER_SRE;
import static software.wings.sm.StateMachineExecutionSimulator.populateRequiredEntityTypesByAccessType;
import static software.wings.sm.StateType.ARTIFACT_CHECK;
import static software.wings.sm.StateType.ARTIFACT_COLLECTION;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_DEPLOY;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_ROLLBACK;
import static software.wings.sm.StateType.AWS_AMI_SERVICE_SETUP;
import static software.wings.sm.StateType.AWS_CLUSTER_SETUP;
import static software.wings.sm.StateType.AWS_CODEDEPLOY_ROLLBACK;
import static software.wings.sm.StateType.AWS_CODEDEPLOY_STATE;
import static software.wings.sm.StateType.AWS_LAMBDA_ROLLBACK;
import static software.wings.sm.StateType.AWS_LAMBDA_STATE;
import static software.wings.sm.StateType.AWS_NODE_SELECT;
import static software.wings.sm.StateType.COMMAND;
import static software.wings.sm.StateType.DC_NODE_SELECT;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_ROLLBACK;
import static software.wings.sm.StateType.ECS_SERVICE_SETUP;
import static software.wings.sm.StateType.ELASTIC_LOAD_BALANCER;
import static software.wings.sm.StateType.GCP_CLUSTER_SETUP;
import static software.wings.sm.StateType.HELM_DEPLOY;
import static software.wings.sm.StateType.HELM_ROLLBACK;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY;
import static software.wings.sm.StateType.KUBERNETES_DEPLOY_ROLLBACK;
import static software.wings.sm.StateType.KUBERNETES_SETUP;
import static software.wings.sm.StateType.KUBERNETES_SETUP_ROLLBACK;
import static software.wings.sm.StateType.NEW_RELIC;
import static software.wings.sm.StateType.PCF_ROLLBACK;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.ROLLING_NODE_SELECT;
import static software.wings.sm.StateType.values;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentType;
import software.wings.app.StaticConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.BuildWorkflow;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureStrategy;
import software.wings.beans.FailureType;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.Graph;
import software.wings.beans.GraphNode;
import software.wings.beans.GraphNode.GraphNodeBuilder;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.MultiServiceOrchestrationWorkflow;
import software.wings.beans.NameValuePair;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.PhysicalInfrastructureMappingBase;
import software.wings.beans.Pipeline;
import software.wings.beans.RepairActionCode;
import software.wings.beans.RoleType;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.stats.CloneMetadata;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.ExplanationException;
import software.wings.exception.InvalidArgumentsException;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.expression.ExpressionEvaluator;
import software.wings.scheduler.PruneEntityJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.newrelic.NewRelicMetricNames;
import software.wings.service.impl.newrelic.NewRelicMetricNames.WorkflowInfo;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.MetricDataAnalysisService;
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
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.sm.states.AwsCodeDeployState;
import software.wings.sm.states.ElasticLoadBalancerState.Operation;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilCategory;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.validation.Update;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
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
  @Inject private PluginManager pluginManager;
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
  @Inject private MetricDataAnalysisService metricDataAnalysisService;
  @Inject private TriggerService triggerService;
  @Inject private EnvironmentService environmentService;
  @Inject private WorkflowServiceHelper workflowServiceHelper;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

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

    boolean filterForWorkflow = isNotBlank(workflowId);
    boolean filterForPhase = filterForWorkflow && isNotBlank(phaseId);
    Workflow workflow = null;
    Map<StateTypeScope, List<Stencil>> mapByScope = null;
    WorkflowPhase workflowPhase = null;
    if (filterForWorkflow) {
      workflow = readWorkflow(appId, workflowId);
      if (workflow == null) {
        throw new InvalidRequestException("Workflow does not exist", USER);
      }
      if (filterForPhase) {
        if (workflow != null) {
          OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
          if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
            workflowPhase = ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhaseIdMap().get(phaseId);
          }
        }
        if (workflowPhase == null) {
          throw new InvalidRequestException(
              "Worflow Phase  not associated with Workflow [" + workflow.getName() + "]", USER);
        }
        String serviceId = workflowPhase.getServiceId();
        if (serviceId != null) {
          mapByScope = stencilsMap.entrySet().stream().collect(toMap(Entry::getKey,
              stateTypeScopeListEntry
              -> stencilPostProcessor.postProcess(stateTypeScopeListEntry.getValue(), appId, serviceId)));
        } else {
          mapByScope = stencilsMap.entrySet().stream().collect(toMap(Entry::getKey,
              stateTypeScopeListEntry -> stencilPostProcessor.postProcess(stateTypeScopeListEntry.getValue(), appId)));
        }
      } else {
        // For workflow, anyways skipping the command names. So, sending service Id as "NONE" to make sure that
        // EnumDataProvider can ignore that.
        mapByScope = stencilsMap.entrySet().stream().collect(toMap(Entry::getKey,
            stateTypeScopeListEntry
            -> stencilPostProcessor.postProcess(stateTypeScopeListEntry.getValue(), appId, "NONE")));
      }
    } else {
      mapByScope = stencilsMap.entrySet().stream().collect(toMap(Entry::getKey,
          stateTypeScopeListEntry -> stencilPostProcessor.postProcess(stateTypeScopeListEntry.getValue(), appId)));
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
        if (workflowPhase != null && workflowPhase.getInfraMappingId() != null
            && !workflowPhase.checkInfraTemplatized()) {
          InfrastructureMapping infrastructureMapping =
              infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
          predicate = stencil -> stencil.matches(infrastructureMapping);
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
    return maps;
  }

  private Map<StateTypeScope, List<StateTypeDescriptor>> loadStateTypes(String accountId) {
    if (cachedStencils != null) {
      return cachedStencils;
    }

    List<StateTypeDescriptor> stencils = new ArrayList<StateTypeDescriptor>();
    Arrays.stream(values()).forEach(state -> stencils.add(state));

    List<StateTypeDescriptor> plugins = pluginManager.getExtensions(StateTypeDescriptor.class);
    stencils.addAll(plugins);

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

  @Override
  public String getHPAYamlStringWithCustomMetric(
      Integer minAutoscaleInstances, Integer maxAutoscaleInstances, Integer targetCpuUtilizationPercentage) {
    return workflowServiceHelper.getHPAYamlStringWithCustomMetric(
        minAutoscaleInstances, maxAutoscaleInstances, targetCpuUtilizationPercentage);
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
          logger.error("Failed to load Orchestration workflow {} ", workflow, e);
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
                  .build();

          workflow.setWorkflowExecutions(
              workflowExecutionService.listExecutions(workflowExecutionPageRequest, false, false, false, false)
                  .getResponse());
        } catch (Exception e) {
          logger.error("Failed to fetch recent executions for workflow {}", workflow, e);
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
  public Workflow readWorkflow(String appId, String workflowId, Integer version) {
    Workflow workflow = wingsPersistence.get(Workflow.class, appId, workflowId);
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
    StateMachine stateMachine = readStateMachine(
        workflow.getAppId(), workflow.getUuid(), version == null ? workflow.getDefaultVersion() : version);
    if (stateMachine != null) {
      workflow.setOrchestrationWorkflow(stateMachine.getOrchestrationWorkflow());
    }
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow != null) {
      orchestrationWorkflow.onLoad();
      workflow.setTemplatized(orchestrationWorkflow.isTemplatized());
    }
    populateServices(workflow);
  }

  private void populateServices(Workflow workflow) {
    if (workflow == null) {
      return;
    }

    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow == null || orchestrationWorkflow.getServiceIds() == null) {
      return;
    }
    List<Service> services = orchestrationWorkflow.getServiceIds()
                                 .stream()
                                 .map(serviceId -> serviceResourceService.get(workflow.getAppId(), serviceId, false))
                                 .filter(Objects::nonNull)
                                 .collect(toList());

    workflow.setServices(services);
    workflow.setTemplatizedServiceIds(orchestrationWorkflow.getTemplatizedServiceIds());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Workflow createWorkflow(Workflow workflow) {
    validateBasicOrRollingWorkflow(workflow);
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    workflow.setDefaultVersion(1);
    String key = wingsPersistence.save(workflow);
    if (orchestrationWorkflow != null) {
      if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(CANARY)
          || orchestrationWorkflow.getOrchestrationWorkflowType().equals(MULTI_SERVICE)) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
        if (isNotEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
          List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
          canaryOrchestrationWorkflow.setWorkflowPhases(new ArrayList<>());
          workflowPhases.forEach(workflowPhase -> attachWorkflowPhase(workflow, workflowPhase));
        }
      } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)
          || orchestrationWorkflow.getOrchestrationWorkflowType().equals(ROLLING)) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
        WorkflowPhase workflowPhase;
        if (isEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
          workflowPhase = aWorkflowPhase()
                              .withInfraMappingId(workflow.getInfraMappingId())
                              .withServiceId(workflow.getServiceId())
                              .withDaemonSet(isDaemonSet(workflow.getAppId(), workflow.getServiceId()))
                              .build();
          attachWorkflowPhase(workflow, workflowPhase);
        }
      } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BUILD)) {
        BuildWorkflow buildWorkflow = (BuildWorkflow) orchestrationWorkflow;

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
      workflowServiceHelper.transformEnvTemplateExpressions(workflow, orchestrationWorkflow);
      orchestrationWorkflow.onSave();

      updateRequiredEntityTypes(workflow.getAppId(), orchestrationWorkflow);
      StateMachine stateMachine = new StateMachine(workflow, workflow.getDefaultVersion(),
          ((CustomOrchestrationWorkflow) orchestrationWorkflow).getGraph(), stencilMap());
      stateMachine = wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
    }
    // create initial version
    entityVersionService.newEntityVersion(
        workflow.getAppId(), WORKFLOW, key, workflow.getName(), EntityVersion.ChangeType.CREATED, workflow.getNotes());

    Workflow newWorkflow = readWorkflow(workflow.getAppId(), key, workflow.getDefaultVersion());
    updateKeywords(newWorkflow);

    Workflow finalWorkflow1 = newWorkflow;
    executorService.submit(() -> notifyNewRelicMetricCollection(finalWorkflow1));

    executorService.submit(() -> {
      String accountId = appService.getAccountIdByAppId(workflow.getAppId());
      YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
      if (ygs != null) {
        List<GitFileChange> changeSet = new ArrayList<>();
        changeSet.add(entityUpdateService.getWorkflowGitSyncFile(accountId, workflow, ChangeType.ADD));
        yamlChangeSetService.saveChangeSet(ygs, changeSet);
      }
    });

    return newWorkflow;
  }

  private void updateKeywords(Workflow workflow) {
    List<String> keywords = workflowServiceHelper.getKeywords(workflow);
    wingsPersistence.update(wingsPersistence.createQuery(Workflow.class)
                                .filter(Constants.APP_ID, workflow.getAppId())
                                .filter(Constants.UUID, workflow.getUuid()),
        wingsPersistence.createUpdateOperations(Workflow.class).set("keywords", keywords));
    workflow.setKeywords(keywords);
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
    if (workflowServiceHelper.workflowHasSshInfraMapping(appId, canaryOrchestrationWorkflow)) {
      return ensureArtifactCheckInPreDeployment(canaryOrchestrationWorkflow);
    }
    return false;
  }

  private boolean ensureArtifactCheckInPreDeployment(CanaryOrchestrationWorkflow canaryOrchestrationWorkflow) {
    PhaseStep preDeploymentSteps = canaryOrchestrationWorkflow.getPreDeploymentSteps();
    if (preDeploymentSteps == null) {
      preDeploymentSteps = new PhaseStep();
      canaryOrchestrationWorkflow.setPreDeploymentSteps(preDeploymentSteps);
    }
    if (preDeploymentSteps.getSteps() == null) {
      preDeploymentSteps.setSteps(new ArrayList<>());
    }
    boolean artifactCheckFound =
        preDeploymentSteps.getSteps().stream().anyMatch(graphNode -> ARTIFACT_CHECK.name().equals(graphNode.getType()));
    if (artifactCheckFound) {
      return false;
    } else {
      preDeploymentSteps.getSteps().add(
          GraphNodeBuilder.aGraphNode().withType(ARTIFACT_CHECK.name()).withName("Artifact Check").build());
      return true;
    }
  }

  private void notifyNewRelicMetricCollection(Workflow workflow) {
    try {
      if (workflow.getOrchestrationWorkflow() == null
          || !(workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow)) {
        return;
      }

      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
          (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
      if (isEmpty(canaryOrchestrationWorkflow.getWorkflowPhases())) {
        return;
      }

      Map<String, NewRelicMetricNames> newRelicMetricNamesMap = new HashMap();
      String accountId = appService.getAccountIdByAppId(workflow.getAppId());
      for (WorkflowPhase workflowPhase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
        if (workflowPhase.getPhaseSteps() == null) {
          continue;
        }

        for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
          if (phaseStep.getSteps() == null) {
            continue;
          }
          for (GraphNode node : phaseStep.getSteps()) {
            if (!NEW_RELIC.name().equals(node.getType())) {
              continue;
            }

            String newRelicAppId = (String) node.getProperties().get("applicationId");
            String newRelicServerConfigId = (String) node.getProperties().get("analysisServerConfigId");
            if (newRelicMetricNamesMap.containsKey(newRelicAppId + "-" + newRelicServerConfigId)) {
              continue;
            }

            NewRelicMetricNames newRelicMetricNames =
                NewRelicMetricNames.builder()
                    .newRelicAppId(newRelicAppId)
                    .newRelicConfigId(newRelicServerConfigId)
                    .registeredWorkflows(Collections.singletonList(WorkflowInfo.builder()
                                                                       .accountId(accountId)
                                                                       .appId(workflow.getAppId())
                                                                       .workflowId(workflow.getUuid())
                                                                       .envId(workflow.getEnvId())
                                                                       .infraMappingId(workflow.getInfraMappingId())
                                                                       .build()))
                    .build();
            newRelicMetricNamesMap.put(newRelicAppId + "-" + newRelicServerConfigId, newRelicMetricNames);
          }
        }
      }

      if (newRelicMetricNamesMap.isEmpty()) {
        return;
      }

      for (NewRelicMetricNames newRelicMetricNames : newRelicMetricNamesMap.values()) {
        NewRelicMetricNames metricNames = metricDataAnalysisService.getMetricNames(
            newRelicMetricNames.getNewRelicAppId(), newRelicMetricNames.getNewRelicConfigId());
        if (metricNames == null) {
          metricDataAnalysisService.saveMetricNames(newRelicMetricNames);
          continue;
        }
        boolean foundWorkflowInfo = false;
        for (WorkflowInfo workflowInfo : metricNames.getRegisteredWorkflows()) {
          if (workflowInfo.getWorkflowId().equals(
                  newRelicMetricNames.getRegisteredWorkflows().get(0).getWorkflowId())) {
            foundWorkflowInfo = true;
            break;
          }
        }
        if (!foundWorkflowInfo) {
          metricDataAnalysisService.addMetricNamesWorkflowInfo(newRelicMetricNames);
        }
      }
    } catch (Exception ex) {
      logger.error("Unable to register workflow with NewRelicMetricNames collection", ex);
    }
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public Workflow updateWorkflow(Workflow workflow) {
    return updateWorkflow(workflow, workflow.getOrchestrationWorkflow());
  }

  @Override
  public Workflow updateWorkflow(Workflow workflow, OrchestrationWorkflow orchestrationWorkflow) {
    validateServiceandInframapping(workflow.getAppId(), workflow.getServiceId(), workflow.getInfraMappingId());
    return updateWorkflow(workflow, orchestrationWorkflow, true, false, false, false);
  }

  @Override
  public Workflow updateWorkflow(Workflow workflow, OrchestrationWorkflow orchestrationWorkflow,
      boolean inframappingChanged, boolean envChanged, boolean cloned) {
    return updateWorkflow(workflow, orchestrationWorkflow, true, inframappingChanged, envChanged, cloned);
  }

  private Workflow updateWorkflow(Workflow workflow, OrchestrationWorkflow orchestrationWorkflow,
      boolean onSaveCallNeeded, boolean inframappingChanged, boolean envChanged, boolean cloned) {
    UpdateOperations<Workflow> ops = wingsPersistence.createUpdateOperations(Workflow.class);
    setUnset(ops, "description", workflow.getDescription());
    setUnset(ops, "name", workflow.getName());
    List<TemplateExpression> templateExpressions = workflow.getTemplateExpressions();

    String workflowName = workflow.getName();
    String serviceId = workflow.getServiceId();
    String envId = workflow.getEnvId();
    String inframappingId = workflow.getInfraMappingId();

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
    orchestrationWorkflow = propagateWorkflowDataToPhases(orchestrationWorkflow, templateExpressions,
        workflow.getAppId(), serviceId, inframappingId, envChanged, inframappingChanged);

    setUnset(ops, "templateExpressions", templateExpressions);

    if (orchestrationWorkflow != null) {
      if (onSaveCallNeeded) {
        orchestrationWorkflow.onSave();
        if (envChanged) {
          workflow.setEnvId(envId);
          setUnset(ops, "envId", envId);
        }
        updateRequiredEntityTypes(workflow.getAppId(), orchestrationWorkflow);
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
    }

    wingsPersistence.update(wingsPersistence.createQuery(Workflow.class)
                                .filter("appId", workflow.getAppId())
                                .filter(ID_KEY, workflow.getUuid()),
        ops);

    workflow = readWorkflow(workflow.getAppId(), workflow.getUuid(), workflow.getDefaultVersion());

    Workflow finalWorkflow1 = workflow;
    executorService.submit(() -> notifyNewRelicMetricCollection(finalWorkflow1));

    Workflow finalWorkflow = workflow;
    executorService.submit(() -> {
      String accountId = appService.getAccountIdByAppId(finalWorkflow.getAppId());
      YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
      if (ygs != null) {
        List<GitFileChange> changeSet = new ArrayList<>();
        changeSet.add(entityUpdateService.getWorkflowGitSyncFile(accountId, finalWorkflow, ChangeType.MODIFY));
        yamlChangeSetService.saveChangeSet(ygs, changeSet);
      }
    });
    if (workflowName != null) {
      if (!workflowName.equals(workflow.getName())) {
        executorService.submit(() -> triggerService.updateByApp(finalWorkflow.getAppId()));
      }
    }
    updateKeywords(workflow);
    return workflow;
  }

  /***
   * Populates the workflow level data to Phase. It Validates the service and inframapping for Basics and Multi
   * Service deployment. Resets Node selection if environment or inframapping changed.
   * @param orchestrationWorkflow
   * @param templateExpressions
   * @param appId
   * @param serviceId
   * @param inframappingId
   * @param envChanged
   * @param inframappingChanged
   * @return OrchestrationWorkflow
   */
  private OrchestrationWorkflow propagateWorkflowDataToPhases(OrchestrationWorkflow orchestrationWorkflow,
      List<TemplateExpression> templateExpressions, String appId, String serviceId, String inframappingId,
      boolean envChanged, boolean inframappingChanged) {
    if (orchestrationWorkflow != null) {
      if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)
          || orchestrationWorkflow.getOrchestrationWorkflowType().equals(ROLLING)) {
        handleBasicWorkflow((CanaryOrchestrationWorkflow) orchestrationWorkflow, templateExpressions, appId, serviceId,
            inframappingId, envChanged, inframappingChanged);
      } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(MULTI_SERVICE)) {
        handleMultiServiceWorkflow(orchestrationWorkflow, templateExpressions, appId, envChanged, inframappingChanged);
      } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(CANARY)) {
        handleCanaryWorkflow(orchestrationWorkflow, templateExpressions, appId, envChanged, inframappingChanged);
      }
    }
    return orchestrationWorkflow;
  }

  /***
   *
   * @param orchestrationWorkflow
   * @param templateExpressions
   * @param appId
   * @param serviceId
   * @param inframappingId
   * @param envChanged
   * @param inframappingChanged
   */
  private void handleBasicWorkflow(CanaryOrchestrationWorkflow orchestrationWorkflow,
      List<TemplateExpression> templateExpressions, String appId, String serviceId, String inframappingId,
      boolean envChanged, boolean inframappingChanged) {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = orchestrationWorkflow;
    Optional<TemplateExpression> envExpression =
        templateExpressions.stream()
            .filter(templateExpression -> templateExpression.getFieldName().equals("envId"))
            .findAny();
    if (envExpression.isPresent()) {
      canaryOrchestrationWorkflow.addToUserVariables(asList(envExpression.get()));
    }
    if (canaryOrchestrationWorkflow.getWorkflowPhases() != null) {
      for (WorkflowPhase phase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
        setTemplateExpresssionsToPhase(templateExpressions, phase);
        validateServiceCompatibility(appId, serviceId, phase.getServiceId());
        setServiceId(serviceId, phase);
        setInframappingDetails(appId, inframappingId, phase, envChanged, inframappingChanged);
        if (inframappingChanged || envChanged) {
          resetNodeSelection(phase);
        }
      }
    }
    Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap();
    if (rollbackWorkflowPhaseIdMap != null) {
      rollbackWorkflowPhaseIdMap.values().forEach(phase -> {
        setServiceId(serviceId, phase);
        setInframappingDetails(appId, inframappingId, phase, envChanged, inframappingChanged);
      });
    }
  }

  /***
   * Propagates workflow level data to phase level
   * @param orchestrationWorkflow
   * @param templateExpressions
   * @param appId
   * @param envChanged
   * @param inframappingChanged
   */
  private void handleCanaryWorkflow(OrchestrationWorkflow orchestrationWorkflow,
      List<TemplateExpression> templateExpressions, String appId, boolean envChanged, boolean inframappingChanged) {
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
    canaryOrchestrationWorkflow.addToUserVariables(templateExpressions);
    // If envId changed nullify the infraMapping Ids
    if (canaryOrchestrationWorkflow.getWorkflowPhases() != null) {
      for (WorkflowPhase phase : canaryOrchestrationWorkflow.getWorkflowPhases()) {
        if (envChanged) {
          unsetInfraMappingDetails(phase);
          resetNodeSelection(phase);
        }
        if (inframappingChanged) {
          resetNodeSelection(phase);
        }
        // If environment templatized, then templatize infra automatically
        List<TemplateExpression> phaseTemplateExpressions = phase.getTemplateExpressions();
        if (phaseTemplateExpressions == null) {
          phaseTemplateExpressions = new ArrayList<>();
        }
        templatizeServiceInfra(appId, orchestrationWorkflow, phase, templateExpressions, phaseTemplateExpressions);
        phase.setTemplateExpressions(phaseTemplateExpressions);
      }
    }
    Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap();
    if (rollbackWorkflowPhaseIdMap != null) {
      rollbackWorkflowPhaseIdMap.values().forEach(phase -> {
        if (envChanged) {
          unsetInfraMappingDetails(phase);
          resetNodeSelection(phase);
        }
        if (inframappingChanged) {
          resetNodeSelection(phase);
        }
      });
    }
  }

  /***
   *
   * @param orchestrationWorkflow
   * @param templateExpressions
   * @param appId
   * @param envChanged
   * @param inframappingChanged
   */
  private void handleMultiServiceWorkflow(OrchestrationWorkflow orchestrationWorkflow,
      List<TemplateExpression> templateExpressions, String appId, boolean envChanged, boolean inframappingChanged) {
    MultiServiceOrchestrationWorkflow multiServiceOrchestrationWorkflow =
        (MultiServiceOrchestrationWorkflow) orchestrationWorkflow;
    multiServiceOrchestrationWorkflow.addToUserVariables(templateExpressions);
    List<WorkflowPhase> workflowPhases = multiServiceOrchestrationWorkflow.getWorkflowPhases();
    if (workflowPhases != null) {
      for (WorkflowPhase phase : workflowPhases) {
        if (envChanged) {
          unsetInfraMappingDetails(phase);
          resetNodeSelection(phase);
        }
        if (inframappingChanged) {
          resetNodeSelection(phase);
        }
        // If environment templatized, then templatize infra automatically
        List<TemplateExpression> phaseTemplateExpressions = phase.getTemplateExpressions();
        if (phaseTemplateExpressions == null) {
          phaseTemplateExpressions = new ArrayList<>();
        }
        templatizeServiceInfra(appId, orchestrationWorkflow, phase, templateExpressions, phaseTemplateExpressions);
        phase.setTemplateExpressions(phaseTemplateExpressions);
      }
      Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap =
          multiServiceOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap();
      if (rollbackWorkflowPhaseIdMap != null && rollbackWorkflowPhaseIdMap.values() != null) {
        rollbackWorkflowPhaseIdMap.values().forEach(phase -> {
          if (envChanged) {
            unsetInfraMappingDetails(phase);
            resetNodeSelection(phase);
          }
          if (inframappingChanged) {
            resetNodeSelection(phase);
          }
        });
      }
    }
  }

  /***
   * Templatizes the service infra if environment templatized for Phase
   */
  private void templatizeServiceInfra(String appId, OrchestrationWorkflow orchestrationWorkflow,
      WorkflowPhase workflowPhase, List<TemplateExpression> templateExpressions,
      List<TemplateExpression> phaseTemplateExpressions) {
    List<Variable> userVariables = orchestrationWorkflow.getUserVariables();
    if (isEmpty(userVariables)) {
      return;
    }
    List<Variable> entityVariables =
        userVariables.stream().filter(variable -> variable.getEntityType() != null).collect(toList());
    List<String> serviceInfraVariables =
        entityVariables.stream()
            .filter(variable -> variable.getEntityType().equals(INFRASTRUCTURE_MAPPING))
            .map(Variable::getName)
            .distinct()
            .collect(toList());
    if (workflowServiceHelper.isEnvironmentTemplatized(templateExpressions)
        && !workflowServiceHelper.isInfraTemplatized(phaseTemplateExpressions)) {
      Service service = serviceResourceService.get(appId, workflowPhase.getServiceId(), false);
      notNullCheck("Service", service);
      TemplateExpression templateExpression = new TemplateExpression();

      Map<String, Object> metaData = new HashMap<>();
      metaData.put(ENTITY_TYPE, INFRASTRUCTURE_MAPPING.name());
      if (service.getArtifactType() != null) {
        metaData.put(ARTIFACT_TYPE, service.getArtifactType().name());
      }
      String expression = "${ServiceInfra";
      int i = 1;
      for (String serviceInfraVariable : serviceInfraVariables) {
        if (serviceInfraVariable.startsWith("ServiceInfra")) {
          i++;
        }
      }
      DeploymentType deploymentType = workflowPhase.getDeploymentType();
      if (SSH.equals(deploymentType)) {
        expression = expression + "_SSH";
      } else if (AWS_CODEDEPLOY.equals(deploymentType)) {
        expression = expression + "_AWS_CodeDeploy";
      } else if (ECS.equals(deploymentType)) {
        expression = expression + "_ECS";
      } else if (KUBERNETES.equals(deploymentType)) {
        expression = expression + "_Kubernetes";
      } else if (AWS_LAMBDA.equals(deploymentType)) {
        expression = expression + "_AWS_Lambda";
      } else if (AMI.equals(deploymentType)) {
        expression = expression + "_AMI";
      }
      if (i != 1) {
        expression = expression + i;
      }
      expression = expression + "}";
      templateExpression.setFieldName("infraMappingId");
      templateExpression.setMetadata(metaData);
      templateExpression.setExpression(expression);
      phaseTemplateExpressions.add(templateExpression);
      orchestrationWorkflow.addToUserVariables(phaseTemplateExpressions, PHASE.name(), workflowPhase.getName(), null);
    }
  }

  private void unsetInfraMappingDetails(WorkflowPhase phase) {
    phase.setComputeProviderId(null);
    phase.setInfraMappingId(null);
    phase.setInfraMappingName(null);
    phase.setDeploymentType(null);
  }

  /**
   * Set template expressions to phase from workflow level
   *
   * @param templateExpressions
   * @param workflowPhase
   */
  private void setTemplateExpresssionsToPhase(
      List<TemplateExpression> templateExpressions, WorkflowPhase workflowPhase) {
    if (workflowPhase == null) {
      return;
    }

    List<TemplateExpression> phaseTemplateExpressions = new ArrayList<>();
    Optional<TemplateExpression> serviceExpression =
        templateExpressions.stream()
            .filter(templateExpression -> templateExpression.getFieldName().equals("serviceId"))
            .findAny();
    Optional<TemplateExpression> infraExpression =
        templateExpressions.stream()
            .filter(templateExpression -> templateExpression.getFieldName().equals("infraMappingId"))
            .findAny();
    serviceExpression.ifPresent(phaseTemplateExpressions::add);
    infraExpression.ifPresent(phaseTemplateExpressions::add);
    workflowPhase.setTemplateExpressions(phaseTemplateExpressions);
  }

  /**
   * Set template expressions to phase from workflow level
   *
   * @param workflow
   * @param workflowPhase
   */
  private void setTemplateExpresssionsFromPhase(Workflow workflow, WorkflowPhase workflowPhase) {
    List<TemplateExpression> templateExpressions = workflow.getTemplateExpressions();
    Optional<TemplateExpression> envExpression = templateExpressions == null
        ? Optional.empty()
        : templateExpressions.stream()
              .filter(templateExpression -> templateExpression.getFieldName().equals("envId"))
              .findAny();
    // Reset template expressions
    templateExpressions = new ArrayList<>();
    if (envExpression.isPresent()) {
      templateExpressions.add(envExpression.get());
    }
    if (workflowPhase != null) {
      List<TemplateExpression> phaseTemplateExpressions = workflowPhase.getTemplateExpressions();
      if (isEmpty(phaseTemplateExpressions)) {
        phaseTemplateExpressions = new ArrayList<>();
      }
      // It means, user templatizing it from phase level
      Optional<TemplateExpression> serviceExpression =
          phaseTemplateExpressions.stream()
              .filter(templateExpression -> templateExpression.getFieldName().equals("serviceId"))
              .findAny();
      Optional<TemplateExpression> infraExpression =
          phaseTemplateExpressions.stream()
              .filter(templateExpression -> templateExpression.getFieldName().equals("infraMappingId"))
              .findAny();
      if (serviceExpression.isPresent()) {
        templateExpressions.add(serviceExpression.get());
      }
      if (infraExpression.isPresent()) {
        templateExpressions.add(infraExpression.get());
      }
      validateTemplateExpressions(envExpression, templateExpressions);
      workflow.setTemplateExpressions(templateExpressions);
    }
  }

  private void validateTemplateExpressions(
      Optional<TemplateExpression> envExpression, List<TemplateExpression> templateExpressions) {
    // Validate combinations
    Optional<TemplateExpression> serviceExpression = templateExpressions == null
        ? Optional.empty()
        : templateExpressions.stream()
              .filter(templateExpression -> templateExpression.getFieldName().equals("serviceId"))
              .findAny();
    Optional<TemplateExpression> infraExpression = templateExpressions == null
        ? Optional.empty()
        : templateExpressions.stream()
              .filter(templateExpression -> templateExpression.getFieldName().equals("infraMappingId"))
              .findAny();
    // It means nullifying both Service and InfraMappings .. throw an error if environment is templatized
    // Infra not present
    envExpression.ifPresent(templateExpression -> {
      if (!infraExpression.isPresent()) {
        throw new InvalidRequestException(
            "Service Infrastructure cannot be de-templatized because Environment is templatized", USER);
      }
    });
    // Infra not present
    serviceExpression.ifPresent(templateExpression -> {
      if (!infraExpression.isPresent()) {
        throw new InvalidRequestException(
            "Service Infrastructure cannot be de-templatized because Service is templatized", USER);
      }
    });
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

  /**
   * Validates service compatibility
   *
   * @param appId
   * @param serviceId
   * @param oldServiceId
   */
  private void validateServiceCompatibility(String appId, String serviceId, String oldServiceId) {
    if (serviceId == null || oldServiceId == null || serviceId.equals(oldServiceId)) {
      return;
    }

    Service oldService = serviceResourceService.get(appId, oldServiceId, false);
    notNullCheck("service", oldService);
    Service newService = serviceResourceService.get(appId, serviceId, false);
    notNullCheck("service", newService);
    if (oldService.getArtifactType() != null && !oldService.getArtifactType().equals(newService.getArtifactType())) {
      throw new WingsException(INVALID_REQUEST)
          .addParam("message",
              "Service [" + newService.getName() + "] is not compatible with the service [" + oldService.getName()
                  + "]");
    }
  }

  /**
   * sets inframapping and cloud provider details along with deployment type
   *
   * @param inframappingId
   * @param phase
   */
  private void setInframappingDetails(
      String appId, String inframappingId, WorkflowPhase phase, boolean envChanged, boolean infraChanged) {
    if (inframappingId != null) {
      if (!inframappingId.equals(phase.getInfraMappingId())) {
        phase.setInfraMappingId(inframappingId);
        InfrastructureMapping infrastructureMapping =
            infrastructureMappingService.get(appId, phase.getInfraMappingId());
        notNullCheck("InfraMapping", infrastructureMapping);
        phase.setComputeProviderId(infrastructureMapping.getComputeProviderSettingId());
        phase.setInfraMappingName(infrastructureMapping.getName());
        phase.setDeploymentType(DeploymentType.valueOf(infrastructureMapping.getDeploymentType()));
        resetNodeSelection(phase);
      }
    } else if (envChanged && !infraChanged) {
      unsetInfraMappingDetails(phase);
    }
  }

  /**
   * Resets node selection if environment of infra changed
   *
   * @param phase
   */
  private void resetNodeSelection(WorkflowPhase phase) {
    // Update the node selection
    List<PhaseStep> phaseSteps = phase.getPhaseSteps();
    if (phaseSteps == null) {
      return;
    }
    for (PhaseStep phaseStep : phaseSteps) {
      if (phaseStep.getPhaseStepType().equals(PROVISION_NODE)) {
        List<GraphNode> steps = phaseStep.getSteps();
        if (steps != null) {
          for (GraphNode step : steps) {
            if (step.getType().equals(DC_NODE_SELECT.name()) || step.getType().equals(AWS_NODE_SELECT.name())) {
              Map<String, Object> properties = step.getProperties();
              if ((Boolean) properties.get("specificHosts")) {
                properties.put("specificHosts", Boolean.FALSE);
                properties.remove("hostNames");
              }
            }
          }
        }
      }
    }
  }

  private void ensureWorkflowSafeToDelete(Workflow workflow) {
    List<Pipeline> pipelines = pipelineService.listPipelines(
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(Pipeline.APP_ID_KEY, EQ, workflow.getAppId())
            .addFilter("pipelineStages.pipelineStageElements.properties.workflowId", EQ, workflow.getUuid())
            .build());

    if (!pipelines.isEmpty()) {
      List<String> pipelineNames = pipelines.stream().map(Pipeline::getName).collect(toList());
      String message = String.format("Workflow is referenced by %d %s [%s].", pipelines.size(),
          plural("pipeline", pipelines.size()), Joiner.on(", ").join(pipelineNames));
      throw new InvalidRequestException(message, USER);
    }

    if (workflowExecutionService.workflowExecutionsRunning(
            workflow.getWorkflowType(), workflow.getAppId(), workflow.getUuid())) {
      throw new WingsException(WORKFLOW_EXECUTION_IN_PROGRESS, USER)
          .addParam("message", String.format("Workflow: [%s] couldn't be deleted", workflow.getName()));
    }

    List<Trigger> triggers = triggerService.getTriggersHasWorkflowAction(workflow.getAppId(), workflow.getUuid());
    if (isEmpty(triggers)) {
      return;
    }
    List<String> triggerNames = triggers.stream().map(Trigger::getName).collect(toList());

    throw new WingsException(INVALID_REQUEST, USER)
        .addParam("message",
            String.format(
                "Workflow associated as a trigger action to triggers [%s]", Joiner.on(", ").join(triggerNames)));
  }

  private boolean pruneWorkflow(String appId, String workflowId) {
    PruneEntityJob.addDefaultJob(jobScheduler, Workflow.class, appId, workflowId, Duration.ofSeconds(5));

    if (!wingsPersistence.delete(Workflow.class, appId, workflowId)) {
      return false;
    }

    return true;
  }

  @Override
  public boolean deleteWorkflow(String appId, String workflowId) {
    return deleteWorkflow(appId, workflowId, false);
  }

  private boolean deleteWorkflow(String appId, String workflowId, boolean forceDelete) {
    Workflow workflow = wingsPersistence.get(Workflow.class, appId, workflowId);
    if (workflow == null) {
      return true;
    }

    if (!forceDelete) {
      ensureWorkflowSafeToDelete(workflow);
    }

    String accountId = appService.getAccountIdByAppId(workflow.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();
      changeSet.add(entityUpdateService.getWorkflowGitSyncFile(accountId, workflow, ChangeType.DELETE));
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }

    return pruneWorkflow(appId, workflowId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StateMachine readLatestStateMachine(String appId, String originId) {
    PageRequest<StateMachine> req =
        aPageRequest().addFilter("appId", EQ, appId).addFilter("originId", EQ, originId).build();
    return wingsPersistence.get(StateMachine.class, req);
  }

  public StateMachine readStateMachine(String appId, String stateMachineId) {
    PageRequest<StateMachine> req = aPageRequest().addFilter("appId", EQ, appId).build();
    return wingsPersistence.get(StateMachine.class, req);
  }

  @Override
  public StateMachine readStateMachine(String appId, String originId, Integer version) {
    PageRequest<StateMachine> req = aPageRequest()
                                        .addFilter("appId", EQ, appId)
                                        .addFilter("originId", EQ, originId)
                                        .addFilter("originVersion", EQ, version)
                                        .build();
    return wingsPersistence.get(StateMachine.class, req);
  }

  /**
   * Read latest simple workflow.
   *
   * @param appId the app id
   * @return the workflow
   */
  @Override
  public Workflow readLatestSimpleWorkflow(String appId, String envId) {
    PageRequest<Workflow> req = aPageRequest()
                                    .addFilter("appId", EQ, appId)
                                    .addFilter("envId", EQ, envId)
                                    .addFilter("workflowType", EQ, WorkflowType.SIMPLE)
                                    .addFilter("name", EQ, Constants.SIMPLE_ORCHESTRATION_NAME)
                                    .build();

    PageResponse<Workflow> workflows = listWorkflows(req);
    if (isEmpty(workflows)) {
      return createDefaultSimpleWorkflow(appId, envId);
    }
    return workflows.get(0);
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

  @Override
  public void pruneByEnvironment(String appId, String envId) {
    wingsPersistence.createQuery(Workflow.class)
        .filter("appId", appId)
        .filter("envId", envId)
        .asKeyList()
        .forEach(key -> pruneWorkflow(appId, key.getId().toString()));
  }

  private Workflow createDefaultSimpleWorkflow(String appId, String envId) {
    Workflow workflow = new Workflow();
    workflow.setName(Constants.SIMPLE_ORCHESTRATION_NAME);
    workflow.setDescription(Constants.SIMPLE_ORCHESTRATION_DESC);
    workflow.setAppId(appId);
    workflow.setEnvId(envId);
    workflow.setWorkflowType(WorkflowType.SIMPLE);

    Graph graph = staticConfiguration.defaultSimpleWorkflow();
    CustomOrchestrationWorkflow customOrchestrationWorkflow = new CustomOrchestrationWorkflow();
    customOrchestrationWorkflow.setGraph(graph);
    workflow.setOrchestrationWorkflow(customOrchestrationWorkflow);

    return createWorkflow(workflow);
  }

  /**
   * Sets static configuration.
   *
   * @param staticConfiguration the static configuration
   */
  void setStaticConfiguration(StaticConfiguration staticConfiguration) {
    this.staticConfiguration = staticConfiguration;
  }

  @Override
  public Map<String, String> getData(String appId, String... params) {
    List<Workflow> workflows = wingsPersistence.createQuery(Workflow.class).filter("appId", appId).asList();
    return workflows.stream().collect(toMap(Workflow::getUuid, o -> o.getName()));
  }

  @Override
  public PhaseStep updatePreDeployment(String appId, String workflowId, PhaseStep phaseStep) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow);
    orchestrationWorkflow.setPreDeploymentSteps(phaseStep);

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getPreDeploymentSteps();
  }

  @Override
  public PhaseStep updatePostDeployment(String appId, String workflowId, PhaseStep phaseStep) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow);
    orchestrationWorkflow.setPostDeploymentSteps(phaseStep);

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getPostDeploymentSteps();
  }

  @Override
  public WorkflowPhase createWorkflowPhase(String appId, String workflowId, WorkflowPhase workflowPhase) {
    notNullCheck("workflow", workflowPhase);

    validateServiceandInframapping(appId, workflowPhase.getServiceId(), workflowPhase.getInfraMappingId());

    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();

    notNullCheck("orchestrationWorkflow", orchestrationWorkflow);

    workflowPhase.setDaemonSet(isDaemonSet(appId, workflowPhase.getServiceId()));
    attachWorkflowPhase(workflow, workflowPhase);

    if (workflowPhase.getDeploymentType() == SSH && orchestrationWorkflow.getOrchestrationWorkflowType() != BUILD) {
      ensureArtifactCheckInPreDeployment((CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow());
    }

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getWorkflowPhaseIdMap().get(workflowPhase.getUuid());
  }

  public void validateServiceandInframapping(String appId, String serviceId, String inframappingId) {
    // Validate if service Id is valid or not
    if (serviceId == null || inframappingId == null) {
      return;
    }
    Service service = serviceResourceService.get(appId, serviceId, false);
    if (service == null) {
      throw new InvalidRequestException("Service [" + serviceId + "] does not exist");
    }
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, inframappingId);
    if (infrastructureMapping == null) {
      throw new InvalidRequestException("Service Infrastructure [" + inframappingId + "] does not exist");
    }
    if (!service.getUuid().equals(infrastructureMapping.getServiceId())) {
      throw new WingsException(INVALID_REQUEST)
          .addParam("message",
              "Service Infrastructure [" + infrastructureMapping.getName() + "] not mapped to Service ["
                  + service.getName() + "]");
    }
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
    switch (stateType) {
      case AWS_CODEDEPLOY_STATE: {
        List<ArtifactStream> artifactStreams = artifactStreamService.getArtifactStreamsForService(appId, serviceId);
        if (artifactStreams.stream().anyMatch(
                artifactStream -> ArtifactStreamType.AMAZON_S3.name().equals(artifactStream.getArtifactStreamType()))) {
          return AwsCodeDeployState.loadDefaults();
        }
        break;
      }
      default:
        unhandled(stateType);
    }
    return Collections.emptyMap();
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
  public void pruneDescendingEntities(String appId, String workflowId) {
    List<OwnedByWorkflow> services =
        ServiceClassLocator.descendingServices(this, WorkflowServiceImpl.class, OwnedByWorkflow.class);
    PruneEntityJob.pruneDescendingEntities(services, descending -> descending.pruneByWorkflow(appId, workflowId));
  }

  @Override
  public boolean workflowHasSshInfraMapping(String appId, String workflowId) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("Workflow", workflow, USER);
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    notNullCheck("OrchestrationWorkflow", orchestrationWorkflow, USER);
    List<String> infraMappingIds = new ArrayList<>();
    if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
      return workflowServiceHelper.workflowHasSshInfraMapping(appId, canaryOrchestrationWorkflow);
    }
    return false;
  }

  private void attachWorkflowPhase(Workflow workflow, WorkflowPhase workflowPhase) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.needCloudProvider()) {
      setCloudProvider(workflow.getAppId(), workflowPhase);
    }

    // No need to generate phase steps if it's already created
    if (isNotEmpty(workflowPhase.getPhaseSteps()) && orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases().add(workflowPhase);
      return;
    }

    if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(CANARY)) {
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
      boolean serviceRepeat = canaryOrchestrationWorkflow.serviceRepeat(workflowPhase);
      generateNewWorkflowPhaseSteps(workflow.getAppId(), workflow.getEnvId(), workflowPhase, serviceRepeat,
          orchestrationWorkflow.getOrchestrationWorkflowType());
      canaryOrchestrationWorkflow.getWorkflowPhases().add(workflowPhase);

      WorkflowPhase rollbackWorkflowPhase = generateRollbackWorkflowPhase(
          workflow.getAppId(), workflowPhase, !serviceRepeat, orchestrationWorkflow.getOrchestrationWorkflowType());
      canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)
        || orchestrationWorkflow.getOrchestrationWorkflowType().equals(ROLLING)) {
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
      generateNewWorkflowPhaseSteps(workflow.getAppId(), workflow.getEnvId(), workflowPhase, false,
          orchestrationWorkflow.getOrchestrationWorkflowType());
      canaryOrchestrationWorkflow.getWorkflowPhases().add(workflowPhase);

      WorkflowPhase rollbackWorkflowPhase = generateRollbackWorkflowPhase(
          workflow.getAppId(), workflowPhase, true, orchestrationWorkflow.getOrchestrationWorkflowType());
      canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(MULTI_SERVICE)) {
      MultiServiceOrchestrationWorkflow multiServiceOrchestrationWorkflow =
          (MultiServiceOrchestrationWorkflow) orchestrationWorkflow;
      boolean serviceRepeat = multiServiceOrchestrationWorkflow.serviceRepeat(workflowPhase);
      generateNewWorkflowPhaseSteps(workflow.getAppId(), workflow.getEnvId(), workflowPhase, serviceRepeat,
          orchestrationWorkflow.getOrchestrationWorkflowType());
      multiServiceOrchestrationWorkflow.getWorkflowPhases().add(workflowPhase);

      WorkflowPhase rollbackWorkflowPhase = generateRollbackWorkflowPhase(
          workflow.getAppId(), workflowPhase, !serviceRepeat, orchestrationWorkflow.getOrchestrationWorkflowType());
      multiServiceOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(
          workflowPhase.getUuid(), rollbackWorkflowPhase);
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BUILD)) {
      BuildWorkflow buildWorkflow = (BuildWorkflow) orchestrationWorkflow;
      generateNewWorkflowPhaseStepsForArtifactCollection(workflowPhase);
      buildWorkflow.getWorkflowPhases().add(workflowPhase);
    }
  }

  private void setCloudProvider(String appId, WorkflowPhase workflowPhase) {
    if (workflowPhase.checkInfraTemplatized()) {
      return;
    }
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
    notNullCheck("InfraMapping", infrastructureMapping);

    workflowPhase.setComputeProviderId(infrastructureMapping.getComputeProviderSettingId());
    workflowPhase.setInfraMappingName(infrastructureMapping.getName());
    workflowPhase.setDeploymentType(DeploymentType.valueOf(infrastructureMapping.getDeploymentType()));
  }

  @Override
  @ValidationGroups(Update.class)
  public WorkflowPhase updateWorkflowPhase(
      @NotEmpty String appId, @NotEmpty String workflowId, @Valid WorkflowPhase workflowPhase) {
    if (workflowPhase.isRollback() || workflowPhase.getPhaseSteps().stream().anyMatch(PhaseStep::isRollback)) {
      // This might seem as user error, but since this is controlled from the our UI lets get allerted for it
      throw new InvalidRequestException("The direct workflow phase should not have rollback flag set!", USER_SRE);
    }

    Workflow workflow = readWorkflow(appId, workflowId);
    if (workflow == null) {
      throw new InvalidArgumentsException(NameValuePair.builder().name("application").value(appId).build(),
          NameValuePair.builder().name("workflow").value(workflowId).build(),
          new ExplanationException("This might be caused from someone else deleted "
                  + "the application and/or the workflow while you worked on it.",
              MOVE_TO_THE_PARENT_OBJECT));
    }
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow);

    if (orchestrationWorkflow.getWorkflowPhaseIdMap().get(workflowPhase.getUuid()) == null) {
      throw new InvalidArgumentsException(NameValuePair.builder().name("workflow").value(workflowId).build(),
          NameValuePair.builder().name("workflowPhase").value(appId).build(),
          new ExplanationException("This might be caused from someone else modified "
                  + "the workflow resulting in removing the phase that you worked on.",
              MOVE_TO_THE_PARENT_OBJECT));
    }

    String serviceId = workflowPhase.getServiceId();
    String infraMappingId = workflowPhase.getInfraMappingId();
    if (!orchestrationWorkflow.getOrchestrationWorkflowType().equals(BUILD)) {
      Service service = serviceResourceService.get(appId, workflowPhase.getServiceId(), false);
      if (service == null) {
        throw new InvalidRequestException("Service [" + workflowPhase.getServiceId() + "] does not exist", USER);
      }
      InfrastructureMapping infrastructureMapping = null;
      if (!workflowPhase.checkInfraTemplatized()) {
        if (infraMappingId == null) {
          throw new InvalidRequestException(
              String.format(WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE, workflowPhase.getName()), USER);
        }
        infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
        notNullCheck("InfraMapping", infrastructureMapping);
        if (!service.getUuid().equals(infrastructureMapping.getServiceId())) {
          throw new WingsException(INVALID_REQUEST, USER)
              .addParam("message",
                  "Service Infrastructure [" + infrastructureMapping.getName() + "] not mapped to Service ["
                      + service.getName() + "]");
        }
      }
      if (infrastructureMapping != null) {
        workflowPhase.setComputeProviderId(infrastructureMapping.getComputeProviderSettingId());
        workflowPhase.setInfraMappingName(infrastructureMapping.getName());
        workflowPhase.setDeploymentType(DeploymentType.valueOf(infrastructureMapping.getDeploymentType()));
      }

      WorkflowPhase rollbackWorkflowPhase =
          orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());
      if (rollbackWorkflowPhase != null) {
        rollbackWorkflowPhase.setServiceId(serviceId);
        rollbackWorkflowPhase.setInfraMappingId(infraMappingId);
        if (infrastructureMapping != null) {
          rollbackWorkflowPhase.setComputeProviderId(infrastructureMapping.getComputeProviderSettingId());
          rollbackWorkflowPhase.setInfraMappingName(infrastructureMapping.getName());
          rollbackWorkflowPhase.setDeploymentType(DeploymentType.valueOf(infrastructureMapping.getDeploymentType()));
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
        break;
      }
    }
    if (!orchestrationWorkflow.getOrchestrationWorkflowType().equals(BUILD)) {
      validateServiceCompatibility(appId, serviceId, oldServiceId);
      if (!workflowPhase.checkInfraTemplatized()) {
        if (!infraMappingId.equals(oldInfraMappingId)) {
          inframappingChanged = true;
        }
      }
      // Propagate template expressions to workflow level
      if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)
          || orchestrationWorkflow.getOrchestrationWorkflowType().equals(ROLLING)) {
        setTemplateExpresssionsFromPhase(workflow, workflowPhase);
      } else {
        List<TemplateExpression> templateExpressions = workflow.getTemplateExpressions();
        Optional<TemplateExpression> envExpression = templateExpressions == null
            ? Optional.empty()
            : templateExpressions.stream()
                  .filter(templateExpression -> templateExpression.getFieldName().equals("envId"))
                  .findAny();
        validateTemplateExpressions(envExpression, workflowPhase.getTemplateExpressions());
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

  @Override
  public WorkflowPhase updateWorkflowPhaseRollback(
      String appId, String workflowId, String phaseId, WorkflowPhase rollbackWorkflowPhase) {
    if (!rollbackWorkflowPhase.isRollback()
        || rollbackWorkflowPhase.getPhaseSteps().stream().anyMatch(step -> !step.isRollback())) {
      // This might seem as user error, but since this is controlled from the our UI lets get allerted for it
      throw new InvalidRequestException("The rolback workflow phase should have rollback flag set!", USER_SRE);
    }

    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow);
    notNullCheck("WorkflowPhase", orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId));

    orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(phaseId, rollbackWorkflowPhase);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseId);
  }

  @Override
  public void deleteWorkflowPhase(String appId, String workflowId, String phaseId) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow);
    notNullCheck("WorkflowPhase", orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId));

    orchestrationWorkflow.getWorkflowPhases().remove(orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId));
    orchestrationWorkflow.getWorkflowPhaseIdMap().remove(phaseId);
    orchestrationWorkflow.getWorkflowPhaseIds().remove(phaseId);
    orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().remove(phaseId);
    updateWorkflow(workflow, orchestrationWorkflow);
  }

  @Override
  public GraphNode updateGraphNode(String appId, String workflowId, String subworkflowId, GraphNode node) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow);

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
      throw new InvalidRequestException("node not found");
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
  public Workflow cloneWorkflow(String appId, String originalWorkflowId, Workflow workflow) {
    Workflow originalWorkflow = readWorkflow(appId, originalWorkflowId);
    Workflow clonedWorkflow = originalWorkflow.cloneInternal();
    clonedWorkflow.setName(workflow.getName());
    clonedWorkflow.setDescription(workflow.getDescription());
    Workflow savedWorkflow = createWorkflow(clonedWorkflow);
    if (originalWorkflow.getOrchestrationWorkflow() != null) {
      savedWorkflow.setOrchestrationWorkflow(originalWorkflow.getOrchestrationWorkflow().cloneInternal());
    }
    return updateWorkflow(savedWorkflow, savedWorkflow.getOrchestrationWorkflow(), false, false, false, true);
  }

  @Override
  public Workflow cloneWorkflow(String appId, String originalWorkflowId, CloneMetadata cloneMetadata) {
    notNullCheck("cloneMetadata", cloneMetadata);
    Workflow workflow = cloneMetadata.getWorkflow();
    notNullCheck("workflow", workflow);
    workflow.setAppId(appId);
    String targetAppId = cloneMetadata.getTargetAppId();
    if (targetAppId == null || targetAppId.equals(appId)) {
      return cloneWorkflow(appId, originalWorkflowId, workflow);
    }

    logger.info("Cloning workflow across applications. "
        + "Environment, Service Infrastructure and Node selection will not be cloned");
    validateServiceMapping(appId, targetAppId, cloneMetadata.getServiceMapping());
    Workflow originalWorkflow = readWorkflow(appId, originalWorkflowId);
    Workflow clonedWorkflow = originalWorkflow.cloneInternal();
    clonedWorkflow.setName(workflow.getName());
    clonedWorkflow.setDescription(workflow.getDescription());
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

  /**
   * Validates whether service id and mapped service are of same type
   *
   * @param serviceMapping
   */
  private void validateServiceMapping(String appId, String targetAppId, Map<String, String> serviceMapping) {
    if (serviceMapping == null) {
      throw new InvalidRequestException("At least one service mapping required to clone across applications", USER);
    }
    Set<String> serviceIds = serviceMapping.keySet();
    for (String serviceId : serviceIds) {
      String targetServiceId = serviceMapping.get(serviceId);
      if (serviceId != null && targetServiceId != null) {
        Service oldService = serviceResourceService.get(appId, serviceId, false);
        notNullCheck("service", oldService);
        Service newService = serviceResourceService.get(targetAppId, targetServiceId, false);
        notNullCheck("targetService", newService);
        if (oldService.getArtifactType() != null
            && !oldService.getArtifactType().equals(newService.getArtifactType())) {
          throw new WingsException(INVALID_REQUEST, USER)
              .addParam("message",
                  "Target service  [" + oldService.getName() + " ] is not compatible with service ["
                      + newService.getName() + "]");
        }
      }
    }
  }

  @Override
  public Workflow updateWorkflow(String appId, String workflowId, Integer defaultVersion) {
    Workflow workflow = readWorkflow(appId, workflowId, null);
    wingsPersistence.update(
        workflow, wingsPersistence.createUpdateOperations(Workflow.class).set("defaultVersion", defaultVersion));

    workflow = readWorkflow(appId, workflowId, defaultVersion);

    Workflow finalWorkflow = workflow;
    executorService.submit(() -> {
      String accountId = appService.getAccountIdByAppId(finalWorkflow.getAppId());
      YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
      if (ygs != null) {
        List<GitFileChange> changeSet = new ArrayList<>();
        changeSet.add(entityUpdateService.getWorkflowGitSyncFile(accountId, finalWorkflow, ChangeType.MODIFY));
        yamlChangeSetService.saveChangeSet(ygs, changeSet);
      }
    });

    return workflow;
  }

  @Override
  public List<NotificationRule> updateNotificationRules(
      String appId, String workflowId, List<NotificationRule> notificationRules) {
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

  @Override
  public List<FailureStrategy> updateFailureStrategies(
      String appId, String workflowId, List<FailureStrategy> failureStrategies) {
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow);

    orchestrationWorkflow.setFailureStrategies(failureStrategies);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getFailureStrategies();
  }

  @Override
  public List<Variable> updateUserVariables(String appId, String workflowId, List<Variable> userVariables) {
    if (userVariables != null) {
      userVariables.stream().forEach(variable -> ExpressionEvaluator.isValidVariableName(variable.getName()));
    }
    Workflow workflow = readWorkflow(appId, workflowId);
    notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow);

    orchestrationWorkflow.setUserVariables(userVariables);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getUserVariables();
  }

  private Set<EntityType> updateRequiredEntityTypes(String appId, OrchestrationWorkflow orchestrationWorkflow) {
    notNullCheck("orchestrationWorkflow", orchestrationWorkflow);
    if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      Set<EntityType> requiredEntityTypes = ((CanaryOrchestrationWorkflow) orchestrationWorkflow)
                                                .getWorkflowPhases()
                                                .stream()
                                                .flatMap(phase -> updateRequiredEntityTypes(appId, phase).stream())
                                                .collect(Collectors.toSet());

      Set<EntityType> rollbackRequiredEntityTypes =
          ((CanaryOrchestrationWorkflow) orchestrationWorkflow)
              .getRollbackWorkflowPhaseIdMap()
              .values()
              .stream()
              .flatMap(phase -> updateRequiredEntityTypes(appId, phase).stream())
              .collect(Collectors.toSet());
      requiredEntityTypes.addAll(rollbackRequiredEntityTypes);

      orchestrationWorkflow.setRequiredEntityTypes(requiredEntityTypes);
      return requiredEntityTypes;
    } else if (orchestrationWorkflow instanceof BasicOrchestrationWorkflow) {
      Set<EntityType> requiredEntityTypes = ((BasicOrchestrationWorkflow) orchestrationWorkflow)
                                                .getWorkflowPhases()
                                                .stream()
                                                .flatMap(phase -> updateRequiredEntityTypes(appId, phase).stream())
                                                .collect(Collectors.toSet());

      Set<EntityType> rollbackRequiredEntityTypes =
          ((BasicOrchestrationWorkflow) orchestrationWorkflow)
              .getRollbackWorkflowPhaseIdMap()
              .values()
              .stream()
              .flatMap(phase -> updateRequiredEntityTypes(appId, phase).stream())
              .collect(Collectors.toSet());
      requiredEntityTypes.addAll(rollbackRequiredEntityTypes);

      orchestrationWorkflow.setRequiredEntityTypes(requiredEntityTypes);
    }
    return null;
  }

  private Set<EntityType> updateRequiredEntityTypes(String appId, WorkflowPhase workflowPhase) {
    Set<EntityType> requiredEntityTypes = new HashSet<>();

    if (workflowPhase == null || workflowPhase.getPhaseSteps() == null) {
      return requiredEntityTypes;
    }

    if (asList(ECS, KUBERNETES, AWS_CODEDEPLOY, AWS_LAMBDA, AMI, HELM, PCF)
            .contains(workflowPhase.getDeploymentType())) {
      requiredEntityTypes.add(ARTIFACT);
      return requiredEntityTypes;
    }

    if (workflowPhase.getDeploymentType() != null && workflowPhase.getDeploymentType().equals(DeploymentType.WINRM)) {
      return requiredEntityTypes;
    }

    if (workflowPhase.getInfraMappingId() != null) {
      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
      if (infrastructureMapping != null && infrastructureMapping.getHostConnectionAttrs() != null) {
        SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getHostConnectionAttrs());
        if (settingAttribute != null) {
          HostConnectionAttributes connectionAttributes = (HostConnectionAttributes) settingAttribute.getValue();
          populateRequiredEntityTypesByAccessType(requiredEntityTypes, connectionAttributes.getAccessType());
        }
      }
    }

    String serviceId = workflowPhase.getServiceId();

    for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
      if (phaseStep.getSteps() == null) {
        continue;
      }
      for (GraphNode step : phaseStep.getSteps()) {
        if ("COMMAND".equals(step.getType())) {
          ServiceCommand command = serviceResourceService.getCommandByName(
              appId, serviceId, (String) step.getProperties().get("commandName"));
          if (command != null && command.getCommand() != null && command.getCommand().isArtifactNeeded()) {
            requiredEntityTypes.add(ARTIFACT);
            phaseStep.setArtifactNeeded(true);
            break;
          }
        }
      }
    }
    return requiredEntityTypes;
  }

  private void generateNewWorkflowPhaseSteps(String appId, String envId, WorkflowPhase workflowPhase,
      boolean serviceRepeat, OrchestrationWorkflowType orchestrationWorkflowType) {
    DeploymentType deploymentType = workflowPhase.getDeploymentType();
    if (deploymentType == ECS) {
      generateNewWorkflowPhaseStepsForECS(appId, workflowPhase, !serviceRepeat);
    } else if (deploymentType == KUBERNETES) {
      generateNewWorkflowPhaseStepsForKubernetes(appId, workflowPhase, !serviceRepeat);
    } else if (deploymentType == HELM) {
      generateNewWorkflowPhaseStepsForHelm(appId, workflowPhase, !serviceRepeat);
    } else if (deploymentType == AWS_CODEDEPLOY) {
      generateNewWorkflowPhaseStepsForAWSCodeDeploy(appId, workflowPhase);
    } else if (deploymentType == AWS_LAMBDA) {
      generateNewWorkflowPhaseStepsForAWSLambda(appId, envId, workflowPhase);
    } else if (deploymentType == AMI) {
      generateNewWorkflowPhaseStepsForAWSAmi(appId, envId, workflowPhase, !serviceRepeat);
    } else if (deploymentType == PCF) {
      generateNewWorkflowPhaseStepsForPCF(appId, envId, workflowPhase, !serviceRepeat, orchestrationWorkflowType);
    } else {
      generateNewWorkflowPhaseStepsForSSH(appId, workflowPhase, orchestrationWorkflowType);
    }
  }

  private void generateNewWorkflowPhaseStepsForAWSAmi(
      String appId, String envId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
      if (infraMapping instanceof AwsAmiInfrastructureMapping) {
        Map<String, Object> defaultData = new HashMap<>();
        defaultData.put("maxInstances", 10);
        defaultData.put("autoScalingSteadyStateTimeout", 10);
        workflowPhase.addPhaseStep(aPhaseStep(AMI_AUTOSCALING_GROUP_SETUP, Constants.SETUP_AUTOSCALING_GROUP)
                                       .addStep(aGraphNode()
                                                    .withId(generateUuid())
                                                    .withType(AWS_AMI_SERVICE_SETUP.name())
                                                    .withName("AWS AutoScaling Group Setup")
                                                    .withProperties(defaultData)
                                                    .build())
                                       .build());
      }
    }
    workflowPhase.addPhaseStep(aPhaseStep(AMI_DEPLOY_AUTOSCALING_GROUP, Constants.DEPLOY_SERVICE)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(AWS_AMI_SERVICE_DEPLOY.name())
                                                .withName(Constants.UPGRADE_AUTOSCALING_GROUP)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private void generateNewWorkflowPhaseStepsForAWSLambda(String appId, String envId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    workflowPhase.addPhaseStep(aPhaseStep(PREPARE_STEPS, Constants.PREPARE_STEPS).build());

    workflowPhase.addPhaseStep(aPhaseStep(DEPLOY_AWS_LAMBDA, Constants.DEPLOY_SERVICE)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(AWS_LAMBDA_STATE.name())
                                                .withName(Constants.AWS_LAMBDA)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private void generateNewWorkflowPhaseStepsForArtifactCollection(WorkflowPhase workflowPhase) {
    workflowPhase.addPhaseStep(aPhaseStep(PREPARE_STEPS, Constants.PREPARE_STEPS).build());

    workflowPhase.addPhaseStep(aPhaseStep(COLLECT_ARTIFACT, Constants.COLLECT_ARTIFACT)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(ARTIFACT_COLLECTION.name())
                                                .withName(Constants.ARTIFACT_COLLECTION)
                                                .build())
                                   .build());
    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private void generateNewWorkflowPhaseStepsForAWSCodeDeploy(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    workflowPhase.addPhaseStep(aPhaseStep(PREPARE_STEPS, Constants.PREPARE_STEPS).build());

    Map<String, String> stateDefaults = getStateDefaults(appId, service.getUuid(), AWS_CODEDEPLOY_STATE);
    GraphNodeBuilder node =
        aGraphNode().withId(generateUuid()).withType(AWS_CODEDEPLOY_STATE.name()).withName(Constants.AWS_CODE_DEPLOY);
    if (isNotEmpty(stateDefaults)) {
      if (isNotBlank(stateDefaults.get("bucket"))) {
        node.addProperty("bucket", stateDefaults.get("bucket"));
      }
      if (isNotBlank(stateDefaults.get("key"))) {
        node.addProperty("key", stateDefaults.get("key"));
      }
      if (isNotBlank(stateDefaults.get("bundleType"))) {
        node.addProperty("bundleType", stateDefaults.get("bundleType"));
      }
    }
    workflowPhase.addPhaseStep(
        aPhaseStep(DEPLOY_AWSCODEDEPLOY, Constants.DEPLOY_SERVICE).addStep(node.build()).build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private void generateNewWorkflowPhaseStepsForECS(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
      if (infraMapping instanceof EcsInfrastructureMapping
          && Constants.RUNTIME.equals(((EcsInfrastructureMapping) infraMapping).getClusterName())) {
        workflowPhase.addPhaseStep(aPhaseStep(CLUSTER_SETUP, Constants.SETUP_CLUSTER)
                                       .addStep(aGraphNode()
                                                    .withId(generateUuid())
                                                    .withType(AWS_CLUSTER_SETUP.name())
                                                    .withName("AWS Cluster Setup")
                                                    .build())
                                       .build());
      }
      workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                                     .addStep(aGraphNode()
                                                  .withId(generateUuid())
                                                  .withType(ECS_SERVICE_SETUP.name())
                                                  .withName(Constants.ECS_SERVICE_SETUP)
                                                  .build())
                                     .build());
    }
    workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(ECS_SERVICE_DEPLOY.name())
                                                .withName(Constants.UPGRADE_CONTAINERS)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private void generateNewWorkflowPhaseStepsForPCF(String appId, String envId, WorkflowPhase workflowPhase,
      boolean serviceSetupRequired, OrchestrationWorkflowType orchestrationWorkflowType) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    if (serviceSetupRequired) {
      workflowPhase.addPhaseStep(
          aPhaseStep(PCF_SETUP, Constants.SETUP)
              .addStep(
                  aGraphNode().withId(generateUuid()).withType(PCF_SETUP.name()).withName(Constants.PCF_SETUP).build())
              .build());
    }

    workflowPhase.addPhaseStep(
        aPhaseStep(PCF_RESIZE, Constants.DEPLOY)
            .addStep(
                aGraphNode().withId(generateUuid()).withType(PCF_RESIZE.name()).withName(Constants.PCF_RESIZE).build())
            .build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    if (BASIC.equals(orchestrationWorkflowType)) {
      workflowPhase.addPhaseStep(aPhaseStep(PCF_ROUTE_SWAP, Constants.PCF_ROUTE_SWAP)
                                     .addStep(aGraphNode()
                                                  .withId(generateUuid())
                                                  .withType(PCF_ROUTE_SWAP.name())
                                                  .withName(Constants.PCF_ROUTE_SWAP)
                                                  .build())
                                     .build());
    }

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private void generateNewWorkflowPhaseStepsForHelm(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.HELM_DEPLOY, Constants.DEPLOY_CONTAINERS)
                                   .addStep(aGraphNode()
                                                .withId(generateUuid())
                                                .withType(HELM_DEPLOY.name())
                                                .withName(Constants.HELM_DEPLOY)
                                                .build())
                                   .build());
    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());
    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private void generateNewWorkflowPhaseStepsForKubernetes(
      String appId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
      if (infraMapping instanceof GcpKubernetesInfrastructureMapping
          && Constants.RUNTIME.equals(((GcpKubernetesInfrastructureMapping) infraMapping).getClusterName())) {
        workflowPhase.addPhaseStep(aPhaseStep(CLUSTER_SETUP, Constants.SETUP_CLUSTER)
                                       .addStep(aGraphNode()
                                                    .withId(generateUuid())
                                                    .withType(GCP_CLUSTER_SETUP.name())
                                                    .withName("GCP Cluster Setup")
                                                    .build())
                                       .build());
      }
      workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                                     .addStep(aGraphNode()
                                                  .withId(generateUuid())
                                                  .withType(KUBERNETES_SETUP.name())
                                                  .withName(Constants.KUBERNETES_SERVICE_SETUP)
                                                  .build())
                                     .build());
    }

    if (!workflowPhase.isDaemonSet()) {
      workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                                     .addStep(aGraphNode()
                                                  .withId(generateUuid())
                                                  .withType(KUBERNETES_DEPLOY.name())
                                                  .withName(Constants.UPGRADE_CONTAINERS)
                                                  .build())
                                     .build());
    }
    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());
    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private void generateNewWorkflowPhaseStepsForSSH(
      String appId, WorkflowPhase workflowPhase, OrchestrationWorkflowType orchestrationWorkflowType) {
    // For DC only - for other types it has to be customized

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
    StateType stateType;
    if (orchestrationWorkflowType == ROLLING) {
      stateType = ROLLING_NODE_SELECT;
    } else {
      stateType =
          infrastructureMapping.getComputeProviderType().equals(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
          ? DC_NODE_SELECT
          : AWS_NODE_SELECT;
    }

    if (!asList(ROLLING_NODE_SELECT, DC_NODE_SELECT, AWS_NODE_SELECT).contains(stateType)) {
      throw new InvalidRequestException("Unsupported state type: " + stateType, USER);
    }

    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    workflowPhase.addPhaseStep(aPhaseStep(PROVISION_NODE, Constants.PROVISION_NODE_NAME)
                                   .addStep(aGraphNode()
                                                .withType(stateType.name())
                                                .withName("Select Nodes")
                                                .addProperty("specificHosts", false)
                                                .addProperty("instanceCount", 1)
                                                .addProperty("excludeSelectedHostsFromFuturePhases", true)
                                                .build())
                                   .build());

    List<GraphNode> disableServiceSteps = commandNodes(commandMap, CommandType.DISABLE);
    List<GraphNode> enableServiceSteps = commandNodes(commandMap, CommandType.ENABLE);

    if (attachElbSteps(infrastructureMapping)) {
      disableServiceSteps.add(aGraphNode()
                                  .withType(ELASTIC_LOAD_BALANCER.name())
                                  .withName("Elastic Load Balancer")
                                  .addProperty("operation", Operation.Disable)
                                  .build());
      enableServiceSteps.add(aGraphNode()
                                 .withType(ELASTIC_LOAD_BALANCER.name())
                                 .withName("Elastic Load Balancer")
                                 .addProperty("operation", Operation.Enable)
                                 .build());
    }

    workflowPhase.addPhaseStep(
        aPhaseStep(DISABLE_SERVICE, Constants.DISABLE_SERVICE).addAllSteps(disableServiceSteps).build());

    workflowPhase.addPhaseStep(aPhaseStep(DEPLOY_SERVICE, Constants.DEPLOY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.INSTALL))
                                   .build());

    workflowPhase.addPhaseStep(
        aPhaseStep(ENABLE_SERVICE, Constants.ENABLE_SERVICE).addAllSteps(enableServiceSteps).build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private boolean attachElbSteps(InfrastructureMapping infrastructureMapping) {
    return (infrastructureMapping instanceof PhysicalInfrastructureMappingBase
               && isNotBlank(((PhysicalInfrastructureMappingBase) infrastructureMapping).getLoadBalancerId()))
        || (infrastructureMapping instanceof AwsInfrastructureMapping
               && isNotBlank(((AwsInfrastructureMapping) infrastructureMapping).getLoadBalancerId()));
  }

  private WorkflowPhase generateRollbackWorkflowPhase(String appId, WorkflowPhase workflowPhase,
      boolean serviceSetupRequired, OrchestrationWorkflowType orchestrationWorkflowType) {
    DeploymentType deploymentType = workflowPhase.getDeploymentType();
    if (deploymentType == ECS) {
      return generateRollbackWorkflowPhaseForEcs(workflowPhase);
    } else if (deploymentType == KUBERNETES) {
      return generateRollbackWorkflowPhaseForKubernetes(workflowPhase, appId, serviceSetupRequired);
    } else if (deploymentType == AWS_CODEDEPLOY) {
      return generateRollbackWorkflowPhaseForAwsCodeDeploy(workflowPhase);
    } else if (deploymentType == AWS_LAMBDA) {
      return generateRollbackWorkflowPhaseForAwsLambda(workflowPhase);
    } else if (deploymentType == AMI) {
      return generateRollbackWorkflowPhaseForAwsAmi(workflowPhase);
    } else if (deploymentType == HELM) {
      return generateRollbackWorkflowPhaseForHelm(workflowPhase);
    } else if (deploymentType == PCF) {
      return generateRollbackWorkflowPhaseForPCF(workflowPhase);
    } else {
      return generateRollbackWorkflowPhaseForSSH(appId, workflowPhase, orchestrationWorkflowType);
    }
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForPCF(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(PhaseStepType.PCF_RESIZE, Constants.DEPLOY)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(PCF_ROLLBACK.name())
                                       .withName(Constants.PCF_ROLLBACK)
                                       .addProperty("rollback", true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build())
        .build();
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForHelm(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(PhaseStepType.HELM_DEPLOY, Constants.DEPLOY_CONTAINERS)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(HELM_ROLLBACK.name())
                                       .withName(Constants.HELM_ROLLBACK)
                                       .addProperty("rollback", true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForAwsAmi(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(AMI_DEPLOY_AUTOSCALING_GROUP, Constants.ROLLBACK_SERVICE)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(AWS_AMI_SERVICE_ROLLBACK.name())
                                       .withName(Constants.ROLLBACK_AWS_AMI_CLUSTER)
                                       .addProperty("rollback", true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withRollback(true)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForAwsLambda(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(DEPLOY_AWS_LAMBDA, Constants.DEPLOY_SERVICE)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(AWS_LAMBDA_ROLLBACK.name())
                                       .withName(Constants.ROLLBACK_AWS_LAMBDA)
                                       .addProperty("rollback", true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        // Verificanion is not exactly rollbacking operation. It should be executed if deployment is needed
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForEcs(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(ECS_SERVICE_ROLLBACK.name())
                                       .withName(Constants.ROLLBACK_CONTAINERS)
                                       .addProperty("rollback", true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForAwsCodeDeploy(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(DEPLOY_AWSCODEDEPLOY, Constants.DEPLOY_SERVICE)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(AWS_CODEDEPLOY_ROLLBACK.name())
                                       .withName(Constants.ROLLBACK_AWS_CODE_DEPLOY)
                                       .addProperty("rollback", true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForSSH(
      String appId, WorkflowPhase workflowPhase, OrchestrationWorkflowType orchestrationWorkflowType) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
    StateType stateType = null;
    if (orchestrationWorkflowType == ROLLING) {
      stateType = ROLLING_NODE_SELECT;
    } else {
      stateType =
          infrastructureMapping.getComputeProviderType().equals(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
          ? DC_NODE_SELECT
          : AWS_NODE_SELECT;
    }

    List<GraphNode> disableServiceSteps = commandNodes(commandMap, CommandType.DISABLE, true);
    List<GraphNode> enableServiceSteps = commandNodes(commandMap, CommandType.ENABLE, true);

    if (attachElbSteps(infrastructureMapping)) {
      disableServiceSteps.add(aGraphNode()
                                  .withType(ELASTIC_LOAD_BALANCER.name())
                                  .withName("Elastic Load Balancer")
                                  .addProperty("operation", Operation.Disable)
                                  .withRollback(true)
                                  .build());
      enableServiceSteps.add(aGraphNode()
                                 .withType(ELASTIC_LOAD_BALANCER.name())
                                 .withName("Elastic Load Balancer")
                                 .addProperty("operation", Operation.Enable)
                                 .withRollback(true)
                                 .build());
    }

    WorkflowPhase rollbackWorkflowPhase =
        aWorkflowPhase()
            .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
            .withRollback(true)
            .withServiceId(workflowPhase.getServiceId())
            .withComputeProviderId(workflowPhase.getComputeProviderId())
            .withInfraMappingName(workflowPhase.getInfraMappingName())
            .withPhaseNameForRollback(workflowPhase.getName())
            .withDeploymentType(workflowPhase.getDeploymentType())
            .withInfraMappingId(workflowPhase.getInfraMappingId())
            .addPhaseStep(aPhaseStep(DISABLE_SERVICE, Constants.DISABLE_SERVICE)
                              .addAllSteps(disableServiceSteps)
                              .withPhaseStepNameForRollback(Constants.ENABLE_SERVICE)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .withRollback(true)
                              .build())
            .addPhaseStep(aPhaseStep(STOP_SERVICE, Constants.STOP_SERVICE)
                              .addAllSteps(commandNodes(commandMap, CommandType.STOP, true))
                              .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .withRollback(true)
                              .build())
            .addPhaseStep(aPhaseStep(DEPLOY_SERVICE, Constants.DEPLOY_SERVICE)
                              .addAllSteps(commandNodes(commandMap, CommandType.INSTALL, true))
                              .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .withRollback(true)
                              .build())
            .addPhaseStep(aPhaseStep(ENABLE_SERVICE, Constants.ENABLE_SERVICE)
                              .addAllSteps(enableServiceSteps)
                              .withPhaseStepNameForRollback(Constants.DISABLE_SERVICE)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .withRollback(true)
                              .build())
            // When we rolling back the verification steps the same criterie to run if deployment is needed should be
            // used
            .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                              .addAllSteps(commandNodes(commandMap, CommandType.VERIFY, true))
                              .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .withRollback(true)
                              .build())
            .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
            .build();

    // get provision NODE
    Optional<PhaseStep> provisionPhaseStep =
        workflowPhase.getPhaseSteps().stream().filter(ps -> ps.getPhaseStepType() == PROVISION_NODE).findFirst();
    if (provisionPhaseStep.isPresent() && provisionPhaseStep.get().getSteps() != null) {
      Optional<GraphNode> awsProvisionNode =
          provisionPhaseStep.get()
              .getSteps()
              .stream()
              .filter(n
                  -> n.getType() != null && n.getType().equals(AWS_NODE_SELECT.name()) && n.getProperties() != null
                      && n.getProperties().get("provisionNode") != null
                      && n.getProperties().get("provisionNode").equals(true))
              .findFirst();

      awsProvisionNode.ifPresent(node
          -> rollbackWorkflowPhase.getPhaseSteps().add(
              aPhaseStep(DE_PROVISION_NODE, Constants.DE_PROVISION_NODE).build()));
    }

    return rollbackWorkflowPhase;
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForKubernetes(
      WorkflowPhase workflowPhase, String appId, boolean serviceSetupRequired) {
    if (workflowPhase.isDaemonSet()) {
      return generateRollbackWorkflowPhaseForDaemonSets(workflowPhase);
    } else {
      WorkflowPhaseBuilder workflowPhaseBuilder =
          aWorkflowPhase()
              .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
              .withRollback(true)
              .withServiceId(workflowPhase.getServiceId())
              .withComputeProviderId(workflowPhase.getComputeProviderId())
              .withInfraMappingName(workflowPhase.getInfraMappingName())
              .withPhaseNameForRollback(workflowPhase.getName())
              .withDeploymentType(workflowPhase.getDeploymentType())
              .withInfraMappingId(workflowPhase.getInfraMappingId())
              .addPhaseStep(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                                .addStep(aGraphNode()
                                             .withId(generateUuid())
                                             .withType(KUBERNETES_DEPLOY_ROLLBACK.name())
                                             .withName(Constants.ROLLBACK_CONTAINERS)
                                             .addProperty("rollback", true)
                                             .build())
                                .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                                .withStatusForRollback(ExecutionStatus.SUCCESS)
                                .withRollback(true)
                                .build());
      if (serviceSetupRequired) {
        workflowPhaseBuilder.addPhaseStep(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                                              .addStep(aGraphNode()
                                                           .withId(generateUuid())
                                                           .withType(KUBERNETES_SETUP_ROLLBACK.name())
                                                           .withName(Constants.ROLLBACK_KUBERNETES_SETUP)
                                                           .addProperty("rollback", true)
                                                           .build())
                                              .withPhaseStepNameForRollback(Constants.SETUP_CONTAINER)
                                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                                              .withRollback(true)
                                              .build());
      }

      // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
      workflowPhaseBuilder
          .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                            .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                            .withStatusForRollback(ExecutionStatus.SUCCESS)
                            .withRollback(true)
                            .build())
          .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build());
      return workflowPhaseBuilder.build();
    }
  }

  private boolean isDaemonSet(String appId, String serviceId) {
    KubernetesContainerTask containerTask =
        (KubernetesContainerTask) serviceResourceService.getContainerTaskByDeploymentType(
            appId, serviceId, KUBERNETES.name());
    return containerTask != null && containerTask.checkDaemonSet();
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForDaemonSets(WorkflowPhase workflowPhase) {
    return aWorkflowPhase()
        .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
        .withRollback(true)
        .withDaemonSet(true)
        .withServiceId(workflowPhase.getServiceId())
        .withComputeProviderId(workflowPhase.getComputeProviderId())
        .withInfraMappingName(workflowPhase.getInfraMappingName())
        .withPhaseNameForRollback(workflowPhase.getName())
        .withDeploymentType(workflowPhase.getDeploymentType())
        .withInfraMappingId(workflowPhase.getInfraMappingId())
        .addPhaseStep(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                          .addStep(aGraphNode()
                                       .withId(generateUuid())
                                       .withType(KUBERNETES_SETUP_ROLLBACK.name())
                                       .withName(Constants.ROLLBACK_KUBERNETES_SETUP)
                                       .addProperty("rollback", true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.SETUP_CONTAINER)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        // When we rolling back the verification steps the same criterie to run if deployment is needed should be used
        .addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                          .withPhaseStepNameForRollback(Constants.SETUP_CONTAINER)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).withRollback(true).build())
        .build();
  }

  private Map<CommandType, List<Command>> getCommandTypeListMap(Service service) {
    Map<CommandType, List<Command>> commandMap = new HashMap<>();
    List<ServiceCommand> serviceCommands = service.getServiceCommands();
    if (serviceCommands == null) {
      return commandMap;
    }
    for (ServiceCommand sc : serviceCommands) {
      if (sc.getCommand() == null || sc.getCommand().getCommandType() == null) {
        continue;
      }
      commandMap.computeIfAbsent(sc.getCommand().getCommandType(), k -> new ArrayList<>()).add(sc.getCommand());
    }
    return commandMap;
  }

  private List<GraphNode> commandNodes(Map<CommandType, List<Command>> commandMap, CommandType commandType) {
    return commandNodes(commandMap, commandType, false);
  }

  private List<GraphNode> commandNodes(
      Map<CommandType, List<Command>> commandMap, CommandType commandType, boolean rollback) {
    List<GraphNode> nodes = new ArrayList<>();

    List<Command> commands = commandMap.get(commandType);
    if (commands == null) {
      return nodes;
    }

    for (Command command : commands) {
      nodes.add(aGraphNode()
                    .withId(generateUuid())
                    .withType(COMMAND.name())
                    .withName(command.getName())
                    .addProperty("commandName", command.getName())
                    .withRollback(rollback)
                    .build());
    }
    return nodes;
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
      case CANARY:
      case MULTI_SERVICE: {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
        canaryOrchestrationWorkflow.setFailureStrategies(failureStrategies);
        break;
      }
      default: { noop(); }
    }
  }

  private void validateBasicOrRollingWorkflow(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow != null
        && (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)
               || orchestrationWorkflow.getOrchestrationWorkflowType().equals(ROLLING))) {
      if (!orchestrationWorkflow.isInfraMappingTemplatized()) {
        notNullCheck("Invalid inframappingId", workflow.getInfraMappingId(), USER);

        if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(ROLLING)) {
          String infraMappingId = workflow.getInfraMappingId();
          InfrastructureMapping infrastructureMapping =
              infrastructureMappingService.get(workflow.getAppId(), infraMappingId);
          if (infrastructureMapping == null
              || !(InfrastructureMappingType.AWS_SSH.name().equals(infrastructureMapping.getInfraMappingType())
                     || InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name().equals(
                            infrastructureMapping.getInfraMappingType()))) {
            logger.warn("Rolling Deployment is requested for {}", infrastructureMapping.getInfraMappingType());
            throw new InvalidRequestException(
                "Requested Infrastructure Type is not supported using Rolling Deployment", USER);
          }
        }
      }
      if (!orchestrationWorkflow.isServiceTemplatized()) {
        notNullCheck("Invalid serviceId", workflow.getServiceId(), USER);
      }
    }
  }
}
