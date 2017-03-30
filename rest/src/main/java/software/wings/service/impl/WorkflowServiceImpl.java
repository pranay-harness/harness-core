/**
 *
 */

package software.wings.service.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.StateMachineExecutionSimulator.populateRequiredEntityTypesByAccessType;
import static software.wings.sm.StateType.AWS_NODE_SELECT;
import static software.wings.sm.StateType.COMMAND;
import static software.wings.sm.StateType.DC_NODE_SELECT;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.KUBERNETES_REPLICATION_CONTROLLER_DEPLOY;
import static software.wings.sm.StateType.values;

import com.google.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.api.DeploymentType;
import software.wings.app.StaticConfiguration;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.ErrorCode;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Node;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowFailureStrategy;
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
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilCategory;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
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
  @Inject private EnvironmentService environmentService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private EntityVersionService entityVersionService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private SettingsService settingsService;

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
    maps.values().forEach(list -> { Collections.sort(list, stencilDefaultSorter); });

    boolean filterForWorkflow = isNotBlank(workflowId);
    boolean filterForPhase = filterForWorkflow && isNotBlank(phaseId);

    Predicate<Stencil> predicate = stencil -> true;
    if (filterForWorkflow) {
      Workflow workflow = readWorkflow(appId, workflowId);
      if (filterForPhase) {
        WorkflowPhase workflowPhase =
            ((CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getWorkflowPhaseIdMap().get(phaseId);
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
      for (StateTypeScope scope : sd.getScopes()) {
        List<StateTypeDescriptor> listByScope = mapByScope.get(scope);
        if (listByScope == null) {
          listByScope = new ArrayList<>();
          mapByScope.put(scope, listByScope);
        }
        listByScope.add(sd);
      }
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
            workflowExecutionService.listExecutions(workflowExecutionPageRequest, false, false, false).getResponse());
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

    List<Service> services = new ArrayList<>();
    workflow.getOrchestrationWorkflow().getServiceIds().forEach(
        serviceId -> { services.add(serviceResourceService.get(workflow.getAppId(), serviceId, false)); });
    workflow.setServices(services);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Workflow createWorkflow(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    workflow.setDefaultVersion(1);
    String key = wingsPersistence.save(workflow);
    if (orchestrationWorkflow != null) {
      if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
        if (canaryOrchestrationWorkflow.getWorkflowPhases() != null
            && !canaryOrchestrationWorkflow.getWorkflowPhases().isEmpty()) {
          List<WorkflowPhase> workflowPhases = canaryOrchestrationWorkflow.getWorkflowPhases();
          canaryOrchestrationWorkflow.setWorkflowPhases(new ArrayList<>());
          workflowPhases.forEach(workflowPhase -> attachWorkflowPhase(workflow, workflowPhase));
        }
      }
      orchestrationWorkflow.onSave();
      orchestrationWorkflow.setRequiredEntityTypes(getRequiredEntityTypes(workflow.getAppId(), orchestrationWorkflow));
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

    if (orchestrationWorkflow != null) {
      if (onSaveCallNeeded) {
        orchestrationWorkflow.onSave();
      }

      EntityVersion entityVersion =
          entityVersionService.lastEntityVersion(workflow.getAppId(), EntityType.WORKFLOW, workflow.getUuid());
      workflow.setDefaultVersion(entityVersion.getVersion());

      if (orchestrationWorkflow != null) {
        StateMachine stateMachine = new StateMachine(workflow, workflow.getDefaultVersion(),
            ((CustomOrchestrationWorkflow) orchestrationWorkflow).getGraph(), stencilMap());
        stateMachine = wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
      }

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

  @Override
  public boolean deleteWorkflow(String appId, String workflowId) {
    return wingsPersistence.delete(Workflow.class, appId, workflowId);
    // TODO: cleanup state machines and check the workflow in running state
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

  private StateMachine readStateMachine(String appId, String originId, Integer version) {
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
      deleteWorkflow(appId, (String) key.getId());
    }
  }

  @Override
  public void deleteStateMachinesByApplication(String appId) {
    wingsPersistence.delete(wingsPersistence.createQuery(StateMachine.class).field("appId").equal(appId));
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

  public List<WorkflowFailureStrategy> listWorkflowFailureStrategies(String appId) {
    return listWorkflowFailureStrategies(aPageRequest().addFilter("appId", Operator.EQ, appId).build()).getResponse();
  }

  @Override
  public PageResponse<WorkflowFailureStrategy> listWorkflowFailureStrategies(
      PageRequest<WorkflowFailureStrategy> pageRequest) {
    return wingsPersistence.query(WorkflowFailureStrategy.class, pageRequest);
  }

  @Override
  public WorkflowFailureStrategy createWorkflowFailureStrategy(@Valid WorkflowFailureStrategy workflowFailureStrategy) {
    return wingsPersistence.saveAndGet(WorkflowFailureStrategy.class, workflowFailureStrategy);
  }

  @Override
  public WorkflowFailureStrategy updateWorkflowFailureStrategy(@Valid WorkflowFailureStrategy workflowFailureStrategy) {
    // TODO:
    return workflowFailureStrategy;
  }

  @Override
  public boolean deleteWorkflowFailureStrategy(String appId, String workflowFailureStrategyId) {
    return wingsPersistence.delete(WorkflowFailureStrategy.class, appId, workflowFailureStrategyId);
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

    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();

    if (orchestrationWorkflow.getWorkflowPhases() == null) {
      workflowPhase.setName(Constants.PHASE_NAME_PREFIX + 1);
    } else {
      workflowPhase.setName(Constants.PHASE_NAME_PREFIX + (orchestrationWorkflow.getWorkflowPhases().size() + 1));
    }

    boolean serviceRepeat = false;
    if (orchestrationWorkflow.getWorkflowPhaseIds() != null) {
      for (String phaseId : orchestrationWorkflow.getWorkflowPhaseIds()) {
        WorkflowPhase existingPhase = orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId);
        if (existingPhase.getServiceId().equals(workflowPhase.getServiceId())
            && existingPhase.getDeploymentType() == workflowPhase.getDeploymentType()) {
          serviceRepeat = true;
          break;
        }
      }
    }
    generateNewWorkflowPhaseSteps(workflow.getAppId(), workflow.getEnvId(), workflowPhase, serviceRepeat);
    orchestrationWorkflow.getWorkflowPhases().add(workflowPhase);

    WorkflowPhase rollbackWorkflowPhase = generateRollbackWorkflowPhase(workflow.getAppId(), workflowPhase);
    orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
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

  private Set<EntityType> getRequiredEntityTypes(String appId, OrchestrationWorkflow orchestrationWorkflow) {
    Validator.notNullCheck("orchestrationWorkflow", orchestrationWorkflow);
    if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      return ((CanaryOrchestrationWorkflow) orchestrationWorkflow)
          .getWorkflowPhases()
          .stream()
          .flatMap(phase -> getRequiredEntityTypes(appId, phase).stream())
          .collect(Collectors.toSet());
    }

    return null;
  }

  private Set<EntityType> getRequiredEntityTypes(String appId, WorkflowPhase workflowPhase) {
    Set<EntityType> requiredEntityTypes = new HashSet<>();

    if (workflowPhase == null || workflowPhase.getPhaseSteps() == null) {
      return requiredEntityTypes;
    }

    if (workflowPhase.getDeploymentType() == DeploymentType.ECS
        || workflowPhase.getDeploymentType() == DeploymentType.KUBERNETES) {
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
            return requiredEntityTypes;
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
    } else {
      generateNewWorkflowPhaseStepsForSSH(appId, envId, workflowPhase);
    }
  }

  private void generateNewWorkflowPhaseStepsForECS(
      String appId, String envId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service, DeploymentType.ECS);

    if (serviceSetupRequired) {
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
                                                .withName(Constants.ECS_SERVICE_DEPLOY)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());
  }

  private void generateNewWorkflowPhaseStepsForKubernetes(
      String appId, String envId, WorkflowPhase workflowPhase, boolean serviceSetupRequired) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service, DeploymentType.KUBERNETES);

    if (serviceSetupRequired) {
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
  }

  private void generateNewWorkflowPhaseStepsForSSH(String appId, String envId, WorkflowPhase workflowPhase) {
    // For DC only - for other types it has to be customized

    StateType stateType = determineStateType(appId, workflowPhase.getInfraMappingId());

    if (!Arrays.asList(DC_NODE_SELECT, AWS_NODE_SELECT).contains(stateType)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Unsupported state type: " + stateType);
    }

    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service, DeploymentType.SSH);

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.PROVISION_NODE, Constants.PROVISION_NODE_NAME)
                                   .addStep(aNode()
                                                .withType(stateType.name())
                                                .withName("Select Nodes")
                                                .withOrigin(true)
                                                .addProperty("specificHosts", false)
                                                .addProperty("instanceCount", 1)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.DISABLE_SERVICE, Constants.DISABLE_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.DISABLE))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.DEPLOY_SERVICE, Constants.DEPLOY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.INSTALL))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.VERIFY_SERVICE, Constants.VERIFY_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.ENABLE_SERVICE, Constants.ENABLE_SERVICE)
                                   .addAllSteps(commandNodes(commandMap, CommandType.ENABLE))
                                   .build());

    // Not needed for non-DC
    // workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.DEPROVISION_NODE).build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.WRAP_UP, Constants.WRAP_UP).build());
  }

  private WorkflowPhase generateRollbackWorkflowPhase(String appId, WorkflowPhase workflowPhase) {
    DeploymentType deploymentType = workflowPhase.getDeploymentType();
    if (deploymentType == DeploymentType.ECS) {
      return generateRollbackWorkflowPhaseForECS(appId, workflowPhase);
    } else if (deploymentType == DeploymentType.KUBERNETES) {
      return generateRollbackWorkflowPhaseForKubernetes(appId, workflowPhase);
    } else {
      return generateRollbackWorkflowPhaseForSSH(appId, workflowPhase);
    }
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForECS(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service, DeploymentType.ECS);

    WorkflowPhase rollbackWorkflowPhase =
        aWorkflowPhase()
            .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
            .withRollback(true)
            .withServiceId(workflowPhase.getServiceId())
            .withComputeProviderId(workflowPhase.getComputeProviderId())
            .withInfraMappingName(workflowPhase.getInfraMappingName())
            .withRollbackPhaseName(workflowPhase.getName())
            .withDeploymentType(workflowPhase.getDeploymentType())
            .withInfraMappingId(workflowPhase.getInfraMappingId())
            .addPhaseStep(aPhaseStep(PhaseStepType.STOP_SERVICE, Constants.STOP_SERVICE)
                              .addAllSteps(commandNodes(commandMap, CommandType.RESIZE, true))
                              .withRollback(true)
                              .build())
            .build();

    return rollbackWorkflowPhase;
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForKubernetes(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service, DeploymentType.KUBERNETES);

    WorkflowPhase rollbackWorkflowPhase =
        aWorkflowPhase()
            .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
            .withRollback(true)
            .withServiceId(workflowPhase.getServiceId())
            .withComputeProviderId(workflowPhase.getComputeProviderId())
            .withInfraMappingName(workflowPhase.getInfraMappingName())
            .withRollbackPhaseName(workflowPhase.getName())
            .withDeploymentType(workflowPhase.getDeploymentType())
            .withInfraMappingId(workflowPhase.getInfraMappingId())
            .addPhaseStep(aPhaseStep(PhaseStepType.STOP_SERVICE, Constants.STOP_SERVICE)
                              .addAllSteps(commandNodes(commandMap, CommandType.RESIZE, true))
                              .withRollback(true)
                              .build())
            .build();

    return rollbackWorkflowPhase;
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForSSH(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service, DeploymentType.SSH);

    WorkflowPhase rollbackWorkflowPhase =
        aWorkflowPhase()
            .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
            .withRollback(true)
            .withServiceId(workflowPhase.getServiceId())
            .withComputeProviderId(workflowPhase.getComputeProviderId())
            .withInfraMappingName(workflowPhase.getInfraMappingName())
            .withRollbackPhaseName(workflowPhase.getName())
            .withDeploymentType(workflowPhase.getDeploymentType())
            .withInfraMappingId(workflowPhase.getInfraMappingId())
            .addPhaseStep(aPhaseStep(PhaseStepType.DISABLE_SERVICE, Constants.DISABLE_SERVICE)
                              .addAllSteps(commandNodes(commandMap, CommandType.DISABLE, true))
                              .withRollback(true)
                              .build())
            .addPhaseStep(aPhaseStep(PhaseStepType.STOP_SERVICE, Constants.STOP_SERVICE)
                              .addAllSteps(commandNodes(commandMap, CommandType.STOP, true))
                              .withRollback(true)
                              .build())
            .addPhaseStep(aPhaseStep(PhaseStepType.ENABLE_SERVICE, Constants.ENABLE_SERVICE)
                              .addAllSteps(commandNodes(commandMap, CommandType.ENABLE, true))
                              .withRollback(true)
                              .build())
            .build();

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

      if (awsProvisionNode.isPresent()) {
        rollbackWorkflowPhase.getPhaseSteps().add(
            aPhaseStep(PhaseStepType.DE_PROVISION_NODE, Constants.DE_PROVISION_NODE).build());
      }
    }

    return rollbackWorkflowPhase;
  }

  private Map<CommandType, List<Command>> getCommandTypeListMap(Service service, DeploymentType deploymentType) {
    Map<CommandType, List<Command>> commandMap = new HashMap<>();
    if (service.getServiceCommands() == null) {
      return commandMap;
    }
    for (ServiceCommand sc : service.getServiceCommands()) {
      if (sc.getCommand() == null || sc.getCommand().getCommandType() == null) {
        continue;
      }
      List<Command> list = commandMap.get(sc.getCommand().getCommandType());
      if (list == null) {
        list = new ArrayList<>();
        commandMap.put(sc.getCommand().getCommandType(), list);
      }
      list.add(sc.getCommand());
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

  private StateType determineStateType(String appId, String infraMappingId) {
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
    StateType stateType =
        infrastructureMapping.getComputeProviderType().equals(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
        ? DC_NODE_SELECT
        : AWS_NODE_SELECT;
    return stateType;
  }
}
