/**
 *
 */

package software.wings.service.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.ErrorCode.WORKFLOW_EXECUTION_IN_PROGRESS;
import static software.wings.beans.FailureStrategy.FailureStrategyBuilder.aFailureStrategy;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.OrchestrationWorkflowType.BASIC;
import static software.wings.beans.OrchestrationWorkflowType.CANARY;
import static software.wings.beans.OrchestrationWorkflowType.MULTI_SERVICE;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.StateMachineExecutionSimulator.populateRequiredEntityTypesByAccessType;
import static software.wings.sm.StateType.AWS_CODEDEPLOY_ROLLBACK;
import static software.wings.sm.StateType.AWS_CODEDEPLOY_STATE;
import static software.wings.sm.StateType.AWS_NODE_SELECT;
import static software.wings.sm.StateType.COMMAND;
import static software.wings.sm.StateType.DC_NODE_SELECT;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ECS_SERVICE_ROLLBACK;
import static software.wings.sm.StateType.ELASTIC_LOAD_BALANCER;
import static software.wings.sm.StateType.KUBERNETES_REPLICATION_CONTROLLER_DEPLOY;
import static software.wings.sm.StateType.KUBERNETES_REPLICATION_CONTROLLER_ROLLBACK;
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
import software.wings.beans.ErrorCode;
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
import software.wings.beans.PhaseStepType;
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
        OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
        if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
          workflowPhase = ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhaseIdMap().get(phaseId);
        } else if (orchestrationWorkflow instanceof BasicOrchestrationWorkflow) {
          workflowPhase = ((BasicOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhaseIdMap().get(phaseId);
        } else if (orchestrationWorkflow instanceof MultiServiceOrchestrationWorkflow) {
          workflowPhase =
              ((MultiServiceOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhaseIdMap().get(phaseId);
        }
        InfrastructureMapping infrastructureMapping =
            infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
        predicate = stencil -> stencil.matches(infrastructureMapping);
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
      workflows.getResponse().forEach(
          workflow -> { loadOrchestrationWorkflow(workflow, workflow.getDefaultVersion()); });
    }

    if (previousExecutionsCount != null && previousExecutionsCount > 0) {
      for (Workflow workflow : workflows) {
        PageRequest<WorkflowExecution> workflowExecutionPageRequest =
            aPageRequest()
                .withLimit(previousExecutionsCount.toString())
                .addFilter("workflowId", EQ, workflow.getUuid())
                .build();

        workflow.setWorkflowExecutions(
            workflowExecutionService.listExecutions(workflowExecutionPageRequest, false, false, false, false)
                .getResponse());
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
      return workflow;
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
    if (workflow.getOrchestrationWorkflow() != null) {
      workflow.getOrchestrationWorkflow().onLoad();
    }

    populateServices(workflow);
  }

  private void populateServices(Workflow workflow) {
    if (workflow == null || workflow.getOrchestrationWorkflow() == null
        || workflow.getOrchestrationWorkflow().getServiceIds() == null) {
      return;
    }

    List<Service> services = workflow.getOrchestrationWorkflow()
                                 .getServiceIds()
                                 .stream()
                                 .map(serviceId -> serviceResourceService.get(workflow.getAppId(), serviceId, false))
                                 .filter(Objects::nonNull)
                                 .collect(Collectors.toList());
    workflow.setServices(services);
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
        workflow.getAppId(), EntityType.WORKFLOW, key, workflow.getName(), ChangeType.CREATED, workflow.getNotes());

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
    return updateWorkflow(workflow, orchestrationWorkflow, true);
  }

  private Workflow updateWorkflow(
      Workflow workflow, OrchestrationWorkflow orchestrationWorkflow, boolean onSaveCallNeeded) {
    UpdateOperations<Workflow> ops = wingsPersistence.createUpdateOperations(Workflow.class);
    setUnset(ops, "description", workflow.getDescription());
    setUnset(ops, "name", workflow.getName());
    setUnset(ops, "templateExpressions", workflow.getTemplateExpressions());

    if (workflow.isTemplatized() || workflow.getTemplateExpressions() != null) {
      if (orchestrationWorkflow == null) {
        workflow = readWorkflow(workflow.getAppId(), workflow.getUuid(), workflow.getDefaultVersion());
        orchestrationWorkflow = workflow.getOrchestrationWorkflow();
      }
      orchestrationWorkflow = propagateTemplateExpressions(orchestrationWorkflow, workflow.getTemplateExpressions());
    }
    if (orchestrationWorkflow != null) {
      if (onSaveCallNeeded) {
        orchestrationWorkflow.onSave();
        updateRequiredEntityTypes(workflow.getAppId(), orchestrationWorkflow);
      }

      EntityVersion entityVersion = entityVersionService.newEntityVersion(workflow.getAppId(), EntityType.WORKFLOW,
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

    workflow = readWorkflow(workflow.getAppId(), workflow.getUuid(), workflow.getDefaultVersion());
    return workflow;
  }

  private OrchestrationWorkflow propagateTemplateExpressions(
      OrchestrationWorkflow orchestrationWorkflow, List<TemplateExpression> templateExpressions) {
    if (orchestrationWorkflow != null) {
      if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)) {
        BasicOrchestrationWorkflow basicOrchestrationWorkflow = (BasicOrchestrationWorkflow) orchestrationWorkflow;
        if (basicOrchestrationWorkflow.getWorkflowPhases() != null) {
          for (WorkflowPhase phase : basicOrchestrationWorkflow.getWorkflowPhases()) {
            phase.setTemplateExpressions(templateExpressions);
          }
        }
      } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(MULTI_SERVICE)) {
        MultiServiceOrchestrationWorkflow multiServiceOrchestrationWorkflow =
            (MultiServiceOrchestrationWorkflow) orchestrationWorkflow;
        if (multiServiceOrchestrationWorkflow.getWorkflowPhases() != null) {
          for (WorkflowPhase phase : multiServiceOrchestrationWorkflow.getWorkflowPhases()) {
            phase.setTemplateExpressions(templateExpressions);
          }
        }
      }
    }
    return orchestrationWorkflow;
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

    ensureWorkflowSafeToDelete(workflow);

    boolean deleted = false;
    if (forceDelete) {
      deleted = wingsPersistence.delete(Workflow.class, appId, workflowId);
    } else {
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
          String.format("Workflow is referenced by %s pipline%s [%s].", pipelines.size(),
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
              && existingPhase.getInfraMappingId().equals(workflowPhase.getInfraMappingId())) {
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
              && existingPhase.getInfraMappingId().equals(workflowPhase.getInfraMappingId())) {
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
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
    Validator.notNullCheck("InfraMapping", infrastructureMapping);

    Workflow workflow = readWorkflow(appId, workflowId);
    Validator.notNullCheck("workflow", workflow);
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);
    Validator.notNullCheck("WorkflowPhase", orchestrationWorkflow.getWorkflowPhaseIdMap().get(workflowPhase.getUuid()));

    boolean found = false;
    for (int i = 0; i < orchestrationWorkflow.getWorkflowPhases().size(); i++) {
      if (orchestrationWorkflow.getWorkflowPhases().get(i).getUuid().equals(workflowPhase.getUuid())) {
        orchestrationWorkflow.getWorkflowPhases().remove(i);
        orchestrationWorkflow.getWorkflowPhases().add(i, workflowPhase);
        orchestrationWorkflow.getWorkflowPhaseIdMap().put(workflowPhase.getUuid(), workflowPhase);
        found = true;
        break;
      }
    }

    if (!found) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "no matching workflowPhase");
    }

    orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) updateWorkflow(workflow, orchestrationWorkflow).getOrchestrationWorkflow();
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
      throw new WingsException(ErrorCode.INVALID_REQUEST, "args", "node");
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
    savedWorkflow.setOrchestrationWorkflow(originalWorkflow.getOrchestrationWorkflow().clone());
    return updateWorkflow(savedWorkflow, savedWorkflow.getOrchestrationWorkflow(), false);
  }

  @Override
  public Workflow templatizeWorkflow(String appId, String originalWorkflowId, Workflow workflow) {
    Workflow originalWorkflow = readWorkflow(appId, originalWorkflowId);
    Workflow templatizedWorkflow = originalWorkflow.clone();
    templatizedWorkflow.setName(workflow.getName());
    templatizedWorkflow.setDescription(workflow.getDescription());
    templatizedWorkflow.setTemplatized(true);
    Workflow savedWorkflow = createWorkflow(templatizedWorkflow);
    savedWorkflow.setOrchestrationWorkflow(originalWorkflow.getOrchestrationWorkflow().clone());
    return updateWorkflow(savedWorkflow, savedWorkflow.getOrchestrationWorkflow(), false);
  }

  @Override
  public Workflow updateWorkflow(String appId, String workflowId, Integer defaultVersion) {
    Workflow workflow = readWorkflow(appId, workflowId, null);
    wingsPersistence.update(
        workflow, wingsPersistence.createUpdateOperations(Workflow.class).set("defaultVersion", defaultVersion));
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

    if (workflowPhase.getDeploymentType() == DeploymentType.ECS
        || workflowPhase.getDeploymentType() == DeploymentType.KUBERNETES
        || workflowPhase.getDeploymentType() == DeploymentType.AWS_CODEDEPLOY) {
      requiredEntityTypes.add(EntityType.ARTIFACT);
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
          if (command.getCommand().isArtifactNeeded()) {
            requiredEntityTypes.add(EntityType.ARTIFACT);
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
      generateNewWorkflowPhaseStepsForECS(appId, envId, workflowPhase, !serviceRepeat);
    } else if (deploymentType == DeploymentType.KUBERNETES) {
      generateNewWorkflowPhaseStepsForKubernetes(appId, envId, workflowPhase, !serviceRepeat);
    } else if (deploymentType == DeploymentType.AWS_CODEDEPLOY) {
      generateNewWorkflowPhaseStepsForAWSCodeDeploy(appId, envId, workflowPhase);
    } else {
      generateNewWorkflowPhaseStepsForSSH(appId, envId, workflowPhase);
    }
  }

  private void generateNewWorkflowPhaseStepsForAWSCodeDeploy(String appId, String envId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.PREPARE_STEPS, Constants.PREPARE_STEPS).build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.DEPLOY_AWSCODEDEPLOY, Constants.DEPLOY_SERVICE)
                                   .addStep(aNode()
                                                .withId(getUuid())
                                                .withType(AWS_CODEDEPLOY_STATE.name())
                                                .withName(Constants.AWS_CODE_DEPLOY)
                                                //.addProperty()
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.WRAP_UP, Constants.WRAP_UP).build());
  }

  private void generateNewWorkflowPhaseStepsForECS(
      String appId, String envId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
      if (infraMapping instanceof EcsInfrastructureMapping
          && Constants.RUNTIME.equals(((EcsInfrastructureMapping) infraMapping).getClusterName())) {
        workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.CLUSTER_SETUP, Constants.SETUP_CLUSTER)
                                       .addStep(aNode()
                                                    .withId(getUuid())
                                                    .withType(StateType.AWS_CLUSTER_SETUP.name())
                                                    .withName("AWS Cluster Setup")
                                                    .build())
                                       .build());
      }
      workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                                     .addStep(aNode()
                                                  .withId(getUuid())
                                                  .withType(StateType.ECS_SERVICE_SETUP.name())
                                                  .withName(Constants.ECS_SERVICE_SETUP)
                                                  .build())
                                     .build());
    }
    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                                   .addStep(aNode()
                                                .withId(getUuid())
                                                .withType(ECS_SERVICE_DEPLOY.name())
                                                .withName(Constants.UPGRADE_CONTAINERS)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.WRAP_UP, Constants.WRAP_UP).build());
  }

  private void generateNewWorkflowPhaseStepsForKubernetes(
      String appId, String envId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    if (serviceSetupRequired) {
      InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
      if (infraMapping instanceof GcpKubernetesInfrastructureMapping
          && Constants.RUNTIME.equals(((GcpKubernetesInfrastructureMapping) infraMapping).getClusterName())) {
        workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.CLUSTER_SETUP, Constants.SETUP_CLUSTER)
                                       .addStep(aNode()
                                                    .withId(getUuid())
                                                    .withType(StateType.GCP_CLUSTER_SETUP.name())
                                                    .withName("GCP Cluster Setup")
                                                    .build())
                                       .build());
      }
      workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.CONTAINER_SETUP, Constants.SETUP_CONTAINER)
                                     .addStep(aNode()
                                                  .withId(getUuid())
                                                  .withType(StateType.KUBERNETES_REPLICATION_CONTROLLER_SETUP.name())
                                                  .withName("Kubernetes Service Setup")
                                                  .build())
                                     .build());
    }

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                                   .addStep(aNode()
                                                .withId(getUuid())
                                                .withType(KUBERNETES_REPLICATION_CONTROLLER_DEPLOY.name())
                                                .withName("Upgrade Containers")
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());
    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.WRAP_UP, Constants.WRAP_UP).build());
  }

  private void generateNewWorkflowPhaseStepsForSSH(String appId, String envId, WorkflowPhase workflowPhase) {
    // For DC only - for other types it has to be customized

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.get(appId, workflowPhase.getInfraMappingId());
    StateType stateType =
        infrastructureMapping.getComputeProviderType().equals(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
        ? DC_NODE_SELECT
        : AWS_NODE_SELECT;

    if (!Arrays.asList(DC_NODE_SELECT, AWS_NODE_SELECT).contains(stateType)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Unsupported state type: " + stateType);
    }

    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service);

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.PROVISION_NODE, Constants.PROVISION_NODE_NAME)
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
        aPhaseStep(PhaseStepType.DISABLE_SERVICE, Constants.DISABLE_SERVICE).addAllSteps(disableServiceSteps).build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.DEPLOY_SERVICE, Constants.DEPLOY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.INSTALL))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(
        aPhaseStep(PhaseStepType.ENABLE_SERVICE, Constants.ENABLE_SERVICE).addAllSteps(enableServiceSteps).build());

    // Not needed for non-DC
    // workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.DEPROVISION_NODE).build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.WRAP_UP, Constants.WRAP_UP).build());
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
    } else {
      return generateRollbackWorkflowPhaseForSSH(appId, workflowPhase);
    }
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
        .addPhaseStep(aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
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
        .addPhaseStep(aPhaseStep(PhaseStepType.WRAP_UP, Constants.WRAP_UP).build())
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
        .addPhaseStep(aPhaseStep(PhaseStepType.DEPLOY_AWSCODEDEPLOY, Constants.DEPLOY_SERVICE)
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
        .addPhaseStep(aPhaseStep(PhaseStepType.WRAP_UP, Constants.WRAP_UP).build())
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
            .addPhaseStep(aPhaseStep(PhaseStepType.DISABLE_SERVICE, Constants.DISABLE_SERVICE)
                              .addAllSteps(disableServiceSteps)
                              .withPhaseStepNameForRollback(Constants.ENABLE_SERVICE)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .withRollback(true)
                              .build())
            .addPhaseStep(aPhaseStep(PhaseStepType.STOP_SERVICE, Constants.STOP_SERVICE)
                              .addAllSteps(commandNodes(commandMap, CommandType.STOP, true))
                              .withRollback(true)
                              .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .build())
            .addPhaseStep(aPhaseStep(PhaseStepType.DEPLOY_SERVICE, Constants.DEPLOY_SERVICE)
                              .addAllSteps(commandNodes(commandMap, CommandType.INSTALL, true))
                              .withRollback(true)
                              .withPhaseStepNameForRollback(Constants.DEPLOY_SERVICE)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .build())
            .addPhaseStep(aPhaseStep(PhaseStepType.ENABLE_SERVICE, Constants.ENABLE_SERVICE)
                              .addAllSteps(enableServiceSteps)
                              .withRollback(true)
                              .withPhaseStepNameForRollback(Constants.DISABLE_SERVICE)
                              .withStatusForRollback(ExecutionStatus.SUCCESS)
                              .build())
            .addPhaseStep(aPhaseStep(PhaseStepType.WRAP_UP, Constants.WRAP_UP).build())
            .build();
    ;

    // get provision NODE
    Optional<PhaseStep> provisionPhaseStep = workflowPhase.getPhaseSteps()
                                                 .stream()
                                                 .filter(ps -> ps.getPhaseStepType() == PhaseStepType.PROVISION_NODE)
                                                 .findFirst();
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
              aPhaseStep(PhaseStepType.DE_PROVISION_NODE, Constants.DE_PROVISION_NODE).build()));
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
