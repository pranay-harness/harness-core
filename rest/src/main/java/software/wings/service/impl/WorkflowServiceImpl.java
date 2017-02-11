/**
 *
 */

package software.wings.service.impl;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.EntityVersion.Builder.anEntityVersion;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.StateType.AWS_NODE_SELECT;
import static software.wings.sm.StateType.COMMAND;
import static software.wings.sm.StateType.DC_NODE_SELECT;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.FORK;
import static software.wings.sm.StateType.REPEAT;
import static software.wings.sm.StateType.values;

import com.google.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.api.DeploymentType;
import software.wings.app.StaticConfiguration;
import software.wings.beans.Base;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.ErrorCodes;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Builder;
import software.wings.beans.Graph.Node;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.NotificationRule;
import software.wings.beans.Orchestration;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.ReadPref;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
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
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.sm.TransitionType;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilPostProcessor;

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

  private Map<StateTypeScope, List<StateTypeDescriptor>> cachedStencils;
  private Map<String, StateTypeDescriptor> cachedStencilMap;

  /**
   * {@inheritDoc}
   */
  @Override
  public StateMachine create(StateMachine stateMachine) {
    stateMachine.validate();
    return wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<StateMachine> list(PageRequest<StateMachine> req) {
    return wingsPersistence.query(StateMachine.class, req);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<StateTypeScope, List<Stencil>> stencils(String appId, StateTypeScope... stateTypeScopes) {
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
      stencils(null);
    }
    return cachedStencilMap;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Workflow> T createWorkflow(Class<T> cls, T workflow) {
    Graph graph = workflow.getGraph();
    workflow.setDefaultVersion(1);
    workflow = wingsPersistence.saveAndGet(cls, workflow);
    if (graph != null) {
      StateMachine stateMachine = new StateMachine(workflow, workflow.getDefaultVersion(), graph, stencilMap());
      stateMachine = wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
      workflow.setGraph(stateMachine.getGraph());
      workflow.getGraph().setVersion(stateMachine.getOriginVersion());
    }

    // create initial version
    entityVersionService.newEntityVersion(workflow.getAppId(), EntityType.WORKFLOW, workflow.getUuid(),
        workflow.getName(), ChangeType.CREATED, workflow.getNotes());

    return workflow;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public <T extends Workflow> T updateWorkflow(T workflow, Integer version) {
    Graph graph = workflow.getGraph();
    if (graph != null) {
      StateMachine stateMachine = new StateMachine(workflow, version, graph, stencilMap());
      stateMachine = wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
      workflow.setGraph(stateMachine.getGraph());
      workflow.getGraph().setVersion(stateMachine.getOriginVersion());
    }
    return workflow;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StateMachine readLatest(String appId, String originId) {
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

  private StateMachine read(String appId, String originId, Integer version) {
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
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Orchestration> listOrchestration(PageRequest<Orchestration> pageRequest, String envId) {
    boolean workflowTypeFilter = false;
    if (pageRequest != null && pageRequest.getFilters() != null) {
      for (SearchFilter filter : pageRequest.getFilters()) {
        if (filter != null && filter.getFieldName() != null && filter.getFieldName().equals("workflowType")) {
          workflowTypeFilter = true;
        }
      }
    }
    if (!workflowTypeFilter) {
      pageRequest.addFilter(aSearchFilter().withField("workflowType", Operator.EQ, WorkflowType.ORCHESTRATION).build());
    }
    if (isNotBlank(envId)) {
      SearchFilter[] searchFilters = new SearchFilter[2];
      searchFilters[0] = aSearchFilter().withField("targetToAllEnv", Operator.EQ, true).build();
      searchFilters[1] = aSearchFilter().withField("envIdVersionMap." + envId, Operator.EXISTS, null).build();
      pageRequest.addFilter(aSearchFilter().withField(null, Operator.OR, searchFilters).build());
    }
    PageResponse<Orchestration> res = wingsPersistence.query(Orchestration.class, pageRequest);
    if (res != null && res.size() > 0) {
      for (Orchestration orchestration : res.getResponse()) {
        StateMachine stateMachine = null;
        Integer version = Optional.ofNullable(orchestration.getEnvIdVersionMap().get(nullToEmpty(envId)))
                              .orElse(anEntityVersion().withVersion(orchestration.getDefaultVersion()).build())
                              .getVersion();
        stateMachine = read(orchestration.getAppId(), orchestration.getUuid(), version);

        if (stateMachine != null) {
          orchestration.setGraph(stateMachine.getGraph());
          orchestration.getGraph().setVersion(stateMachine.getOriginVersion());
        }
      }
    }
    return res;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Orchestration updateOrchestration(Orchestration orchestration) {
    UpdateOperations<Orchestration> ops = wingsPersistence.createUpdateOperations(Orchestration.class);
    setUnset(ops, "description", orchestration.getDescription());
    setUnset(ops, "name", orchestration.getName());

    EntityVersion entityVersion;
    if (orchestration.getGraph() != null) {
      entityVersion = entityVersionService.newEntityVersion(orchestration.getAppId(), EntityType.WORKFLOW,
          orchestration.getUuid(), orchestration.getName(), ChangeType.UPDATED, orchestration.getNotes());
    } else {
      entityVersion = entityVersionService.lastEntityVersion(
          orchestration.getAppId(), EntityType.WORKFLOW, orchestration.getUuid());
    }
    if (orchestration.getSetAsDefault()) {
      orchestration.setDefaultVersion(entityVersion.getVersion());
    }
    setUnset(ops, "envIdVersionMap", orchestration.getEnvIdVersionMap());
    setUnset(ops, "defaultVersion", orchestration.getDefaultVersion());

    wingsPersistence.update(wingsPersistence.createQuery(Orchestration.class)
                                .field("appId")
                                .equal(orchestration.getAppId())
                                .field(ID_KEY)
                                .equal(orchestration.getUuid()),
        ops);

    orchestration = updateWorkflow(orchestration, entityVersion.getVersion());
    return orchestration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Orchestration readOrchestration(String appId, String orchestrationId, Integer version) {
    Orchestration orchestration = wingsPersistence.get(Orchestration.class, appId, orchestrationId);

    if (orchestration == null) {
      return orchestration;
    }
    StateMachine stateMachine =
        read(appId, orchestrationId, version == null ? orchestration.getDefaultVersion() : version);
    if (stateMachine != null) {
      orchestration.setGraph(stateMachine.getGraph());
      orchestration.getGraph().setVersion(stateMachine.getOriginVersion());
    }
    return orchestration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Base> boolean deleteWorkflow(Class<T> cls, String appId, String workflowId) {
    boolean deleted = wingsPersistence.delete(
        wingsPersistence.createQuery(cls).field("appId").equal(appId).field(ID_KEY).equal(workflowId));
    if (deleted) {
      workflowExecutionService.deleteByWorkflow(appId, workflowId);
    }
    return deleted;
  }

  /**
   * Read latest simple workflow orchestration.
   *
   * @param appId the app id
   * @return the orchestration
   */
  @Override
  public Orchestration readLatestSimpleWorkflow(String appId) {
    PageRequest<Orchestration> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    filter.setFieldValues(appId);
    filter.setOp(Operator.EQ);
    req.addFilter(filter);

    filter = new SearchFilter();
    filter.setFieldName("name");
    filter.setFieldValues(Constants.SIMPLE_ORCHESTRATION_NAME);
    filter.setOp(Operator.EQ);
    req.addFilter(filter);

    filter = new SearchFilter();
    filter.setFieldName("workflowType");
    filter.setFieldValues(WorkflowType.SIMPLE);
    filter.setOp(Operator.EQ);
    req.addFilter(filter);

    Orchestration workflow = wingsPersistence.get(Orchestration.class, req, ReadPref.CRITICAL);
    if (workflow == null) {
      workflow = createDefaultSimpleWorkflow(appId);
    }

    return workflow;
  }

  @Override
  public void deleteWorkflowByApplication(String appId) {
    List<Key<Pipeline>> pipelineKeys =
        wingsPersistence.createQuery(Pipeline.class).field("appId").equal(appId).asKeyList();
    for (Key key : pipelineKeys) {
      deleteWorkflow(Pipeline.class, appId, (String) key.getId());
    }

    List<Key<Orchestration>> orchestrationKeys =
        wingsPersistence.createQuery(Orchestration.class).field("appId").equal(appId).asKeyList();
    for (Key key : orchestrationKeys) {
      deleteWorkflow(Orchestration.class, appId, (String) key.getId());
    }
  }

  @Override
  public void deleteStateMachinesByApplication(String appId) {
    wingsPersistence.delete(wingsPersistence.createQuery(StateMachine.class).field("appId").equal(appId));
  }

  @Override
  public StateMachine readForEnv(String appId, String envId, String orchestrationId) {
    Orchestration orchestration = wingsPersistence.get(Orchestration.class, appId, orchestrationId);
    if (orchestration != null
        && (orchestration.getEnvIdVersionMap().containsKey(envId) || orchestration.getTargetToAllEnv())) {
      Integer version = Optional.ofNullable(orchestration.getEnvIdVersionMap().get(envId))
                            .orElse(anEntityVersion().withVersion(orchestration.getDefaultVersion()).build())
                            .getVersion();
      return read(appId, orchestrationId, version);
    }
    return null;
  }

  private Orchestration createDefaultSimpleWorkflow(String appId) {
    Orchestration orchestration = new Orchestration();
    orchestration.setName(Constants.SIMPLE_ORCHESTRATION_NAME);
    orchestration.setDescription(Constants.SIMPLE_ORCHESTRATION_DESC);
    orchestration.setWorkflowType(WorkflowType.SIMPLE);
    orchestration.setAppId(appId);
    Graph graph = staticConfiguration.defaultSimpleWorkflow();
    orchestration.setGraph(graph);

    return createWorkflow(Orchestration.class, orchestration);
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
    PageRequest<Orchestration> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", appId, EQ);
    pageRequest.addFilter("workflowType", WorkflowType.ORCHESTRATION, EQ);
    return listOrchestration(pageRequest, null).stream().collect(toMap(Orchestration::getUuid, o -> (o.getName())));
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
  public WorkflowFailureStrategy create(@Valid WorkflowFailureStrategy workflowFailureStrategy) {
    return wingsPersistence.saveAndGet(WorkflowFailureStrategy.class, workflowFailureStrategy);
  }

  @Override
  public WorkflowFailureStrategy update(@Valid WorkflowFailureStrategy workflowFailureStrategy) {
    // TODO:
    return workflowFailureStrategy;
  }

  @Override
  public boolean deleteWorkflowFailureStrategy(String appId, String workflowFailureStrategyId) {
    return wingsPersistence.delete(WorkflowFailureStrategy.class, appId, workflowFailureStrategyId);
  }

  @Override
  public PageResponse<OrchestrationWorkflow> listOrchestrationWorkflows(
      PageRequest<OrchestrationWorkflow> pageRequest) {
    return listOrchestrationWorkflows(pageRequest, 0);
  }

  @Override
  public PageResponse<OrchestrationWorkflow> listOrchestrationWorkflows(
      PageRequest<OrchestrationWorkflow> pageRequest, Integer previousExecutionsCount) {
    PageResponse<OrchestrationWorkflow> pageResponse = wingsPersistence.query(OrchestrationWorkflow.class, pageRequest);

    if (pageResponse == null || pageResponse.size() == 0) {
      return pageResponse;
    }

    for (OrchestrationWorkflow orchestrationWorkflow : pageResponse) {
      populateOrchestrationWorkflow(orchestrationWorkflow);

      if (previousExecutionsCount != null && previousExecutionsCount > 0) {
        PageRequest<WorkflowExecution> workflowExecutionPageRequest =
            aPageRequest()
                .withLimit(previousExecutionsCount.toString())
                .addFilter("workflowId", EQ, orchestrationWorkflow.getUuid())
                .build();

        orchestrationWorkflow.setWorkflowExecutions(
            workflowExecutionService.listExecutions(workflowExecutionPageRequest, false, false, false).getResponse());
      }
    }

    return pageResponse;
  }

  @Override
  public OrchestrationWorkflow readOrchestrationWorkflow(String appId, String orchestrationWorkflowId) {
    OrchestrationWorkflow orchestrationWorkflow =
        wingsPersistence.get(OrchestrationWorkflow.class, appId, orchestrationWorkflowId);
    populateOrchestrationWorkflow(orchestrationWorkflow);
    return orchestrationWorkflow;
  }

  private void populateOrchestrationWorkflow(OrchestrationWorkflow orchestrationWorkflow) {
    if (orchestrationWorkflow == null) {
      return;
    }
    populateServices(orchestrationWorkflow);
    populatePhaseSteps(orchestrationWorkflow);
  }
  private void populateServices(OrchestrationWorkflow orchestrationWorkflow) {
    if (orchestrationWorkflow.getWorkflowPhaseIdMap() == null) {
      return;
    }
    List<Service> services = new ArrayList<>();
    Set<String> serviceIds = orchestrationWorkflow.getWorkflowPhaseIdMap()
                                 .values()
                                 .stream()
                                 .map(WorkflowPhase::getServiceId)
                                 .collect(Collectors.toSet());
    serviceIds.forEach(
        serviceId -> { services.add(serviceResourceService.get(orchestrationWorkflow.getAppId(), serviceId, false)); });
    orchestrationWorkflow.setServices(services);
  }

  private void populatePhaseSteps(OrchestrationWorkflow orchestrationWorkflow) {
    if (orchestrationWorkflow == null) {
      return;
    }
    populatePhaseSteps(orchestrationWorkflow.getPreDeploymentSteps(), orchestrationWorkflow.getGraph());
    if (orchestrationWorkflow.getWorkflowPhaseIdMap() != null) {
      orchestrationWorkflow.getWorkflowPhaseIdMap().values().forEach(workflowPhase -> {
        workflowPhase.getPhaseSteps().forEach(
            phaseStep -> { populatePhaseSteps(phaseStep, orchestrationWorkflow.getGraph()); });
      });
    }
    if (orchestrationWorkflow.getRollbackWorkflowPhaseIdMap() != null) {
      orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().values().forEach(workflowPhase -> {
        workflowPhase.getPhaseSteps().forEach(
            phaseStep -> { populatePhaseSteps(phaseStep, orchestrationWorkflow.getGraph()); });
      });
    }
    populatePhaseSteps(orchestrationWorkflow.getPostDeploymentSteps(), orchestrationWorkflow.getGraph());
  }

  @Override
  public OrchestrationWorkflow createOrchestrationWorkflow(OrchestrationWorkflow orchestrationWorkflow) {
    orchestrationWorkflow.setGraph(generateMainGraph(orchestrationWorkflow));

    int i = 0;
    for (WorkflowPhase workflowPhase : orchestrationWorkflow.getWorkflowPhases()) {
      workflowPhase.setName(Constants.PHASE_NAME_PREFIX + ++i);
      generateNewWorkflowPhaseSteps(
          orchestrationWorkflow.getAppId(), orchestrationWorkflow.getEnvironmentId(), workflowPhase, i);
      populatePhaseStepIds(workflowPhase);
      orchestrationWorkflow.getGraph().getSubworkflows().putAll(generateGraph(workflowPhase));

      WorkflowPhase rollbackWorkflowPhase =
          generateRollbackWorkflowPhase(orchestrationWorkflow.getAppId(), workflowPhase);
      orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);
      orchestrationWorkflow.getGraph().getSubworkflows().putAll(generateGraph(rollbackWorkflowPhase));
    }
    orchestrationWorkflow = wingsPersistence.saveAndGet(OrchestrationWorkflow.class, orchestrationWorkflow);

    updateStateMachine(orchestrationWorkflow.getAppId(), orchestrationWorkflow.getUuid());
    return orchestrationWorkflow;
  }

  @Override
  public boolean deleteOrchestrationWorkflow(String appId, String orchestrationWorkflowId) {
    return wingsPersistence.delete(OrchestrationWorkflow.class, appId, orchestrationWorkflowId);
  }

  @Override
  public OrchestrationWorkflow updateOrchestrationWorkflowBasic(
      String appId, String orchestrationWorkflowId, OrchestrationWorkflow orchestrationWorkflow) {
    updateOrchestrationWorkflowField(appId, orchestrationWorkflowId, "name", orchestrationWorkflow.getName());
    return orchestrationWorkflow;
  }

  @Override
  public PhaseStep updatePreDeployment(String appId, String orchestrationWorkflowId, PhaseStep phaseStep) {
    OrchestrationWorkflow orchestrationWorkflow = readOrchestrationWorkflow(appId, orchestrationWorkflowId);

    Graph workflowOuterStepsGraph = generateGraph(phaseStep);
    Map<String, Object> map = new HashMap<>();
    map.put("preDeploymentSteps", phaseStep);
    map.put("graph.subworkflows." + phaseStep.getUuid(), workflowOuterStepsGraph);

    updateOrchestrationWorkflowField(appId, orchestrationWorkflowId, map);

    updateStateMachine(appId, orchestrationWorkflowId);
    return phaseStep;
  }

  @Override
  public PhaseStep updatePostDeployment(String appId, String orchestrationWorkflowId, PhaseStep phaseStep) {
    OrchestrationWorkflow orchestrationWorkflow = readOrchestrationWorkflow(appId, orchestrationWorkflowId);

    Graph workflowOuterStepsGraph = generateGraph(phaseStep);
    Map<String, Object> map = new HashMap<>();
    map.put("postDeploymentSteps", phaseStep);
    map.put("graph.subworkflows." + phaseStep.getUuid(), workflowOuterStepsGraph);

    updateOrchestrationWorkflowField(appId, orchestrationWorkflowId, map);

    updateStateMachine(appId, orchestrationWorkflowId);

    return phaseStep;
  }

  @Override
  public WorkflowPhase createWorkflowPhase(String appId, String orchestrationWorkflowId, WorkflowPhase workflowPhase) {
    OrchestrationWorkflow orchestrationWorkflow = readOrchestrationWorkflow(appId, orchestrationWorkflowId);

    if (orchestrationWorkflow == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "orchestrationWorkflowId");
    }

    if (orchestrationWorkflow.getWorkflowPhases() == null) {
      workflowPhase.setName(Constants.PHASE_NAME_PREFIX + 1);
    } else {
      workflowPhase.setName(Constants.PHASE_NAME_PREFIX + (orchestrationWorkflow.getWorkflowPhases().size() + 1));
    }
    Query<OrchestrationWorkflow> query = wingsPersistence.createQuery(OrchestrationWorkflow.class)
                                             .field("appId")
                                             .equal(appId)
                                             .field(ID_KEY)
                                             .equal(orchestrationWorkflowId);

    int phaseIndex = 0;
    if (orchestrationWorkflow.getWorkflowPhases() != null) {
      phaseIndex = orchestrationWorkflow.getWorkflowPhases().size();
    }
    generateNewWorkflowPhaseSteps(
        orchestrationWorkflow.getAppId(), orchestrationWorkflow.getEnvironmentId(), workflowPhase, phaseIndex);
    populatePhaseStepIds(workflowPhase);
    orchestrationWorkflow.getWorkflowPhaseIds().add(workflowPhase.getUuid());
    orchestrationWorkflow.getWorkflowPhaseIdMap().put(workflowPhase.getUuid(), workflowPhase);

    WorkflowPhase rollbackWorkflowPhase =
        generateRollbackWorkflowPhase(orchestrationWorkflow.getAppId(), workflowPhase);
    orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowPhase.getUuid(), rollbackWorkflowPhase);

    Map<String, Graph> rollbackSubworkflows = generateGraph(rollbackWorkflowPhase);
    orchestrationWorkflow.getGraph().getSubworkflows().putAll(rollbackSubworkflows);

    Graph graph = generateMainGraph(orchestrationWorkflow);

    UpdateOperations<OrchestrationWorkflow> updateOps =
        wingsPersistence.createUpdateOperations(OrchestrationWorkflow.class)
            .set("workflowPhaseIds", orchestrationWorkflow.getWorkflowPhaseIds());

    Map<String, Graph> phaseSubworkflows = generateGraph(workflowPhase);
    for (Map.Entry<String, Graph> entry : phaseSubworkflows.entrySet()) {
      updateOps.set("graph.subworkflows." + entry.getKey(), entry.getValue());
    }
    updateOps.set("workflowPhaseIdMap." + workflowPhase.getUuid(), workflowPhase)
        .set("graph.nodes", graph.getNodes())
        .set("graph.links", graph.getLinks());

    updateOps.set("rollbackWorkflowPhaseIdMap." + workflowPhase.getUuid(), rollbackWorkflowPhase);
    for (Map.Entry<String, Graph> entry : rollbackSubworkflows.entrySet()) {
      updateOps.set("graph.subworkflows." + entry.getKey(), entry.getValue());
    }

    wingsPersistence.update(query, updateOps);

    updateStateMachine(appId, orchestrationWorkflowId);
    return workflowPhase;
  }

  @Override
  public WorkflowPhase updateWorkflowPhase(String appId, String orchestrationWorkflowId, WorkflowPhase workflowPhase) {
    populatePhaseStepIds(workflowPhase);
    OrchestrationWorkflow orchestrationWorkflow = readOrchestrationWorkflow(appId, orchestrationWorkflowId);

    Query<OrchestrationWorkflow> query = wingsPersistence.createQuery(OrchestrationWorkflow.class)
                                             .field("appId")
                                             .equal(appId)
                                             .field(ID_KEY)
                                             .equal(orchestrationWorkflowId);
    Map<String, Graph> phaseSubworkflows = generateGraph(workflowPhase);

    UpdateOperations<OrchestrationWorkflow> updateOps =
        wingsPersistence.createUpdateOperations(OrchestrationWorkflow.class)
            .set("workflowPhaseIdMap." + workflowPhase.getUuid(), workflowPhase);
    for (Map.Entry<String, Graph> entry : phaseSubworkflows.entrySet()) {
      updateOps.set("graph.subworkflows." + entry.getKey(), entry.getValue());
    }
    wingsPersistence.update(query, updateOps);

    updateStateMachine(appId, orchestrationWorkflowId);
    return workflowPhase;
  }

  @Override
  public WorkflowPhase updateWorkflowPhaseRollback(
      String appId, String orchestrationWorkflowId, String phaseId, WorkflowPhase rollbackWorkflowPhase) {
    populatePhaseStepIds(rollbackWorkflowPhase);
    Query<OrchestrationWorkflow> query = wingsPersistence.createQuery(OrchestrationWorkflow.class)
                                             .field("appId")
                                             .equal(appId)
                                             .field(ID_KEY)
                                             .equal(orchestrationWorkflowId);

    UpdateOperations<OrchestrationWorkflow> updateOps =
        wingsPersistence.createUpdateOperations(OrchestrationWorkflow.class)
            .set("rollbackWorkflowPhaseIdMap." + phaseId, rollbackWorkflowPhase);

    Map<String, Graph> rollbackSubworkflows = generateGraph(rollbackWorkflowPhase);
    for (Map.Entry<String, Graph> entry : rollbackSubworkflows.entrySet()) {
      updateOps.set("graph.subworkflows." + entry.getKey(), entry.getValue());
    }

    wingsPersistence.update(query, updateOps);

    updateStateMachine(appId, orchestrationWorkflowId);
    return rollbackWorkflowPhase;
  }

  @Override
  public void deleteWorkflowPhase(String appId, String orchestrationWorkflowId, String phaseId) {
    OrchestrationWorkflow orchestrationWorkflow = readOrchestrationWorkflow(appId, orchestrationWorkflowId);

    if (orchestrationWorkflow == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "orchestrationWorkflowId");
    }

    List<String> phaseIds = orchestrationWorkflow.getWorkflowPhaseIds();
    if (orchestrationWorkflow == null || phaseIds == null || !phaseIds.contains(phaseId)) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "phaseId");
    }

    phaseIds.remove(phaseId);
    Query<OrchestrationWorkflow> query = wingsPersistence.createQuery(OrchestrationWorkflow.class)
                                             .field("appId")
                                             .equal(appId)
                                             .field(ID_KEY)
                                             .equal(orchestrationWorkflowId);
    UpdateOperations<OrchestrationWorkflow> updateOps =
        wingsPersistence.createUpdateOperations(OrchestrationWorkflow.class).set("workflowPhaseIds", phaseIds);

    if (orchestrationWorkflow.getWorkflowPhaseIdMap() != null
        && orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseId) != null) {
      updateOps.unset("workflowPhaseIdMap." + phaseId);

      // TODO: subworkflows cleanup
      updateOps.unset("graph.subworkflows." + phaseId);
    }

    if (orchestrationWorkflow.getRollbackWorkflowPhaseIdMap() != null
        && orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseId) != null) {
      updateOps.unset("rollbackWorkflowPhaseIdMap." + phaseId);

      // TODO: subworkflows cleanup
      updateOps.unset(
          "graph.subworkflows." + orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseId).getUuid());
    }

    wingsPersistence.update(query, updateOps);
    updateStateMachine(appId, orchestrationWorkflowId);
  }

  @Override
  public Node updateGraphNode(String appId, String orchestrationWorkflowId, String subworkflowId, Node node) {
    OrchestrationWorkflow orchestrationWorkflow = readOrchestrationWorkflow(appId, orchestrationWorkflowId);
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
    if (found) {
      Query<OrchestrationWorkflow> query = wingsPersistence.createQuery(OrchestrationWorkflow.class)
                                               .field("appId")
                                               .equal(appId)
                                               .field(ID_KEY)
                                               .equal(orchestrationWorkflowId);
      UpdateOperations<OrchestrationWorkflow> updateOps =
          wingsPersistence.createUpdateOperations(OrchestrationWorkflow.class)
              .set("graph.subworkflows." + subworkflowId, graph);
      wingsPersistence.update(query, updateOps);

      return node;
    }

    updateStateMachine(appId, orchestrationWorkflowId);
    return null;
  }

  @Override
  public List<NotificationRule> updateNotificationRules(
      String appId, String orchestrationWorkflowId, List<NotificationRule> notificationRules) {
    updateOrchestrationWorkflowField(appId, orchestrationWorkflowId, "notificationRules", notificationRules);
    return notificationRules;
  }

  @Override
  public List<FailureStrategy> updateFailureStrategies(
      String appId, String orchestrationWorkflowId, List<FailureStrategy> failureStrategies) {
    updateOrchestrationWorkflowField(appId, orchestrationWorkflowId, "failureStrategies", failureStrategies);
    return failureStrategies;
  }

  @Override
  public List<Variable> updateUserVariables(
      String appId, String orchestrationWorkflowId, List<Variable> userVariables) {
    updateOrchestrationWorkflowField(appId, orchestrationWorkflowId, "userVariables", userVariables);
    return userVariables;
  }

  private void updateStateMachine(String appId, String orchestrationWorkflowId) {
    OrchestrationWorkflow orchestrationWorkflow = readOrchestrationWorkflow(appId, orchestrationWorkflowId);
    if (orchestrationWorkflow.getGraph() != null) {
      StateMachine stateMachine = new StateMachine(orchestrationWorkflow, stencilMap());
      wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
    }
    updateRequiredEntities(orchestrationWorkflow);
  }

  private void updateRequiredEntities(OrchestrationWorkflow orchestrationWorkflow) {
    Set<EntityType> requiredEntityTypes = new HashSet<>();

    if (orchestrationWorkflow != null && orchestrationWorkflow.getWorkflowPhases() != null) {
      for (WorkflowPhase workflowPhase : orchestrationWorkflow.getWorkflowPhases()) {
        requiredEntityTypes.addAll(getRequiredEntityTypes(orchestrationWorkflow.getAppId(), workflowPhase));
      }
    }
    updateOrchestrationWorkflowField(
        orchestrationWorkflow.getAppId(), orchestrationWorkflow.getUuid(), "requiredEntityTypes", requiredEntityTypes);
  }

  private Set<EntityType> getRequiredEntityTypes(String appId, WorkflowPhase workflowPhase) {
    Set<EntityType> requiredEntityTypes = new HashSet<>();
    if (workflowPhase.getDeploymentType() == DeploymentType.ECS) {
      requiredEntityTypes.add(EntityType.ARTIFACT);
      return requiredEntityTypes;
    }

    if (workflowPhase == null || workflowPhase.getPhaseSteps() == null) {
      return requiredEntityTypes;
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

  private void generateNewWorkflowPhaseSteps(String appId, String envId, WorkflowPhase workflowPhase, int phaseIndex) {
    DeploymentType deploymentType = workflowPhase.getDeploymentType();
    if (deploymentType == DeploymentType.ECS) {
      generateNewWorkflowPhaseStepsForECS(appId, envId, workflowPhase, phaseIndex == 0);
    } else {
      generateNewWorkflowPhaseStepsForSSH(appId, envId, workflowPhase);
    }
  }

  private void generateNewWorkflowPhaseStepsForECS(
      String appId, String envId, WorkflowPhase workflowPhase, boolean includeContainer) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service, DeploymentType.ECS);

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.CONTAINER_SETUP)
                                   .withName("Setup Container")
                                   .addStep(aNode()
                                                .withId(getUuid())
                                                .withType(StateType.ECS_SERVICE_SETUP.name())
                                                .withName("ECS Service Setup")
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(
        aPhaseStep(PhaseStepType.CONTAINER_DEPLOY)
            .withName("Deploy Containers")
            .addStep(
                aNode().withId(getUuid()).withType(ECS_SERVICE_DEPLOY.name()).withName("ECS Sevice Deploy").build())
            .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.VERIFY_SERVICE)
                                   .withName("Verify Service")
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());
  }

  private void generateNewWorkflowPhaseStepsForSSH(String appId, String envId, WorkflowPhase workflowPhase) {
    // For DC only - for other types it has to be customized

    StateType stateType =
        determineStateType(appId, envId, workflowPhase.getServiceId(), workflowPhase.getComputeProviderId());

    if (!Arrays.asList(DC_NODE_SELECT, AWS_NODE_SELECT).contains(stateType)) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Unsupported state type: " + stateType);
    }

    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service, DeploymentType.SSH);

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.PROVISION_NODE)
                                   .withName(Constants.PROVISION_NODE_NAME)
                                   .addStep(aNode()
                                                .withType(stateType.name())
                                                .withName("Select Nodes")
                                                .withOrigin(true)
                                                .addProperty("specificHosts", false)
                                                .addProperty("instanceCount", 1)
                                                .build())
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.DISABLE_SERVICE)
                                   .withName("Disable Service")
                                   .addAllSteps(commandNodes(commandMap, CommandType.DISABLE))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.DEPLOY_SERVICE)
                                   .withName("Deploy Service")
                                   .addAllSteps(commandNodes(commandMap, CommandType.INSTALL))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.VERIFY_SERVICE)
                                   .withName("Verify Service")
                                   .addAllSteps(commandNodes(commandMap, CommandType.VERIFY))
                                   .build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.ENABLE_SERVICE)
                                   .withName("Enable Service")
                                   .addAllSteps(commandNodes(commandMap, CommandType.ENABLE))
                                   .build());

    // Not needed for non-DC
    // workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.DEPROVISION_NODE).build());

    workflowPhase.addPhaseStep(aPhaseStep(PhaseStepType.WRAP_UP).withName("Wrap Up").build());
  }

  private WorkflowPhase generateRollbackWorkflowPhase(String appId, WorkflowPhase workflowPhase) {
    DeploymentType deploymentType = workflowPhase.getDeploymentType();
    if (deploymentType == DeploymentType.ECS) {
      return generateRollbackWorkflowPhaseForECS(appId, workflowPhase);
    } else {
      return generateRollbackWorkflowPhaseForSSH(appId, workflowPhase);
    }
  }

  private WorkflowPhase generateRollbackWorkflowPhaseForECS(String appId, WorkflowPhase workflowPhase) {
    Service service = serviceResourceService.get(appId, workflowPhase.getServiceId());
    Map<CommandType, List<Command>> commandMap = getCommandTypeListMap(service, DeploymentType.SSH);

    WorkflowPhase rollbackWorkflowPhase =
        aWorkflowPhase()
            .withName(Constants.ROLLBACK_PREFIX + workflowPhase.getName())
            .withRollback(true)
            .withServiceId(workflowPhase.getServiceId())
            .withComputeProviderId(workflowPhase.getComputeProviderId())
            .withRollbackPhaseName(workflowPhase.getName())
            .withDeploymentType(workflowPhase.getDeploymentType())
            .withDeploymentMasterId(workflowPhase.getDeploymentMasterId())
            .addPhaseStep(aPhaseStep(PhaseStepType.STOP_SERVICE)
                              .withName("Stop Service")
                              .addAllSteps(commandNodes(commandMap, CommandType.RESIZE))
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
            .withRollbackPhaseName(workflowPhase.getName())
            .withDeploymentType(workflowPhase.getDeploymentType())
            .withDeploymentMasterId(workflowPhase.getDeploymentMasterId())
            .addPhaseStep(aPhaseStep(PhaseStepType.DISABLE_SERVICE)
                              .withName("Disable Service")
                              .addAllSteps(commandNodes(commandMap, CommandType.DISABLE))
                              .build())
            .addPhaseStep(aPhaseStep(PhaseStepType.STOP_SERVICE)
                              .withName("Stop Service")
                              .addAllSteps(commandNodes(commandMap, CommandType.STOP))
                              .build())
            .addPhaseStep(aPhaseStep(PhaseStepType.ENABLE_SERVICE)
                              .withName("Enable Service")
                              .addAllSteps(commandNodes(commandMap, CommandType.ENABLE))
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
            aPhaseStep(PhaseStepType.DE_PROVISION_NODE).withName("Deprovision Nodes").build());
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
                    .build());
    }
    return nodes;
  }

  private StateType determineStateType(String appId, String envId, String serviceId, String computeProviderId) {
    InfrastructureMapping infrastructureMapping =
        infrastructureMappingService.getInfraMappingByComputeProviderAndServiceId(
            appId, envId, serviceId, computeProviderId);

    StateType stateType =
        infrastructureMapping instanceof PhysicalInfrastructureMapping ? DC_NODE_SELECT : AWS_NODE_SELECT;

    return stateType;
  }

  private void populatePhaseStepIds(WorkflowPhase workflowPhase) {
    if (workflowPhase == null || workflowPhase.getPhaseSteps() == null || workflowPhase.getPhaseSteps().isEmpty()) {
      return;
    }
    workflowPhase.getPhaseSteps().forEach(this ::populatePhaseStepIds);
  }

  private void populatePhaseStepIds(PhaseStep phaseStep) {
    if (phaseStep == null || phaseStep.getSteps() == null) {
      logger.error("Incorrect arguments to populate phaseStepIds: {}", phaseStep);
      return;
    }
    phaseStep.setStepsIds(phaseStep.getSteps().stream().map(Node::getId).collect(toList()));
  }

  private void populatePhaseSteps(PhaseStep phaseStep, Graph graph) {
    if (phaseStep == null || phaseStep.getUuid() == null || graph == null || graph.getSubworkflows() == null
        || graph.getSubworkflows().get(phaseStep.getUuid()) == null) {
      logger.error("Incorrect arguments to populate phaseStep: {}, graph: {}", phaseStep, graph);
      return;
    }
    if (phaseStep.getStepsIds() == null || phaseStep.getStepsIds().isEmpty()) {
      logger.info("Empty stepList for the phaseStep: {}", phaseStep);
      return;
    }

    Graph subWorkflowGraph = graph.getSubworkflows().get(phaseStep.getUuid());
    if (subWorkflowGraph == null) {
      logger.info("No subworkflow found for the phaseStep: {}", phaseStep);
      return;
    }

    Map<String, Node> nodesMap = subWorkflowGraph.getNodesMap();
    phaseStep.setSteps(phaseStep.getStepsIds().stream().map(stepId -> nodesMap.get(stepId)).collect(toList()));
  }

  private Graph generateMainGraph(OrchestrationWorkflow orchestrationWorkflow) {
    String id1 = orchestrationWorkflow.getPreDeploymentSteps().getUuid();
    String id2;
    Node preDeploymentNode = orchestrationWorkflow.getPreDeploymentSteps().generatePhaseStepNode();
    preDeploymentNode.setOrigin(true);
    Builder graphBuilder = aGraph()
                               .addNodes(preDeploymentNode)
                               .addSubworkflow(id1, generateGraph(orchestrationWorkflow.getPreDeploymentSteps()));

    List<WorkflowPhase> workflowPhases = orchestrationWorkflow.getWorkflowPhases();

    Map<String, WorkflowPhase> rollbackPhaseMap = orchestrationWorkflow.getRollbackWorkflowPhaseIdMap();
    if (rollbackPhaseMap == null) {
      rollbackPhaseMap = new HashMap<>();
    }

    if (workflowPhases != null) {
      for (WorkflowPhase workflowPhase : workflowPhases) {
        id2 = workflowPhase.getUuid();
        graphBuilder.addNodes(workflowPhase.generatePhaseNode())
            .addLinks(
                aLink().withId(getUuid()).withFrom(id1).withTo(id2).withType(TransitionType.SUCCESS.name()).build());

        if (rollbackPhaseMap.get(workflowPhase.getUuid()) != null) {
          Node rollbackNode = rollbackPhaseMap.get(workflowPhase.getUuid()).generatePhaseNode();
          graphBuilder.addNodes(rollbackNode);
        }
        id1 = id2;
      }
    }
    id2 = orchestrationWorkflow.getPostDeploymentSteps().getUuid();
    graphBuilder.addNodes(orchestrationWorkflow.getPostDeploymentSteps().generatePhaseStepNode())
        .addLinks(aLink().withId(getUuid()).withFrom(id1).withTo(id2).withType(TransitionType.SUCCESS.name()).build())
        .addSubworkflow(id2, generateGraph(orchestrationWorkflow.getPostDeploymentSteps()));

    return graphBuilder.build();
  }

  private Map<String, Graph> generateGraph(WorkflowPhase workflowPhase) {
    Map<String, Graph> graphs = new HashMap<>();
    Builder graphBuilder = aGraph().withGraphName(workflowPhase.getName());

    String id1 = null;
    String id2;
    Node node;
    for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
      id2 = phaseStep.getUuid();
      node = phaseStep.generatePhaseStepNode();
      graphBuilder.addNodes(node);
      if (id1 == null) {
        node.setOrigin(true);
      } else {
        graphBuilder.addLinks(
            aLink().withId(getUuid()).withFrom(id1).withTo(id2).withType(TransitionType.SUCCESS.name()).build());
      }
      id1 = id2;
      Graph stepsGraph = generateGraph(phaseStep);
      graphs.put(phaseStep.getUuid(), stepsGraph);
    }

    graphs.put(workflowPhase.getUuid(), graphBuilder.build());
    return graphs;
  }

  private Graph generateGraph(PhaseStep phaseStep) {
    Builder graphBuilder = aGraph().withGraphName(phaseStep.getName());
    if (phaseStep == null || phaseStep.getSteps() == null || phaseStep.getSteps().isEmpty()) {
      return graphBuilder.build();
    }

    Node originNode = null;
    Node repeatNode = null;
    if (phaseStep.getPhaseStepType() == PhaseStepType.DEPLOY_SERVICE
        || phaseStep.getPhaseStepType() == PhaseStepType.DISABLE_SERVICE
        || phaseStep.getPhaseStepType() == PhaseStepType.ENABLE_SERVICE
        || phaseStep.getPhaseStepType() == PhaseStepType.VERIFY_SERVICE) {
      // TODO - only meant for physical DC
      // introduce repeat node

      repeatNode = aNode()
                       .withType(REPEAT.name())
                       .withName("All Instances")
                       .addProperty("executionStrategy", "PARALLEL")
                       .addProperty("repeatElementExpression", "${instances}")
                       .build();

      graphBuilder.addNodes(repeatNode);
    }

    if (phaseStep.isStepsInParallel()) {
      Node forkNode = aNode().withId(getUuid()).withType(FORK.name()).withName(phaseStep.getName() + "-FORK").build();
      for (Node step : phaseStep.getSteps()) {
        graphBuilder.addNodes(step);
        graphBuilder.addLinks(aLink()
                                  .withId(getUuid())
                                  .withFrom(forkNode.getId())
                                  .withTo(step.getId())
                                  .withType(TransitionType.FORK.name())
                                  .build());
      }
      if (originNode == null) {
        originNode = forkNode;
      }
    } else {
      String id1 = null;
      String id2;
      for (Node step : phaseStep.getSteps()) {
        id2 = step.getId();
        graphBuilder.addNodes(step);
        if (id1 == null && originNode == null) {
          originNode = step;
        } else {
          graphBuilder.addLinks(
              aLink().withId(getUuid()).withFrom(id1).withTo(id2).withType(TransitionType.SUCCESS.name()).build());
        }
        id1 = id2;
      }
    }
    if (repeatNode == null) {
      originNode.setOrigin(true);
    } else {
      repeatNode.setOrigin(true);
      graphBuilder.addLinks(aLink()
                                .withId(getUuid())
                                .withFrom(repeatNode.getId())
                                .withTo(originNode.getId())
                                .withType(TransitionType.REPEAT.name())
                                .build());
    }

    return graphBuilder.build();
  }

  private void updateOrchestrationWorkflowField(
      String appId, String orchestrationWorkflowId, String fieldName, Object fieldValue) {
    Map<String, Object> map = new HashMap<>();
    map.put(fieldName, fieldValue);
    updateOrchestrationWorkflowField(appId, orchestrationWorkflowId, map);
  }

  private void updateOrchestrationWorkflowField(
      String appId, String orchestrationWorkflowId, Map<String, Object> values) {
    Query<OrchestrationWorkflow> query = wingsPersistence.createQuery(OrchestrationWorkflow.class)
                                             .field("appId")
                                             .equal(appId)
                                             .field(ID_KEY)
                                             .equal(orchestrationWorkflowId);
    UpdateOperations<OrchestrationWorkflow> updateOps =
        wingsPersistence.createUpdateOperations(OrchestrationWorkflow.class);
    for (Entry<String, Object> entry : values.entrySet()) {
      updateOps.set(entry.getKey(), entry.getValue());
    }
    wingsPersistence.update(query, updateOps);
  }
}
