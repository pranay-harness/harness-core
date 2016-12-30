/**
 *
 */

package software.wings.service.impl;

import static com.google.common.base.Strings.nullToEmpty;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.EntityVersion.Builder.anEntityVersion;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.app.StaticConfiguration;
import software.wings.beans.Base;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.ErrorCodes;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Graph;
import software.wings.beans.NotificationRule;
import software.wings.beans.Orchestration;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Pipeline;
import software.wings.beans.ReadPref;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowFailureStrategy;
import software.wings.beans.WorkflowOuterSteps;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.stencils.DataProvider;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.MapperUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
    stencils.addAll(Arrays.asList(StateType.values()));

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
    return wingsPersistence.query(OrchestrationWorkflow.class, pageRequest);
  }

  @Override
  public OrchestrationWorkflow readOrchestrationWorkflow(String appId, String orchestrationWorkflowId) {
    return wingsPersistence.get(OrchestrationWorkflow.class, appId, orchestrationWorkflowId);
  }

  @Override
  public OrchestrationWorkflow createOrchestrationWorkflow(OrchestrationWorkflow orchestrationWorkflow) {
    return wingsPersistence.saveAndGet(OrchestrationWorkflow.class, orchestrationWorkflow);
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
  public WorkflowOuterSteps updatePreDeployment(
      String appId, String orchestrationWorkflowId, WorkflowOuterSteps workflowOuterSteps) {
    updateOrchestrationWorkflowField(appId, orchestrationWorkflowId, "preDeploymentSteps", workflowOuterSteps);
    return workflowOuterSteps;
  }

  @Override
  public WorkflowOuterSteps updatePostDeployment(
      String appId, String orchestrationWorkflowId, WorkflowOuterSteps workflowOuterSteps) {
    updateOrchestrationWorkflowField(appId, orchestrationWorkflowId, "postDeploymentSteps", workflowOuterSteps);
    return workflowOuterSteps;
  }

  private void updateOrchestrationWorkflowField(
      String appId, String orchestrationWorkflowId, String fieldName, Object fieldValue) {
    Query<OrchestrationWorkflow> query = wingsPersistence.createQuery(OrchestrationWorkflow.class)
                                             .field("appId")
                                             .equal(appId)
                                             .field(ID_KEY)
                                             .equal(orchestrationWorkflowId);
    UpdateOperations<OrchestrationWorkflow> updateOps =
        wingsPersistence.createUpdateOperations(OrchestrationWorkflow.class).set(fieldName, fieldValue);

    wingsPersistence.update(query, updateOps);
  }

  @Override
  public WorkflowPhase createWorkflowPhase(String appId, String orchestrationWorkflowId, WorkflowPhase workflowPhase) {
    Query<OrchestrationWorkflow> query = wingsPersistence.createQuery(OrchestrationWorkflow.class)
                                             .field("appId")
                                             .equal(appId)
                                             .field(ID_KEY)
                                             .equal(orchestrationWorkflowId);
    workflowPhase.setUuid(UUIDGenerator.getUuid());

    workflowPhase.addPhaseStep(aPhaseStep().withPhaseStepType(PhaseStepType.PROVISION_NODE).build());
    workflowPhase.addPhaseStep(aPhaseStep().withPhaseStepType(PhaseStepType.DEPLOY_SERVICE).build());
    workflowPhase.addPhaseStep(aPhaseStep().withPhaseStepType(PhaseStepType.ENABLE_SERVICE).build());
    workflowPhase.addPhaseStep(aPhaseStep().withPhaseStepType(PhaseStepType.VERIFY_SERVICE).build());
    workflowPhase.addPhaseStep(aPhaseStep().withPhaseStepType(PhaseStepType.DISABLE_SERVICE).build());
    workflowPhase.addPhaseStep(aPhaseStep().withPhaseStepType(PhaseStepType.DEPROVISION_NODE).build());
    workflowPhase.addPhaseStep(aPhaseStep().withPhaseStepType(PhaseStepType.WRAP_UP).build());

    UpdateOperations<OrchestrationWorkflow> updateOps =
        wingsPersistence.createUpdateOperations(OrchestrationWorkflow.class).add("workflowPhases", workflowPhase);

    wingsPersistence.update(query, updateOps);

    return workflowPhase;
  }

  @Override
  public WorkflowPhase updateWorkflowPhase(String appId, String orchestrationWorkflowId, WorkflowPhase workflowPhase) {
    OrchestrationWorkflow orchestrationWorkflow = readOrchestrationWorkflow(appId, orchestrationWorkflowId);

    if (orchestrationWorkflow == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "orchestrationWorkflowId");
    }
    List<WorkflowPhase> workflowPhases = orchestrationWorkflow.getWorkflowPhases();
    if (workflowPhases == null || workflowPhases.isEmpty() || workflowPhase == null
        || workflowPhase.getUuid() == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "workflowPhase");
    }

    Map<String, WorkflowPhase> workflowMap = workflowPhases.stream().collect(toMap(WorkflowPhase::getUuid, identity()));

    WorkflowPhase origWorkflowPhase = workflowMap.get(workflowPhase.getUuid());

    MapperUtils.mapObject(workflowPhase, origWorkflowPhase);

    Query<OrchestrationWorkflow> query = wingsPersistence.createQuery(OrchestrationWorkflow.class)
                                             .field("appId")
                                             .equal(appId)
                                             .field(ID_KEY)
                                             .equal(orchestrationWorkflowId);
    UpdateOperations<OrchestrationWorkflow> updateOps =
        wingsPersistence.createUpdateOperations(OrchestrationWorkflow.class).add("workflowPhases", workflowPhase);

    wingsPersistence.update(query, updateOps);

    return origWorkflowPhase;
  }

  @Override
  public void deleteWorkflowPhase(String appId, String orchestrationWorkflowId, String phaseId) {
    OrchestrationWorkflow orchestrationWorkflow = readOrchestrationWorkflow(appId, orchestrationWorkflowId);

    if (orchestrationWorkflow == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "orchestrationWorkflowId");
    }
    List<WorkflowPhase> workflowPhases = orchestrationWorkflow.getWorkflowPhases();
    if (workflowPhases == null || workflowPhases.isEmpty() || phaseId == null) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "workflowPhase");
    }

    Map<String, WorkflowPhase> workflowMap = workflowPhases.stream().collect(toMap(WorkflowPhase::getUuid, identity()));

    WorkflowPhase origWorkflowPhase = workflowMap.get(phaseId);

    workflowPhases.remove(origWorkflowPhase);

    Query<OrchestrationWorkflow> query = wingsPersistence.createQuery(OrchestrationWorkflow.class)
                                             .field("appId")
                                             .equal(appId)
                                             .field(ID_KEY)
                                             .equal(orchestrationWorkflowId);
    UpdateOperations<OrchestrationWorkflow> updateOps =
        wingsPersistence.createUpdateOperations(OrchestrationWorkflow.class).set("workflowPhases", workflowPhases);

    wingsPersistence.update(query, updateOps);
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
}
