/**
 *
 */

package software.wings.service.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.EntityType.ARTIFACT;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.beans.EntityType.valueOf;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ErrorCode.WORKFLOW_EXECUTION_IN_PROGRESS;
import static software.wings.beans.FailureStrategy.FailureStrategyBuilder.aFailureStrategy;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.OrchestrationWorkflowType.BASIC;
import static software.wings.beans.OrchestrationWorkflowType.CANARY;
import static software.wings.beans.OrchestrationWorkflowType.MULTI_SERVICE;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.CLUSTER_SETUP;
import static software.wings.beans.PhaseStepType.CONTAINER_DEPLOY;
import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;
import static software.wings.beans.PhaseStepType.DEPLOY_AWSCODEDEPLOY;
import static software.wings.beans.PhaseStepType.DEPLOY_AWS_LAMBDA;
import static software.wings.beans.PhaseStepType.DEPLOY_SERVICE;
import static software.wings.beans.PhaseStepType.DE_PROVISION_NODE;
import static software.wings.beans.PhaseStepType.DISABLE_SERVICE;
import static software.wings.beans.PhaseStepType.ENABLE_SERVICE;
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
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.StateMachineExecutionSimulator.populateRequiredEntityTypesByAccessType;
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
import static software.wings.sm.StateType.KUBERNETES_REPLICATION_CONTROLLER_DEPLOY;
import static software.wings.sm.StateType.KUBERNETES_REPLICATION_CONTROLLER_ROLLBACK;
import static software.wings.sm.StateType.KUBERNETES_REPLICATION_CONTROLLER_SETUP;
import static software.wings.sm.StateType.values;

import com.google.common.base.Joiner;
import com.google.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.api.DeploymentType;
import software.wings.app.StaticConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureStrategy;
import software.wings.beans.FailureType;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Node;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.MultiServiceOrchestrationWorkflow;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.RepairActionCode;
import software.wings.beans.RoleType;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.stats.CloneMetadata;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.sm.states.ElasticLoadBalancerState.Operation;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilCategory;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.ExpressionEvaluator;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.Arrays;
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
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class WorkflowServiceImpl.
 *
 * @author Rishi
 */
@Singleton
@ValidateOnExecution
public class WorkflowServiceImpl implements WorkflowService, DataProvider {
  private static final Comparator<Stencil> stencilDefaultSorter = (o1, o2) -> {
    int comp = o1.getStencilCategory().getDisplayOrder().compareTo(o2.getStencilCategory().getDisplayOrder());
    if (comp != 0) {
      return comp;
    } else {
      comp = o1.getDisplayOrder().compareTo(o2.getDisplayOrder());
      if (comp != 0) {
        return comp;
      } else {
        return o1.getType().compareTo(o2.getType());
      }
    }
  };
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;
  @Inject private StencilPostProcessor stencilPostProcessor;
  @Inject private PluginManager pluginManager;
  @Inject private StaticConfiguration staticConfiguration;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private EntityVersionService entityVersionService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private SettingsService settingsService;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private AppService appService;
  @Inject private AccountService accountService;
  @Inject private ExecutorService executorService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private PipelineService pipelineService;
  @Inject private YamlDirectoryService yamlDirectoryService;

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
    Map<StateTypeScope, List<StateTypeDescriptor>> stencilsMap = loadStateTypes();

    Map<StateTypeScope, List<Stencil>> mapByScope = stencilsMap.entrySet().stream().collect(toMap(Entry::getKey,
        stateTypeScopeListEntry -> stencilPostProcessor.postProcess(stateTypeScopeListEntry.getValue(), appId)));

    Map<StateTypeScope, List<Stencil>> maps = new HashMap<>();
    if (ArrayUtils.isEmpty(stateTypeScopes)) {
      maps.putAll(mapByScope);
    } else {
      for (StateTypeScope scope : stateTypeScopes) {
        maps.put(scope, mapByScope.get(scope));
      }
    }
    maps.values().forEach(list -> list.sort(stencilDefaultSorter));

    boolean filterForWorkflow = isNotBlank(workflowId);
    boolean filterForPhase = filterForWorkflow && isNotBlank(phaseId);

    Predicate<Stencil> predicate = stencil -> true;
    if (filterForWorkflow) {
      Workflow workflow = readWorkflow(appId, workflowId);
      if (filterForPhase) {
        WorkflowPhase workflowPhase = null;
        if (workflow != null) {
          OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
          if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
            workflowPhase = ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhaseIdMap().get(phaseId);
          } else if (orchestrationWorkflow instanceof BasicOrchestrationWorkflow) {
            workflowPhase = ((BasicOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhaseIdMap().get(phaseId);
          } else if (orchestrationWorkflow instanceof MultiServiceOrchestrationWorkflow) {
            workflowPhase =
                ((MultiServiceOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhaseIdMap().get(phaseId);
          }
          if (workflowPhase.getInfraMappingId() != null) {
            InfrastructureMapping infrastructureMapping =
                infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
            predicate = stencil -> stencil.matches(infrastructureMapping);
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

    return maps;
  }

  private Map<StateTypeScope, List<StateTypeDescriptor>> loadStateTypes() {
    if (cachedStencils != null) {
      return cachedStencils;
    }

    List<StateTypeDescriptor> stencils = new ArrayList<StateTypeDescriptor>();
    stencils.addAll(Arrays.asList(values()));

    List<StateTypeDescriptor> plugins = pluginManager.getExtensions(StateTypeDescriptor.class);
    stencils.addAll(plugins);

    Map<String, StateTypeDescriptor> mapByType = new HashMap<>();
    Map<StateTypeScope, List<StateTypeDescriptor>> mapByScope = new HashMap<>();
    for (StateTypeDescriptor sd : stencils) {
      if (mapByType.get(sd.getType()) != null) {
        // already present for the type
        logger.error("Duplicate implementation for the stencil: {}", sd.getType());
        throw new WingsException("Duplicate implementation for the stencil: " + sd.getType());
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
  public PageResponse<Workflow> listWorkflows(PageRequest<Workflow> pageRequest) {
    return listWorkflows(pageRequest, 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Workflow> listWorkflows(PageRequest<Workflow> pageRequest, Integer previousExecutionsCount) {
    PageResponse<Workflow> workflows = wingsPersistence.query(Workflow.class, pageRequest);
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

  private void loadOrchestrationWorkflow(Workflow workflow, Integer version) {
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
                                 .collect(Collectors.toList());

    workflow.setServices(services);
    workflow.setTemplatizedServiceIds(orchestrationWorkflow.getTemplatizedServiceIds());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Workflow createWorkflow(Workflow workflow) {
    validateBasicWorkflow(workflow);
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    workflow.setDefaultVersion(1);
    String key = wingsPersistence.save(workflow);
    if (orchestrationWorkflow != null) {
      if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(CANARY)) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
        if (canaryOrchestrationWorkflow.getWorkflowPhases() != null
            && !canaryOrchestrationWorkflow.getWorkflowPhases().isEmpty()) {
          List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
          canaryOrchestrationWorkflow.setWorkflowPhases(new ArrayList<>());
          workflowPhases.forEach(workflowPhase -> attachWorkflowPhase(workflow, workflowPhase));
        }
      } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)) {
        // Create Single Phase
        Validator.notNullCheck("infraMappingId", workflow.getInfraMappingId());
        Validator.notNullCheck("serviceId", workflow.getServiceId());
        WorkflowPhase workflowPhase = aWorkflowPhase()
                                          .withInfraMappingId(workflow.getInfraMappingId())
                                          .withServiceId(workflow.getServiceId())
                                          .build();
        attachWorkflowPhase(workflow, workflowPhase);
      } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(MULTI_SERVICE)) {
        MultiServiceOrchestrationWorkflow canaryOrchestrationWorkflow =
            (MultiServiceOrchestrationWorkflow) orchestrationWorkflow;
        if (canaryOrchestrationWorkflow.getWorkflowPhases() != null
            && !canaryOrchestrationWorkflow.getWorkflowPhases().isEmpty()) {
          List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
          canaryOrchestrationWorkflow.setWorkflowPhases(new ArrayList<>());
          workflowPhases.forEach(workflowPhase -> attachWorkflowPhase(workflow, workflowPhase));
        }
      }
      createDefaultNotificationRule(workflow);
      createDefaultFailureStrategy(workflow);
      orchestrationWorkflow.onSave();
      updateRequiredEntityTypes(workflow.getAppId(), orchestrationWorkflow);
      StateMachine stateMachine = new StateMachine(workflow, workflow.getDefaultVersion(),
          ((CustomOrchestrationWorkflow) orchestrationWorkflow).getGraph(), stencilMap());
      stateMachine = wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
    }
    // create initial version
    entityVersionService.newEntityVersion(
        workflow.getAppId(), WORKFLOW, key, workflow.getName(), ChangeType.CREATED, workflow.getNotes());

    return readWorkflow(workflow.getAppId(), key, workflow.getDefaultVersion());
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
    return updateWorkflow(workflow, orchestrationWorkflow, true, false, false);
  }

  @Override
  public Workflow updateWorkflow(
      Workflow workflow, OrchestrationWorkflow orchestrationWorkflow, boolean inframappingChanged, boolean envChanged) {
    return updateWorkflow(workflow, orchestrationWorkflow, true, inframappingChanged, envChanged);
  }

  private Workflow updateWorkflow(Workflow workflow, OrchestrationWorkflow orchestrationWorkflow,
      boolean onSaveCallNeeded, boolean inframappingChanged, boolean envChanged) {
    UpdateOperations<Workflow> ops = wingsPersistence.createUpdateOperations(Workflow.class);
    setUnset(ops, "description", workflow.getDescription());
    setUnset(ops, "name", workflow.getName());
    List<TemplateExpression> templateExpressions = workflow.getTemplateExpressions();

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

    if (templateExpressions == null || templateExpressions.size() == 0) {
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

      EntityVersion entityVersion = entityVersionService.newEntityVersion(workflow.getAppId(), WORKFLOW,
          workflow.getUuid(), workflow.getName(), ChangeType.UPDATED, workflow.getNotes());
      workflow.setDefaultVersion(entityVersion.getVersion());

      StateMachine stateMachine = new StateMachine(workflow, workflow.getDefaultVersion(),
          ((CustomOrchestrationWorkflow) orchestrationWorkflow).getGraph(), stencilMap());
      stateMachine = wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
      setUnset(ops, "defaultVersion", workflow.getDefaultVersion());
    }

    wingsPersistence.update(wingsPersistence.createQuery(Workflow.class)
                                .field("appId")
                                .equal(workflow.getAppId())
                                .field(ID_KEY)
                                .equal(workflow.getUuid()),
        ops);

    //-------------------
    // we need this method if we are supporting individual file or sub-directory git sync
    /*
    EntityUpdateListEvent eule = new EntityUpdateListEvent();

    // see if we need to perform any Git Sync operations for the workflow
    eule.addEntityUpdateEvent(entityUpdateService.workflowListUpdate(workflow, SourceType.ENTITY_UPDATE));

    entityUpdateService.queueEntityUpdateList(eule);
    */

    Application app = appService.get(workflow.getAppId());
    yamlDirectoryService.pushDirectory(app.getAccountId(), false);
    //-------------------

    workflow = readWorkflow(workflow.getAppId(), workflow.getUuid(), workflow.getDefaultVersion());
    return workflow;
  }

  /***
   * Populates the workflow level data to Phase. It Validates the service and inframapping for Basics and Multi Service
   * deployment. Resets Node selection if environment or inframapping changed.
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
      if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)) {
        handleBasicWorkflow((BasicOrchestrationWorkflow) orchestrationWorkflow, templateExpressions, appId, serviceId,
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
  private void handleBasicWorkflow(BasicOrchestrationWorkflow orchestrationWorkflow,
      List<TemplateExpression> templateExpressions, String appId, String serviceId, String inframappingId,
      boolean envChanged, boolean inframappingChanged) {
    BasicOrchestrationWorkflow basicOrchestrationWorkflow = orchestrationWorkflow;
    Optional<TemplateExpression> envExpression =
        templateExpressions.stream()
            .filter(templateExpression -> templateExpression.getFieldName().equals("envId"))
            .findAny();
    if (envExpression.isPresent()) {
      basicOrchestrationWorkflow.addToUserVariables(Arrays.asList(envExpression.get()));
    }
    if (basicOrchestrationWorkflow.getWorkflowPhases() != null) {
      for (WorkflowPhase phase : basicOrchestrationWorkflow.getWorkflowPhases()) {
        setTemplateExpresssionsToPhase(templateExpressions, phase);
        validateServiceCompatibility(appId, serviceId, phase.getServiceId());
        setServiceId(serviceId, phase);
        setInframappingDetails(appId, inframappingId, phase, envChanged, inframappingChanged);
        if (inframappingChanged || envChanged) {
          resetNodeSelection(phase);
        }
      }
    }
    Map<String, WorkflowPhase> rollbackWorkflowPhaseIdMap = basicOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap();
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
    if (userVariables == null || userVariables.size() == 0) {
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
    if (isEnvironmentTemplatized(templateExpressions) && !isInfraTemplatized(phaseTemplateExpressions)) {
      Service service = serviceResourceService.get(appId, workflowPhase.getServiceId(), false);
      Validator.notNullCheck("Service", service);
      TemplateExpression templateExpression = new TemplateExpression();

      Map<String, Object> metaData = new HashMap<>();
      metaData.put(ENTITY_TYPE, INFRASTRUCTURE_MAPPING.name());
      if (service.getArtifactType() != null) {
        metaData.put(ARTIFACT_TYPE, service.getArtifactType().name());
      }
      String expression = "${ServiceInfra";
      int i = 0;
      for (String serviceInfraVariable : serviceInfraVariables) {
        if (serviceInfraVariable.startsWith("ServiceInfra") || serviceInfraVariable.startsWith("ServiceInfra" + i)) {
          i++;
        }
      }
      if (i != 0) {
        expression = expression + i;
      }
      DeploymentType deploymentType = workflowPhase.getDeploymentType();
      if (deploymentType.equals(DeploymentType.SSH)) {
        expression = expression + "_SSH";
      } else if (deploymentType.equals(DeploymentType.AWS_CODEDEPLOY)) {
        expression = expression + "_AWS_CodeDeploy";
      } else if (deploymentType.equals(DeploymentType.ECS)) {
        expression = expression + "_ECS";
      } else if (deploymentType.equals(DeploymentType.KUBERNETES)) {
        expression = expression + "_Kubernetes";
      }
      expression = expression + "}";
      templateExpression.setFieldName("infraMappingId");
      templateExpression.setMetadata(metaData);
      templateExpression.setExpression(expression);
      phaseTemplateExpressions.add(templateExpression);
    }
  }

  /***
   *
   * @param templateExpressions
   * @return
   */
  private boolean isEnvironmentTemplatized(List<TemplateExpression> templateExpressions) {
    if (templateExpressions != null) {
      for (TemplateExpression templateExpression : templateExpressions) {
        Map<String, Object> metadata = templateExpression.getMetadata();
        if (metadata != null) {
          if (metadata.get(ENTITY_TYPE) != null) {
            EntityType entityType = valueOf((String) metadata.get(ENTITY_TYPE));
            if (entityType.equals(ENVIRONMENT)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }
  /***
   *
   * @param templateExpressions
   * @return
   */
  private boolean isInfraTemplatized(List<TemplateExpression> templateExpressions) {
    if (templateExpressions != null) {
      for (TemplateExpression templateExpression : templateExpressions) {
        Map<String, Object> metadata = templateExpression.getMetadata();
        if (metadata != null) {
          if (metadata.get(ENTITY_TYPE) != null) {
            EntityType entityType = valueOf((String) metadata.get(ENTITY_TYPE));
            if (entityType.equals(INFRASTRUCTURE_MAPPING)) {
              return true;
            }
          }
        }
      }
    }
    return false;
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
    if (workflowPhase != null) {
      List<TemplateExpression> phaseTemplateExpressions = new ArrayList<>();
      Optional<TemplateExpression> serviceExpression =
          templateExpressions.stream()
              .filter(templateExpression -> templateExpression.getFieldName().equals("serviceId"))
              .findAny();
      Optional<TemplateExpression> infraExpression =
          templateExpressions.stream()
              .filter(templateExpression -> templateExpression.getFieldName().equals("infraMappingId"))
              .findAny();
      if (serviceExpression.isPresent()) {
        phaseTemplateExpressions.add(serviceExpression.get());
      }
      if (infraExpression.isPresent()) {
        phaseTemplateExpressions.add(infraExpression.get());
      }
      workflowPhase.setTemplateExpressions(phaseTemplateExpressions);
    }
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
      if (phaseTemplateExpressions == null || phaseTemplateExpressions.size() == 0) {
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
    if (envExpression.isPresent()) {
      // Infra not present
      if (!infraExpression.isPresent()) {
        throw new WingsException(INVALID_REQUEST, "message",
            "Service Infrastructure cannot be de-templatized because Environment is templatized");
      }
    }
    if (serviceExpression.isPresent()) {
      // Infra not present
      if (!infraExpression.isPresent()) {
        throw new WingsException(INVALID_REQUEST, "message",
            "Service Infrastructure cannot be de-templatized because Service is templatized");
      }
    }
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
    if (serviceId != null && oldServiceId != null) {
      if (!serviceId.equals(oldServiceId)) {
        Service oldService = serviceResourceService.get(appId, oldServiceId, false);
        Validator.notNullCheck("service", oldService);
        Service newService = serviceResourceService.get(appId, serviceId, false);
        Validator.notNullCheck("service", newService);
        if (oldService.getArtifactType() != null
            && !oldService.getArtifactType().equals(newService.getArtifactType())) {
          throw new WingsException(INVALID_REQUEST, "message",
              "Service [" + newService.getName() + "] is not compatible with the service [" + oldService.getName()
                  + "]");
        }
      }
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
        Validator.notNullCheck("InfraMapping", infrastructureMapping);
        phase.setComputeProviderId(infrastructureMapping.getComputeProviderSettingId());
        phase.setInfraMappingName(infrastructureMapping.getDisplayName());
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
        List<Node> steps = phaseStep.getSteps();
        if (steps != null) {
          for (Node step : steps) {
            if (step.getType().equals(DC_NODE_SELECT.name()) || step.getType().equals(AWS_NODE_SELECT.name())) {
              Map<String, Object> properties = step.getProperties();
              if ((Boolean) properties.get("specificHosts")) {
                properties.put("specificHosts", new Boolean(false));
                properties.remove("hostNames");
              }
            }
          }
        }
      }
    }
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

    boolean deleted;
    if (forceDelete) {
      deleted = wingsPersistence.delete(Workflow.class, appId, workflowId);
    } else {
      ensureWorkflowSafeToDelete(workflow);
      if (workflowExecutionService.workflowExecutionsRunning(workflow.getWorkflowType(), appId, workflowId)) {
        String message = String.format("Workflow: [%s] couldn't be deleted", workflow.getName());
        throw new WingsException(WORKFLOW_EXECUTION_IN_PROGRESS, "message", message);
      }
      deleted = wingsPersistence.delete(Workflow.class, appId, workflowId);
    }
    if (deleted) {
      executorService.submit(() -> artifactStreamService.deleteStreamActionForWorkflow(appId, workflowId));
    }
    return deleted;
  }

  private void ensureWorkflowSafeToDelete(Workflow workflow) {
    List<Pipeline> pipelines = pipelineService.listPipelines(
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, workflow.getAppId())
            .addFilter("pipelineStages.pipelineStageElements.properties.workflowId", EQ, workflow.getUuid())
            .build());

    if (pipelines.size() > 0) {
      List<String> pipelineNames = pipelines.stream().map(Pipeline::getName).collect(Collectors.toList());
      throw new WingsException(INVALID_REQUEST, "message",
          String.format("Workflow is referenced by %s pipeline%s [%s].", pipelines.size(),
              pipelines.size() == 1 ? "" : "s", Joiner.on(", ").join(pipelineNames)));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StateMachine readLatestStateMachine(String appId, String originId) {
    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    filter.setFieldValues(appId);
    filter.setOp(Operator.EQ);
    req.addFilter(filter);

    filter.setFieldName("originId");
    filter.setFieldValues(originId);
    filter.setOp(Operator.EQ);
    req.addFilter(filter);

    return wingsPersistence.get(StateMachine.class, req);
  }

  public StateMachine readStateMachine(String appId, String stateMachineId) {
    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    filter.setFieldValues(appId);
    filter.setOp(Operator.EQ);
    req.addFilter(filter);

    return wingsPersistence.get(StateMachine.class, req);
  }

  @Override
  public StateMachine readStateMachine(String appId, String originId, Integer version) {
    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    filter.setFieldValues(appId);
    filter.setOp(Operator.EQ);
    req.addFilter(filter);

    filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(originId);
    filter.setOp(Operator.EQ);
    req.addFilter(filter);

    filter = new SearchFilter();
    filter.setFieldName("originVersion");
    filter.setFieldValues(version);
    filter.setOp(Operator.EQ);
    req.addFilter(filter);

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
    if (workflows == null || workflows.isEmpty()) {
      return createDefaultSimpleWorkflow(appId, envId);
    }
    return workflows.get(0);
  }

  @Override
  public void deleteWorkflowByApplication(String appId) {
    List<Key<Workflow>> workflowKeys =
        wingsPersistence.createQuery(Workflow.class).field("appId").equal(appId).asKeyList();
    for (Key key : workflowKeys) {
      deleteWorkflow(appId, (String) key.getId(), true);
    }
  }

  @Override
  public void deleteStateMachinesByApplication(String appId) {
    wingsPersistence.delete(wingsPersistence.createQuery(StateMachine.class).field("appId").equal(appId));
  }

  @Override
  public void deleteWorkflowByEnvironment(String appId, String envId) {
    wingsPersistence.createQuery(Workflow.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .asKeyList()
        .forEach(key -> deleteWorkflow(appId, key.getId().toString()));
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
    PageRequest<Workflow> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", appId, EQ);
    return listWorkflows(pageRequest).stream().collect(toMap(Workflow::getUuid, o -> (o.getName())));
  }

  @Override
  public PhaseStep updatePreDeployment(String appId, String workflowId, PhaseStep phaseStep) {
    Workflow workflow = readWorkflow(appId, workflowId);
    Validator.notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);
    orchestrationWorkflow.setPreDeploymentSteps(phaseStep);

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getPreDeploymentSteps();
  }

  @Override
  public PhaseStep updatePostDeployment(String appId, String workflowId, PhaseStep phaseStep) {
    Workflow workflow = readWorkflow(appId, workflowId);
    Validator.notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);
    orchestrationWorkflow.setPostDeploymentSteps(phaseStep);

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getPostDeploymentSteps();
  }

  @Override
  public WorkflowPhase createWorkflowPhase(String appId, String workflowId, WorkflowPhase workflowPhase) {
    Validator.notNullCheck("workflow", workflowPhase);

    validateServiceandInframapping(appId, workflowPhase.getServiceId(), workflowPhase.getInfraMappingId());

    Workflow workflow = readWorkflow(appId, workflowId);
    Validator.notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();

    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);

    attachWorkflowPhase(workflow, workflowPhase);

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
      throw new WingsException(INVALID_REQUEST, "message", "Service [" + serviceId + "] does not exist");
    }
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, inframappingId);
    if (infrastructureMapping == null) {
      throw new WingsException(
          INVALID_REQUEST, "message", "Service Infrastructure [" + inframappingId + "] does not exist");
    }
    if (!service.getUuid().equals(infrastructureMapping.getServiceId())) {
      throw new WingsException(INVALID_REQUEST, "message",
          "Service Infrastructure [" + infrastructureMapping.getDisplayName() + "] not mapped to Service ["
              + service.getName() + "]");
    }
  }

  @Override
  public WorkflowPhase cloneWorkflowPhase(String appId, String workflowId, WorkflowPhase workflowPhase) {
    logger.info("Cloning workflow phase for appId {}, workflowId {} workflowPhase {}", appId, workflowId,
        workflowPhase.getUuid());
    String phaseId = workflowPhase.getUuid();
    String phaseName = workflowPhase.getName();
    Workflow workflow = readWorkflow(appId, workflowId);
    Validator.notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);

    workflowPhase = orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId);
    Validator.notNullCheck("workflowPhase", workflowPhase);

    WorkflowPhase clonedWorkflowPhase = workflowPhase.clone();
    clonedWorkflowPhase.setName(phaseName);

    orchestrationWorkflow.getWorkflowPhases().add(clonedWorkflowPhase);

    WorkflowPhase rollbackWorkflowPhase =
        orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());

    if (rollbackWorkflowPhase != null) {
      WorkflowPhase clonedRollbackWorkflowPhase = rollbackWorkflowPhase.clone();
      orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(
          clonedWorkflowPhase.getUuid(), clonedRollbackWorkflowPhase);
    }

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    logger.info("Cloning workflow phase for appId {}, workflowId {} workflowPhase {} success", appId, workflowId,
        workflowPhase.getUuid());
    return orchestrationWorkflow.getWorkflowPhaseIdMap().get(clonedWorkflowPhase.getUuid());
  }

  private void attachWorkflowPhase(Workflow workflow, WorkflowPhase workflowPhase) {
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(workflow.getAppId(), workflowPhase.getInfraMappingId());
    Validator.notNullCheck("InfraMapping", infrastructureMapping);
    workflowPhase.setComputeProviderId(infrastructureMapping.getComputeProviderSettingId());
    workflowPhase.setInfraMappingName(infrastructureMapping.getDisplayName());
    workflowPhase.setDeploymentType(DeploymentType.valueOf(infrastructureMapping.getDeploymentType()));

    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(CANARY)) {
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
      boolean serviceRepeat = false;
      if (canaryOrchestrationWorkflow.getWorkflowPhaseIds() != null) {
        for (String phaseId : canaryOrchestrationWorkflow.getWorkflowPhaseIds()) {
          WorkflowPhase existingPhase = canaryOrchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId);
          if (existingPhase.getServiceId().equals(workflowPhase.getServiceId())
              && existingPhase.getDeploymentType() == workflowPhase.getDeploymentType()
              && (existingPhase.getInfraMappingId() != null
                     && existingPhase.getInfraMappingId().equals(workflowPhase.getInfraMappingId()))) {
            serviceRepeat = true;
            break;
          }
        }
      }
      generateNewWorkflowPhaseSteps(workflow.getAppId(), workflow.getEnvId(), workflowPhase, serviceRepeat);
      canaryOrchestrationWorkflow.getWorkflowPhases().add(workflowPhase);

      WorkflowPhase rollbackWorkflowPhase = generateRollbackWorkflowPhase(workflow.getAppId(), workflowPhase);
      canaryOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)) {
      BasicOrchestrationWorkflow basicOrchestrationWorkflow = (BasicOrchestrationWorkflow) orchestrationWorkflow;
      generateNewWorkflowPhaseSteps(workflow.getAppId(), workflow.getEnvId(), workflowPhase, false);
      basicOrchestrationWorkflow.getWorkflowPhases().add(workflowPhase);

      WorkflowPhase rollbackWorkflowPhase = generateRollbackWorkflowPhase(workflow.getAppId(), workflowPhase);
      basicOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(MULTI_SERVICE)) {
      MultiServiceOrchestrationWorkflow multiServiceOrchestrationWorkflow =
          (MultiServiceOrchestrationWorkflow) orchestrationWorkflow;
      boolean serviceRepeat = false;
      if (multiServiceOrchestrationWorkflow.getWorkflowPhaseIds() != null) {
        for (String phaseId : multiServiceOrchestrationWorkflow.getWorkflowPhaseIds()) {
          WorkflowPhase existingPhase = multiServiceOrchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId);
          if (existingPhase.getServiceId().equals(workflowPhase.getServiceId())
              && existingPhase.getDeploymentType() == workflowPhase.getDeploymentType()
              && (existingPhase.getInfraMappingId() != null
                     && existingPhase.getInfraMappingId().equals(workflowPhase.getInfraMappingId()))) {
            serviceRepeat = true;
            break;
          }
        }
      }
      generateNewWorkflowPhaseSteps(workflow.getAppId(), workflow.getEnvId(), workflowPhase, serviceRepeat);
      multiServiceOrchestrationWorkflow.getWorkflowPhases().add(workflowPhase);

      WorkflowPhase rollbackWorkflowPhase = generateRollbackWorkflowPhase(workflow.getAppId(), workflowPhase);
      multiServiceOrchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(
          workflowPhase.getUuid(), rollbackWorkflowPhase);
    }
  }

  @Override
  public WorkflowPhase updateWorkflowPhase(String appId, String workflowId, WorkflowPhase workflowPhase) {
    String infraMappingId = workflowPhase.getInfraMappingId();
    String serviceId = workflowPhase.getServiceId();
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId(), false);
    if (service == null) {
      throw new WingsException(
          INVALID_REQUEST, "message", "Service [" + workflowPhase.getServiceId() + "] does not exist");
    }
    if (infraMappingId == null) {
      throw new WingsException(
          INVALID_REQUEST, "message", String.format(WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE, workflowPhase.getName()));
    }
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
    Validator.notNullCheck("InfraMapping", infrastructureMapping);
    if (!service.getUuid().equals(infrastructureMapping.getServiceId())) {
      throw new WingsException(INVALID_REQUEST, "message",
          "Service Infrastructure [" + infrastructureMapping.getDisplayName() + "] not mapped to Service ["
              + service.getName() + "]");
    }
    workflowPhase.setComputeProviderId(infrastructureMapping.getComputeProviderSettingId());
    workflowPhase.setInfraMappingName(infrastructureMapping.getDisplayName());
    workflowPhase.setDeploymentType(DeploymentType.valueOf(infrastructureMapping.getDeploymentType()));

    Workflow workflow = readWorkflow(appId, workflowId);

    Validator.notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);
    Validator.notNullCheck("WorkflowPhase", orchestrationWorkflow.getWorkflowPhaseIdMap().get(workflowPhase.getUuid()));

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
    validateServiceCompatibility(appId, serviceId, oldServiceId);
    WorkflowPhase rollbackWorkflowPhase =
        orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase.getUuid());

    if (rollbackWorkflowPhase != null) {
      rollbackWorkflowPhase.setServiceId(serviceId);
      rollbackWorkflowPhase.setInfraMappingId(infraMappingId);
      rollbackWorkflowPhase.setComputeProviderId(infrastructureMapping.getComputeProviderSettingId());
      rollbackWorkflowPhase.setInfraMappingName(infrastructureMapping.getDisplayName());
      rollbackWorkflowPhase.setDeploymentType(DeploymentType.valueOf(infrastructureMapping.getDeploymentType()));

      orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
    }
    if (!infraMappingId.equals(oldInfraMappingId)) {
      inframappingChanged = true;
    }

    if (!found) {
      throw new WingsException(INVALID_REQUEST, "message", "No matching Workflow Phase");
    }

    // Propagate template expressions to workflow level
    if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)) {
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

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow, inframappingChanged, false)
            .getOrchestrationWorkflow();
    return orchestrationWorkflow.getWorkflowPhaseIdMap().get(workflowPhase.getUuid());
  }

  @Override
  public WorkflowPhase updateWorkflowPhaseRollback(
      String appId, String workflowId, String phaseId, WorkflowPhase rollbackWorkflowPhase) {
    Workflow workflow = readWorkflow(appId, workflowId);
    Validator.notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);
    Validator.notNullCheck("WorkflowPhase", orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId));

    orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(phaseId, rollbackWorkflowPhase);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseId);
  }

  @Override
  public void deleteWorkflowPhase(String appId, String workflowId, String phaseId) {
    Workflow workflow = readWorkflow(appId, workflowId);
    Validator.notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);
    Validator.notNullCheck("WorkflowPhase", orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId));

    orchestrationWorkflow.getWorkflowPhases().remove(orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId));
    orchestrationWorkflow.getWorkflowPhaseIdMap().remove(phaseId);
    orchestrationWorkflow.getWorkflowPhaseIds().remove(phaseId);
    orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().remove(phaseId);
    updateWorkflow(workflow, orchestrationWorkflow);
  }

  @Override
  public Node updateGraphNode(String appId, String workflowId, String subworkflowId, Node node) {
    Workflow workflow = readWorkflow(appId, workflowId);
    Validator.notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);

    Graph graph = orchestrationWorkflow.getGraph().getSubworkflows().get(subworkflowId);

    boolean found = false;
    for (int i = 0; i < graph.getNodes().size(); i++) {
      Node childNode = graph.getNodes().get(i);
      if (childNode.getId().equals(node.getId())) {
        graph.getNodes().remove(i);
        graph.getNodes().add(i, node);
        found = true;
        break;
      }
    }

    if (!found) {
      throw new WingsException(INVALID_REQUEST, "args", "node");
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
    Workflow clonedWorkflow = originalWorkflow.clone();
    clonedWorkflow.setName(workflow.getName());
    clonedWorkflow.setDescription(workflow.getDescription());
    Workflow savedWorkflow = createWorkflow(clonedWorkflow);
    if (originalWorkflow.getOrchestrationWorkflow() != null) {
      savedWorkflow.setOrchestrationWorkflow(originalWorkflow.getOrchestrationWorkflow().clone());
    }
    return updateWorkflow(savedWorkflow, savedWorkflow.getOrchestrationWorkflow(), false, false, false);
  }

  @Override
  public Workflow cloneWorkflow(String appId, String originalWorkflowId, CloneMetadata cloneMetadata) {
    Validator.notNullCheck("cloneMetadata", cloneMetadata);
    Workflow workflow = cloneMetadata.getWorkflow();
    Validator.notNullCheck("workflow", workflow);
    workflow.setAppId(appId);
    String targetAppId = cloneMetadata.getTargetAppId();
    if (targetAppId == null || targetAppId.equals(appId)) {
      return cloneWorkflow(appId, originalWorkflowId, workflow);
    } else {
      logger.info(
          "Cloning workflow across applications. Environment, Service Infrastructure and Node selection will not be cloned");
      validateServiceMapping(appId, targetAppId, cloneMetadata.getServiceMapping());
      Workflow originalWorkflow = readWorkflow(appId, originalWorkflowId);
      Workflow clonedWorkflow = originalWorkflow.clone();
      clonedWorkflow.setName(workflow.getName());
      clonedWorkflow.setDescription(workflow.getDescription());
      clonedWorkflow.setAppId(targetAppId);
      clonedWorkflow.setEnvId(null);
      Workflow savedWorkflow = createWorkflow(clonedWorkflow);
      OrchestrationWorkflow orchestrationWorkflow = originalWorkflow.getOrchestrationWorkflow();
      if (orchestrationWorkflow != null) {
        OrchestrationWorkflow clonedOrchestrationWorkflow = orchestrationWorkflow.clone();
        // Set service ids
        clonedOrchestrationWorkflow.setCloneMetadata(cloneMetadata.getServiceMapping());
        savedWorkflow.setOrchestrationWorkflow(clonedOrchestrationWorkflow);
      }
      return updateWorkflow(savedWorkflow, savedWorkflow.getOrchestrationWorkflow(), false, true, true);
    }
  }

  /**
   * Validates whether service id and mapped service are of same type
   *
   * @param serviceMapping
   */
  private void validateServiceMapping(String appId, String targetAppId, Map<String, String> serviceMapping) {
    if (serviceMapping == null) {
      throw new WingsException(
          INVALID_REQUEST, "message", "At least one service mapping required to clone across applications");
    }
    Set<String> serviceIds = serviceMapping.keySet();
    for (String serviceId : serviceIds) {
      String targetServiceId = serviceMapping.get(serviceId);
      if (serviceId != null && targetServiceId != null) {
        Service oldService = serviceResourceService.get(appId, serviceId, false);
        Validator.notNullCheck("service", oldService);
        Service newService = serviceResourceService.get(targetAppId, targetServiceId, false);
        Validator.notNullCheck("targetService", newService);
        if (oldService.getArtifactType() != null
            && !oldService.getArtifactType().equals(newService.getArtifactType())) {
          throw new WingsException(INVALID_REQUEST, "message",
              "Target service  [" + oldService.getName() + " ] is not compatible with service [" + newService.getName()
                  + "]");
        }
      }
    }
  }

  @Override
  public Workflow updateWorkflow(String appId, String workflowId, Integer defaultVersion) {
    Workflow workflow = readWorkflow(appId, workflowId, null);
    wingsPersistence.update(
        workflow, wingsPersistence.createUpdateOperations(Workflow.class).set("defaultVersion", defaultVersion));

    //-------------------
    // we need this method if we are supporting individual file or sub-directory git sync
    /*
    EntityUpdateListEvent eule = new EntityUpdateListEvent();

    // see if we need to perform any Git Sync operations for the workflow
    eule.addEntityUpdateEvent(entityUpdateService.workflowListUpdate(workflow, SourceType.ENTITY_UPDATE));

    entityUpdateService.queueEntityUpdateList(eule);
    */

    Application app = appService.get(appId);
    yamlDirectoryService.pushDirectory(app.getAccountId(), false);
    //-------------------

    return readWorkflow(appId, workflowId, defaultVersion);
  }

  @Override
  public List<NotificationRule> updateNotificationRules(
      String appId, String workflowId, List<NotificationRule> notificationRules) {
    Workflow workflow = readWorkflow(appId, workflowId);
    Validator.notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);

    orchestrationWorkflow.setNotificationRules(notificationRules);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getNotificationRules();
  }

  @Override
  public List<FailureStrategy> updateFailureStrategies(
      String appId, String workflowId, List<FailureStrategy> failureStrategies) {
    Workflow workflow = readWorkflow(appId, workflowId);
    Validator.notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);

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
    Validator.notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);

    orchestrationWorkflow.setUserVariables(userVariables);
    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
    return orchestrationWorkflow.getUserVariables();
  }

  private Set<EntityType> updateRequiredEntityTypes(String appId, OrchestrationWorkflow orchestrationWorkflow) {
    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);
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

    if (Arrays
            .asList(
                DeploymentType.ECS, DeploymentType.KUBERNETES, DeploymentType.AWS_CODEDEPLOY, DeploymentType.AWS_LAMBDA)
            .contains(workflowPhase.getDeploymentType())) {
      requiredEntityTypes.add(ARTIFACT);
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
      for (Node step : phaseStep.getSteps()) {
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

  private void generateNewWorkflowPhaseSteps(
      String appId, String envId, WorkflowPhase workflowPhase, boolean serviceRepeat) {
    DeploymentType deploymentType = workflowPhase.getDeploymentType();
    if (deploymentType == DeploymentType.ECS) {
      generateNewWorkflowPhaseStepsForECS(appId, workflowPhase, !serviceRepeat);
    } else if (deploymentType == DeploymentType.KUBERNETES) {
      generateNewWorkflowPhaseStepsForKubernetes(appId, workflowPhase, !serviceRepeat);
    } else if (deploymentType == DeploymentType.AWS_CODEDEPLOY) {
      generateNewWorkflowPhaseStepsForAWSCodeDeploy(appId, workflowPhase);
    } else if (deploymentType == DeploymentType.AWS_LAMBDA) {
      generateNewWorkflowPhaseStepsForAWSLambda(appId, envId, workflowPhase);
    } else {
      generateNewWorkflowPhaseStepsForSSH(appId, workflowPhase);
    }
  }

  private void generateNewWorkflowPhaseStepsForAWSLambda(String appId, String envId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    workflowPhase.addPhaseStep(aPhaseStep(PREPARE_STEPS, Constants.PREPARE_STEPS).build());

    workflowPhase.addPhaseStep(
        aPhaseStep(DEPLOY_AWS_LAMBDA, Constants.DEPLOY_SERVICE)
            .addStep(aNode().withId(getUuid()).withType(AWS_LAMBDA_STATE.name()).withName(Constants.AWS_LAMBDA).build())
            .build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private void generateNewWorkflowPhaseStepsForAWSCodeDeploy(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    workflowPhase.addPhaseStep(aPhaseStep(PREPARE_STEPS, Constants.PREPARE_STEPS).build());

    workflowPhase.addPhaseStep(aPhaseStep(DEPLOY_AWSCODEDEPLOY, Constants.DEPLOY_SERVICE)
                                   .addStep(aNode()
                                                .withId(getUuid())
                                                .withType(AWS_CODEDEPLOY_STATE.name())
                                                .withName(Constants.AWS_CODE_DEPLOY)
                                                //.addProperty()
                                                .build())
                                   .build());

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
        workflowPhase.addPhaseStep(
            aPhaseStep(CLUSTER_SETUP, Constants.SETUP_CLUSTER)
                .addStep(
                    aNode().withId(getUuid()).withType(AWS_CLUSTER_SETUP.name()).withName("AWS Cluster Setup").build())
                .build());
      }
      workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                                     .addStep(aNode()
                                                  .withId(getUuid())
                                                  .withType(ECS_SERVICE_SETUP.name())
                                                  .withName(Constants.ECS_SERVICE_SETUP)
                                                  .build())
                                     .build());
    }
    workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                                   .addStep(aNode()
                                                .withId(getUuid())
                                                .withType(ECS_SERVICE_DEPLOY.name())
                                                .withName(Constants.UPGRADE_CONTAINERS)
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
        workflowPhase.addPhaseStep(
            aPhaseStep(CLUSTER_SETUP, Constants.SETUP_CLUSTER)
                .addStep(
                    aNode().withId(getUuid()).withType(GCP_CLUSTER_SETUP.name()).withName("GCP Cluster Setup").build())
                .build());
      }
      workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                                     .addStep(aNode()
                                                  .withId(getUuid())
                                                  .withType(KUBERNETES_REPLICATION_CONTROLLER_SETUP.name())
                                                  .withName("Kubernetes Service Setup")
                                                  .build())
                                     .build());
    }

    workflowPhase.addPhaseStep(aPhaseStep(CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                                   .addStep(aNode()
                                                .withId(getUuid())
                                                .withType(KUBERNETES_REPLICATION_CONTROLLER_DEPLOY.name())
                                                .withName("Upgrade Containers")
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());
    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private void generateNewWorkflowPhaseStepsForSSH(String appId, WorkflowPhase workflowPhase) {
    // For DC only - for other types it has to be customized

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
    StateType stateType =
        infrastructureMapping.getComputeProviderType().equals(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
        ? DC_NODE_SELECT
        : AWS_NODE_SELECT;

    if (!Arrays.asList(DC_NODE_SELECT, AWS_NODE_SELECT).contains(stateType)) {
      throw new WingsException(INVALID_REQUEST, "message", "Unsupported state type: " + stateType);
    }

    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    workflowPhase.addPhaseStep(aPhaseStep(PROVISION_NODE, Constants.PROVISION_NODE_NAME)
                                   .addStep(aNode()
                                                .withType(stateType.name())
                                                .withName("Select Nodes")
                                                .addProperty("specificHosts", false)
                                                .addProperty("instanceCount", 1)
                                                .build())
                                   .build());

    List<Node> disableServiceSteps = commandNodes(commandMap, CommandType.DISABLE);
    List<Node> enableServiceSteps = commandNodes(commandMap, CommandType.ENABLE);

    if (attachElbSteps(infrastructureMapping)) {
      disableServiceSteps.add(aNode()
                                  .withType(ELASTIC_LOAD_BALANCER.name())
                                  .withName("Elastic Load Balancer")
                                  .addProperty("operation", Operation.Disable)
                                  .build());
      enableServiceSteps.add(aNode()
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

    workflowPhase.addPhaseStep(aPhaseStep(VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(
        aPhaseStep(ENABLE_SERVICE, Constants.ENABLE_SERVICE).addAllSteps(enableServiceSteps).build());

    // Not needed for non-DC
    // workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.DEPROVISION_NODE).build());

    workflowPhase.addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build());
  }

  private boolean attachElbSteps(InfrastructureMapping infrastructureMapping) {
    return (infrastructureMapping instanceof PhysicalInfrastructureMapping
               && StringUtils.isNotBlank(((PhysicalInfrastructureMapping) infrastructureMapping).getLoadBalancerId()))
        || (infrastructureMapping instanceof AwsInfrastructureMapping
               && StringUtils.isNotBlank(((AwsInfrastructureMapping) infrastructureMapping).getLoadBalancerId()));
  }

  private WorkflowPhase generateRollbackWorkflowPhase(String appId, WorkflowPhase workflowPhase) {
    DeploymentType deploymentType = workflowPhase.getDeploymentType();
    if (deploymentType == DeploymentType.ECS) {
      return generateRollbackWorkflowPhaseForContainerService(workflowPhase, ECS_SERVICE_ROLLBACK.name());
    } else if (deploymentType == DeploymentType.KUBERNETES) {
      return generateRollbackWorkflowPhaseForContainerService(
          workflowPhase, KUBERNETES_REPLICATION_CONTROLLER_ROLLBACK.name());
    } else if (deploymentType == DeploymentType.AWS_CODEDEPLOY) {
      return generateRollbackWorkflowPhaseForAwsCodeDeploy(workflowPhase, AWS_CODEDEPLOY_ROLLBACK.name());
    } else if (deploymentType == DeploymentType.AWS_LAMBDA) {
      return generateRollbackWorkflowPhaseForAwsLambda(workflowPhase, AWS_LAMBDA_ROLLBACK.name());
    } else {
      return generateRollbackWorkflowPhaseForSSH(appId, workflowPhase);
    }
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForAwsLambda(
      WorkflowPhase workflowPhase, String containerServiceType) {
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
                          .addStep(aNode()
                                       .withId(getUuid())
                                       .withType(containerServiceType)
                                       .withName(Constants.ROLLBACK_AWS_LAMBDA)
                                       .addProperty("rollback", true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build())
        .build();
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForContainerService(
      WorkflowPhase workflowPhase, String containerServiceType) {
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
                          .addStep(aNode()
                                       .withId(getUuid())
                                       .withType(containerServiceType)
                                       .withName(Constants.ROLLBACK_CONTAINERS)
                                       .addProperty("rollback", true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_CONTAINERS)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build())
        .build();
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForAwsCodeDeploy(
      WorkflowPhase workflowPhase, String containerServiceType) {
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
                          .addStep(aNode()
                                       .withId(getUuid())
                                       .withType(containerServiceType)
                                       .withName(Constants.ROLLBACK_AWS_CODE_DEPLOY)
                                       .addProperty("rollback", true)
                                       .build())
                          .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                          .withStatusForRollback(ExecutionStatus.SUCCESS)
                          .withRollback(true)
                          .build())
        .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build())
        .build();
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForSSH(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
    StateType stateType =
        infrastructureMapping.getComputeProviderType().equals(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
        ? DC_NODE_SELECT
        : AWS_NODE_SELECT;

    List<Node> disableServiceSteps = commandNodes(commandMap, CommandType.DISABLE, true);
    List<Node> enableServiceSteps = commandNodes(commandMap, CommandType.ENABLE, true);

    if (attachElbSteps(infrastructureMapping)) {
      disableServiceSteps.add(aNode()
                                  .withType(ELASTIC_LOAD_BALANCER.name())
                                  .withName("Elastic Load Balancer")
                                  .addProperty("operation", Operation.Disable)
                                  .withRollback(true)
                                  .build());
      enableServiceSteps.add(aNode()
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
                              .withRollback(true)
                              .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .build())
            .addPhaseStep(aPhaseStep(DEPLOY_SERVICE, Constants.DEPLOY_SERVICE)
                              .addAllSteps(commandNodes(commandMap, CommandType.INSTALL, true))
                              .withRollback(true)
                              .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .build())
            .addPhaseStep(aPhaseStep(ENABLE_SERVICE, Constants.ENABLE_SERVICE)
                              .addAllSteps(enableServiceSteps)
                              .withRollback(true)
                              .withPhaseStepNameForRollback(Constants.DISABLE_SERVICE)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .build())
            .addPhaseStep(aPhaseStep(WRAP_UP, Constants.WRAP_UP).build())
            .build();
    ;

    // get provision NODE
    Optional<PhaseStep> provisionPhaseStep =
        workflowPhase.getPhaseSteps().stream().filter(ps -> ps.getPhaseStepType() == PROVISION_NODE).findFirst();
    if (provisionPhaseStep.isPresent() && provisionPhaseStep.get().getSteps() != null) {
      Optional<Node> awsProvisionNode =
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

  private Map<CommandType, List<Command>> getCommandTypeListMap(Service service) {
    Map<CommandType, List<Command>> commandMap = new HashMap<>();
    if (service.getServiceCommands() == null) {
      return commandMap;
    }
    for (ServiceCommand sc : service.getServiceCommands()) {
      if (sc.getCommand() == null || sc.getCommand().getCommandType() == null) {
        continue;
      }
      commandMap.computeIfAbsent(sc.getCommand().getCommandType(), k -> new ArrayList<>()).add(sc.getCommand());
    }
    return commandMap;
  }

  private List<Node> commandNodes(Map<CommandType, List<Command>> commandMap, CommandType commandType) {
    return commandNodes(commandMap, commandType, false);
  }

  private List<Node> commandNodes(
      Map<CommandType, List<Command>> commandMap, CommandType commandType, boolean rollback) {
    List<Node> nodes = new ArrayList<>();

    List<Command> commands = commandMap.get(commandType);
    if (commands == null) {
      return nodes;
    }

    for (Command command : commands) {
      nodes.add(aNode()
                    .withId(getUuid())
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
    // TODO: We should be able to get Logged On User Admin role dynamically
    String name = RoleType.ACCOUNT_ADMIN.getDisplayName();
    List<NotificationGroup> notificationGroups =
        notificationSetupService.listNotificationGroups(app.getAccountId(), name);
    if (notificationGroups == null || notificationGroups.isEmpty()) {
      logger.warn("Default notification group not created for account {}. Ignoring adding notification group",
          account.getAccountName());
      return;
    }
    List<ExecutionStatus> conditions = new ArrayList<>();
    conditions.add(ExecutionStatus.FAILED);
    NotificationRule notificationRule = aNotificationRule()
                                            .withConditions(conditions)
                                            .withExecutionScope(ExecutionScope.WORKFLOW)
                                            .withNotificationGroups(notificationGroups)
                                            .build();
    List<NotificationRule> notificationRules = new ArrayList<>();
    notificationRules.add(notificationRule);
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(CANARY)) {
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
      canaryOrchestrationWorkflow.setNotificationRules(notificationRules);
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)) {
      BasicOrchestrationWorkflow basicOrchestrationWorkflow = (BasicOrchestrationWorkflow) orchestrationWorkflow;
      basicOrchestrationWorkflow.setNotificationRules(notificationRules);
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(MULTI_SERVICE)) {
      MultiServiceOrchestrationWorkflow multiServiceOrchestrationWorkflow =
          (MultiServiceOrchestrationWorkflow) orchestrationWorkflow;
      Validator.notNullCheck("multiServiceOrchestrationWorkflow", multiServiceOrchestrationWorkflow);
      multiServiceOrchestrationWorkflow.setNotificationRules(notificationRules);
    }
  }

  private void createDefaultFailureStrategy(Workflow workflow) {
    List<FailureStrategy> failureStrategies = new ArrayList<>();
    failureStrategies.add(aFailureStrategy()
                              .addFailureTypes(FailureType.APPLICATION_ERROR)
                              .withExecutionScope(ExecutionScope.WORKFLOW)
                              .withRepairActionCode(RepairActionCode.ROLLBACK_WORKFLOW)
                              .build());
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(CANARY)) {
      CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
      canaryOrchestrationWorkflow.setFailureStrategies(failureStrategies);
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)) {
      BasicOrchestrationWorkflow basicOrchestrationWorkflow = (BasicOrchestrationWorkflow) orchestrationWorkflow;
      basicOrchestrationWorkflow.setFailureStrategies(failureStrategies);
    } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(MULTI_SERVICE)) {
      MultiServiceOrchestrationWorkflow multiServiceOrchestrationWorkflow =
          (MultiServiceOrchestrationWorkflow) orchestrationWorkflow;
      multiServiceOrchestrationWorkflow.setFailureStrategies(failureStrategies);
    }
  }

  private void validateBasicWorkflow(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow != null && orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)) {
      // Create Single Phase
      Validator.notNullCheck("infraMappingId", workflow.getInfraMappingId());
      Validator.notNullCheck("serviceId", workflow.getServiceId());
    }
  }
}
