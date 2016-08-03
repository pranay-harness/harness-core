/**
 *
 */

package software.wings.service.impl;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCodes.INVALID_PIPELINE;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.dl.MongoHelper.setUnset;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import org.apache.commons.jexl3.JxltEngine.Exception;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.api.SimpleWorkflowParam;
import software.wings.app.StaticConfiguration;
import software.wings.beans.Artifact;
import software.wings.beans.EntityType;
import software.wings.beans.ErrorCodes;
import software.wings.beans.EventType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Graph;
import software.wings.beans.History;
import software.wings.beans.Orchestration;
import software.wings.beans.Pipeline;
import software.wings.beans.ReadPref;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceInstance;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.command.Command;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsDeque;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HistoryService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionEvent;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.ExpressionProcessorFactory;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionCallback;
import software.wings.sm.StateMachineExecutionEventManager;
import software.wings.sm.StateMachineExecutionSimulator;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilPostProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
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
public class WorkflowServiceImpl implements WorkflowService {
  private static final Comparator<Stencil> stencilDefaultSorter = new Comparator<Stencil>() {

    @Override
    public int compare(Stencil o1, Stencil o2) {
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
    }
  };
  private final Logger logger = LoggerFactory.getLogger(getClass());
  /**
   * The Expression processor factory.
   */
  @Inject ExpressionProcessorFactory expressionProcessorFactory;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private StateMachineExecutor stateMachineExecutor;
  @Inject private PluginManager pluginManager;
  @Inject private EnvironmentService environmentService;
  @Inject private StaticConfiguration staticConfiguration;
  @Inject private StencilPostProcessor stencilPostProcessor;
  @Inject private StateMachineExecutionEventManager stateMachineExecutionEventManager;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private ArtifactService artifactService;
  @Inject private StateMachineExecutionSimulator stateMachineExecutionSimulator;
  @Inject private GraphRenderer graphRenderer;
  @Inject private ExecutorService executorService;
  @Inject private HistoryService historyService;

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
  public void trigger(String appId, String stateMachineId, String executionUuid) {
    trigger(appId, stateMachineId, executionUuid, null);
  }

  /**
   * Trigger.
   *
   * @param appId          the app id
   * @param stateMachineId the state machine id
   * @param executionUuid  the execution uuid
   * @param callback       the callback
   */
  void trigger(String appId, String stateMachineId, String executionUuid, StateMachineExecutionCallback callback) {
    stateMachineExecutor.execute(appId, stateMachineId, executionUuid, null, callback);
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

  private Map<String, StateTypeDescriptor> stencilMap() {
    if (cachedStencilMap == null) {
      stencils(null);
    }
    return cachedStencilMap;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Pipeline> listPipelines(PageRequest<Pipeline> pageRequest) {
    PageResponse<Pipeline> res = wingsPersistence.query(Pipeline.class, pageRequest);
    if (res != null && res.size() > 0) {
      for (Pipeline pipeline : res.getResponse()) {
        StateMachine stateMachine = readLatest(pipeline.getAppId(), pipeline.getUuid(), null);
        if (stateMachine != null) {
          pipeline.setGraph(stateMachine.getGraph());
        }
      }
    }
    return res;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Workflow> T createWorkflow(Class<T> cls, T workflow) {
    Graph graph = workflow.getGraph();
    if (cls == Pipeline.class && graph != null) {
      try {
        if (!graph.isLinear()) {
          throw new WingsException(INVALID_PIPELINE);
        }
      } catch (Exception e) {
        throw new WingsException(INVALID_PIPELINE, e);
      }
    }

    workflow = wingsPersistence.saveAndGet(cls, workflow);
    if (graph != null) {
      StateMachine stateMachine = new StateMachine(workflow, graph, stencilMap());
      stateMachine = wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
      workflow.setGraph(stateMachine.getGraph());
    }
    return workflow;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Workflow> T updateWorkflow(T workflow) {
    Graph graph = workflow.getGraph();
    if (graph != null) {
      StateMachine stateMachine = new StateMachine(workflow, graph, stencilMap());
      stateMachine = wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
      workflow.setGraph(stateMachine.getGraph());
    }
    return workflow;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pipeline updatePipeline(Pipeline pipeline) {
    UpdateOperations<Pipeline> ops = wingsPersistence.createUpdateOperations(Pipeline.class);
    setUnset(ops, "description", pipeline.getDescription());
    setUnset(ops, "cronSchedule", pipeline.getCronSchedule());
    setUnset(ops, "name", pipeline.getName());
    setUnset(ops, "services", pipeline.getServices());

    wingsPersistence.update(wingsPersistence.createQuery(Pipeline.class)
                                .field("appId")
                                .equal(pipeline.getAppId())
                                .field(ID_KEY)
                                .equal(pipeline.getUuid()),
        ops);

    Graph graph = pipeline.getGraph();
    pipeline = updateWorkflow(pipeline);
    pipeline.setGraph(graph);
    return pipeline;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pipeline readPipeline(String appId, String pipelineId) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    StateMachine stateMachine = readLatest(appId, pipelineId, null);
    if (stateMachine != null) {
      pipeline.setGraph(stateMachine.getGraph());
    }
    return pipeline;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StateMachine readLatest(String appId, String originId, String name) {
    if (StringUtils.isBlank(name)) {
      name = Constants.DEFAULT_WORKFLOW_NAME;
    }

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
    filter.setFieldName("name");
    filter.setFieldValues(name);
    filter.setOp(Operator.EQ);
    req.addFilter(filter);

    return wingsPersistence.get(StateMachine.class, req);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Orchestration> listOrchestration(PageRequest<Orchestration> pageRequest) {
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
    PageResponse<Orchestration> res = wingsPersistence.query(Orchestration.class, pageRequest);
    if (res != null && res.size() > 0) {
      for (Orchestration orchestration : res.getResponse()) {
        StateMachine stateMachine = readLatest(orchestration.getAppId(), orchestration.getUuid(), null);
        if (stateMachine != null) {
          orchestration.setGraph(stateMachine.getGraph());
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
    wingsPersistence.update(wingsPersistence.createQuery(Orchestration.class)
                                .field("appId")
                                .equal(orchestration.getAppId())
                                .field(ID_KEY)
                                .equal(orchestration.getUuid()),
        ops);

    Graph graph = orchestration.getGraph();
    orchestration = updateWorkflow(orchestration);
    return orchestration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Orchestration readOrchestration(String appId, String envId, String orchestrationId) {
    Orchestration orchestration = wingsPersistence.get(Orchestration.class, appId, orchestrationId);

    if (orchestration == null) {
      return orchestration;
    }
    StateMachine stateMachine = readLatest(appId, orchestrationId, null);
    if (stateMachine != null) {
      orchestration.setGraph(stateMachine.getGraph());
    }
    return orchestration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<WorkflowExecution> listExecutions(
      PageRequest<WorkflowExecution> pageRequest, boolean includeGraph) {
    PageResponse<WorkflowExecution> res = wingsPersistence.query(WorkflowExecution.class, pageRequest);
    if (res == null || res.size() == 0) {
      return res;
    }
    if (!includeGraph) {
      return res;
    }
    for (WorkflowExecution workflowExecution : res) {
      populateGraph(
          workflowExecution, null, null, null, false, (workflowExecution.getWorkflowType() == WorkflowType.SIMPLE));
    }
    return res;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution getExecutionDetails(String appId, String workflowExecutionId) {
    return getExecutionDetails(appId, workflowExecutionId, null, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution getExecutionDetails(String appId, String workflowExecutionId, List<String> expandedGroupIds,
      String requestedGroupId, Graph.NodeOps nodeOps) {
    if (expandedGroupIds == null) {
      expandedGroupIds = new ArrayList<>();
    }
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionId);
    if (workflowExecution != null) {
      populateGraph(workflowExecution, expandedGroupIds, requestedGroupId, nodeOps, true);
    }
    if (workflowExecution.getExecutionArgs() != null) {
      if (workflowExecution.getExecutionArgs().getServiceInstanceIdNames() != null) {
        PageRequest<ServiceInstance> pageRequest =
            PageRequest.Builder.aPageRequest()
                .addFilter("appId", Operator.EQ, appId)
                .addFilter("uuid", Operator.IN,
                    workflowExecution.getExecutionArgs().getServiceInstanceIdNames().keySet().toArray())
                .build();
        workflowExecution.getExecutionArgs().setServiceInstances(
            serviceInstanceService.list(pageRequest).getResponse());
      }
      if (workflowExecution.getExecutionArgs().getArtifactIdNames() != null) {
        PageRequest<Artifact> pageRequest =
            PageRequest.Builder.aPageRequest()
                .addFilter("appId", Operator.EQ, appId)
                .addFilter(
                    "uuid", Operator.IN, workflowExecution.getExecutionArgs().getArtifactIdNames().keySet().toArray())
                .build();
        workflowExecution.getExecutionArgs().setArtifacts(artifactService.list(pageRequest).getResponse());
      }
    }
    workflowExecution.setExpandedGroupIds(expandedGroupIds);
    return workflowExecution;
  }

  private void populateGraph(WorkflowExecution workflowExecution, List<String> expandedGroupIds,
      String requestedGroupId, Graph.NodeOps nodeOps, boolean detailsRequested) {
    populateGraph(workflowExecution, expandedGroupIds, requestedGroupId, nodeOps, detailsRequested, false);
  }

  private void populateGraph(WorkflowExecution workflowExecution, List<String> expandedGroupIds,
      String requestedGroupId, Graph.NodeOps nodeOps, boolean detailsRequested, boolean isSimpleLinear) {
    if (expandedGroupIds == null) {
      expandedGroupIds = new ArrayList<>();
    }
    if (nodeOps != Graph.NodeOps.COLLAPSE && requestedGroupId != null && !expandedGroupIds.contains(requestedGroupId)) {
      expandedGroupIds.add(requestedGroupId);
    }
    List<StateExecutionInstance> instances = queryStateExecutionInstances(workflowExecution, expandedGroupIds, false);
    if (instances != null && instances.size() > 0 && (requestedGroupId == null || nodeOps == null)
        && (expandedGroupIds == null || expandedGroupIds.isEmpty())) {
      StateExecutionInstance firstLevelGroup = getFirstLevelGroup(instances);
      if (firstLevelGroup != null) {
        expandedGroupIds = Lists.newArrayList(firstLevelGroup.getUuid());
        populateGraph(workflowExecution, expandedGroupIds, requestedGroupId, nodeOps, detailsRequested, isSimpleLinear);
        return;
      }
    }

    Map<String, StateExecutionInstance> instanceIdMap =
        instances.stream().collect(toMap(StateExecutionInstance::getUuid, identity()));
    HashSet<String> missingParentInstanceIds = getMissingParentInstanceIds(instances, expandedGroupIds);

    while (missingParentInstanceIds != null && missingParentInstanceIds.size() > 0) {
      List<StateExecutionInstance> moreInstances =
          queryStateExecutionInstances(workflowExecution, missingParentInstanceIds, true);
      if (moreInstances != null) {
        Map<String, StateExecutionInstance> moreInstanceIdMap =
            moreInstances.stream().collect(toMap(StateExecutionInstance::getUuid, identity()));
        instanceIdMap.putAll(moreInstanceIdMap);
      }
      if (!instanceIdMap.keySet().containsAll(missingParentInstanceIds)) {
        WingsException ex =
            new WingsException("Corrupt data.. some of parentinstanceIds are invalid " + missingParentInstanceIds);
        logger.error(ex.getMessage(), ex);
        throw ex;
      }
      expandedGroupIds.addAll(missingParentInstanceIds);
      missingParentInstanceIds = getMissingParentInstanceIds(instances, expandedGroupIds);
    }

    if (nodeOps == Graph.NodeOps.COLLAPSE && requestedGroupId != null) {
      List<String> childrenIds = new ArrayList<>();
      collectChildrenIds(instanceIdMap, requestedGroupId, childrenIds);
      childrenIds.forEach(childId -> { instanceIdMap.remove(childId); });
      if (expandedGroupIds.contains(requestedGroupId)) {
        expandedGroupIds.remove(requestedGroupId);
      }
    }

    StateMachine sm =
        wingsPersistence.get(StateMachine.class, workflowExecution.getAppId(), workflowExecution.getStateMachineId());
    Graph graph =
        graphRenderer.generateGraph(instanceIdMap, sm.getInitialStateName(), expandedGroupIds, detailsRequested);
    workflowExecution.setGraph(graph);
    if (pausedNodesFound(workflowExecution)) {
      workflowExecution.setStatus(ExecutionStatus.PAUSED);
    }
  }

  private StateExecutionInstance getFirstLevelGroup(List<StateExecutionInstance> instances) {
    for (StateExecutionInstance instance : instances) {
      if (instance.getStateType().equals(StateType.REPEAT.name())
          || instance.getStateType().equals(StateType.FORK.name())) {
        return instance;
      }
    }
    return null;
  }

  private boolean pausedNodesFound(WorkflowExecution workflowExecution) {
    PageRequest<StateExecutionInstance> req = PageRequest.Builder.aPageRequest()
                                                  .addFilter("appId", Operator.EQ, workflowExecution.getAppId())
                                                  .addFilter("executionUuid", Operator.EQ, workflowExecution.getUuid())
                                                  .addFilter("status", Operator.EQ, ExecutionStatus.PAUSED)
                                                  .build();
    return wingsPersistence.get(StateExecutionInstance.class, req) != null;
  }

  private void collectChildrenIds(
      Map<String, StateExecutionInstance> instanceIdMap, String collapseGroupId, List<String> childrenIds) {
    if (collapseGroupId == null || instanceIdMap.get(collapseGroupId) == null) {
      return;
    }
    for (StateExecutionInstance instance : instanceIdMap.values()) {
      if (collapseGroupId.equals(instance.getParentInstanceId())) {
        childrenIds.add(instance.getUuid());
        collectChildrenIds(instanceIdMap, instance.getUuid(), childrenIds);
      }
    }
  }

  private HashSet<String> getMissingParentInstanceIds(List<StateExecutionInstance> list, List<String> instanceIds) {
    if (list == null || list.isEmpty()) {
      return null;
    }

    HashSet<String> missingParentInstanceIds = new HashSet<>();
    for (StateExecutionInstance instance : list) {
      if (instance.getParentInstanceId() != null
          && (instanceIds == null || !instanceIds.contains(instance.getParentInstanceId()))) {
        missingParentInstanceIds.add(instance.getParentInstanceId());
      }
    }

    return missingParentInstanceIds;
  }

  private List<StateExecutionInstance> queryStateExecutionInstances(
      WorkflowExecution workflowExecution, Collection<String> expandedGroupIds, boolean byParentInstanceOnly) {
    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .field("appId")
                                              .equal(workflowExecution.getAppId())
                                              .field("executionUuid")
                                              .equal(workflowExecution.getUuid());
    if (byParentInstanceOnly) {
      query.or(query.criteria("uuid").in(expandedGroupIds), query.criteria("parentInstanceId").in(expandedGroupIds));
    } else {
      if (expandedGroupIds == null || expandedGroupIds.isEmpty()) {
        query.field("parentInstanceId").doesNotExist();
      } else {
        query.or(query.criteria("parentInstanceId").doesNotExist(), query.criteria("uuid").in(expandedGroupIds),
            query.criteria("parentInstanceId").in(expandedGroupIds));
      }
    }

    return wingsPersistence.executeGetListQuery(query);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution triggerPipelineExecution(String appId, String pipelineId) {
    return triggerPipelineExecution(appId, pipelineId, null);
  }

  /**
   * Trigger pipeline execution workflow execution.
   *
   * @param appId                   the app id
   * @param pipelineId              the pipeline id
   * @param workflowExecutionUpdate the workflow execution update
   * @return the workflow execution
   */
  public WorkflowExecution triggerPipelineExecution(
      String appId, String pipelineId, WorkflowExecutionUpdate workflowExecutionUpdate) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    if (pipeline == null) {
      throw new WingsException(ErrorCodes.NON_EXISTING_PIPELINE);
    }
    List<WorkflowExecution> runningWorkflowExecutions =
        getRunningWorkflowExecutions(WorkflowType.PIPELINE, appId, pipelineId);
    if (runningWorkflowExecutions != null) {
      for (WorkflowExecution workflowExecution : runningWorkflowExecutions) {
        if (workflowExecution.getStatus() == ExecutionStatus.NEW) {
          throw new WingsException(ErrorCodes.PIPELINE_ALREADY_TRIGGERED, "pilelineName", pipeline.getName());
        }
        if (workflowExecution.getStatus() == ExecutionStatus.RUNNING) {
          // Analyze if pipeline is in initial stage
        }
      }
    }

    StateMachine stateMachine = readLatest(appId, pipelineId, null);
    if (stateMachine == null) {
      throw new WingsException("No stateMachine associated with " + pipelineId);
    }

    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setAppId(appId);
    workflowExecution.setWorkflowId(pipelineId);
    workflowExecution.setWorkflowType(WorkflowType.PIPELINE);
    workflowExecution.setStateMachineId(stateMachine.getUuid());

    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);

    return triggerExecution(workflowExecution, stateMachine, workflowExecutionUpdate, stdParams);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution triggerOrchestrationExecution(
      String appId, String envId, String orchestrationId, ExecutionArgs executionArgs) {
    return triggerOrchestrationExecution(appId, envId, orchestrationId, executionArgs, null);
  }

  /**
   * Trigger orchestration execution workflow execution.
   *
   * @param appId                   the app id
   * @param envId                   the env id
   * @param orchestrationId         the orchestration id
   * @param executionArgs           the execution args
   * @param workflowExecutionUpdate the workflow execution update
   * @return the workflow execution
   */
  public WorkflowExecution triggerOrchestrationExecution(String appId, String envId, String orchestrationId,
      ExecutionArgs executionArgs, WorkflowExecutionUpdate workflowExecutionUpdate) {
    List<WorkflowExecution> runningWorkflowExecutions =
        getRunningWorkflowExecutions(WorkflowType.ORCHESTRATION, appId, orchestrationId);
    if (runningWorkflowExecutions != null && runningWorkflowExecutions.size() > 0) {
      throw new WingsException("Orchestration has already been triggered");
    }
    // TODO - validate list of artifact Ids if it's matching for all the services involved in this orchestration

    StateMachine stateMachine = readLatest(appId, orchestrationId, null);
    if (stateMachine == null) {
      throw new WingsException("No stateMachine associated with " + orchestrationId);
    }

    Orchestration orchestration = wingsPersistence.get(Orchestration.class, appId, orchestrationId);

    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setAppId(appId);
    workflowExecution.setEnvId(envId);
    workflowExecution.setWorkflowId(orchestrationId);
    workflowExecution.setName(orchestration.getName());
    workflowExecution.setWorkflowType(WorkflowType.ORCHESTRATION);
    workflowExecution.setStateMachineId(stateMachine.getUuid());
    workflowExecution.setTotal(1);
    workflowExecution.getBreakdown().setInprogress(1);
    workflowExecution.setExecutionArgs(executionArgs);

    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);
    stdParams.setEnvId(envId);
    if (executionArgs.getArtifacts() != null) {
      stdParams.setArtifactIds(
          executionArgs.getArtifacts().stream().map(Artifact::getUuid).collect(Collectors.toList()));
    }
    stdParams.setExecutionCredential(executionArgs.getExecutionCredential());

    return triggerExecution(workflowExecution, stateMachine, workflowExecutionUpdate, stdParams);
  }

  private WorkflowExecution triggerExecution(WorkflowExecution workflowExecution, StateMachine stateMachine,
      WorkflowExecutionUpdate workflowExecutionUpdate, ContextElement... contextElements) {
    if (workflowExecution.getExecutionArgs() != null) {
      if (workflowExecution.getExecutionArgs().getServiceInstances() != null) {
        List<String> serviceInstanceIds = workflowExecution.getExecutionArgs()
                                              .getServiceInstances()
                                              .stream()
                                              .map(ServiceInstance::getUuid)
                                              .collect(Collectors.toList());
        PageRequest<ServiceInstance> pageRequest = PageRequest.Builder.aPageRequest()
                                                       .addFilter("appId", Operator.EQ, workflowExecution.getAppId())
                                                       .addFilter("uuid", Operator.IN, serviceInstanceIds.toArray())
                                                       .build();
        List<ServiceInstance> serviceInstances = serviceInstanceService.list(pageRequest).getResponse();

        if (serviceInstances == null || serviceInstances.size() != serviceInstanceIds.size()) {
          logger.error("Service instances argument and valid service instance retrieved size not matching");
          throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Invalid service instances");
        }
        workflowExecution.getExecutionArgs().setServiceInstanceIdNames(serviceInstances.stream().collect(
            Collectors.toMap(ServiceInstance::getUuid, ServiceInstance::getDisplayName)));
      }

      if (workflowExecution.getExecutionArgs().getArtifacts() != null) {
        List<String> artifactIds = workflowExecution.getExecutionArgs()
                                       .getArtifacts()
                                       .stream()
                                       .map(Artifact::getUuid)
                                       .collect(Collectors.toList());
        PageRequest<Artifact> pageRequest = PageRequest.Builder.aPageRequest()
                                                .addFilter("appId", Operator.EQ, workflowExecution.getAppId())
                                                .addFilter("uuid", Operator.IN, artifactIds.toArray())
                                                .build();
        List<Artifact> artifacts = artifactService.list(pageRequest).getResponse();

        if (artifacts == null || artifacts.size() != artifactIds.size()) {
          logger.error("Artifact argument and valid artifact retrieved size not matching");
          throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Invalid artifact");
        }
        workflowExecution.getExecutionArgs().setArtifactIdNames(
            artifacts.stream().collect(Collectors.toMap(Artifact::getUuid, Artifact::getDisplayName)));
      }
    }

    String workflowExecutionId = wingsPersistence.save(workflowExecution);
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(workflowExecution.getAppId());
    stateExecutionInstance.setExecutionUuid(workflowExecutionId);
    if (workflowExecutionUpdate == null) {
      workflowExecutionUpdate = new WorkflowExecutionUpdate();
    }
    workflowExecutionUpdate.setAppId(workflowExecution.getAppId());
    workflowExecutionUpdate.setWorkflowExecutionId(workflowExecutionId);
    stateExecutionInstance.setCallback(workflowExecutionUpdate);

    WingsDeque<ContextElement> elements = new WingsDeque<>();
    if (contextElements != null) {
      for (ContextElement contextElement : contextElements) {
        elements.push(contextElement);
      }
    }
    stateExecutionInstance.setContextElements(elements);
    stateMachineExecutor.execute(stateMachine, stateExecutionInstance);

    // TODO: findAndModify
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .field("appId")
                                         .equal(workflowExecution.getAppId())
                                         .field(ID_KEY)
                                         .equal(workflowExecutionId)
                                         .field("status")
                                         .equal(ExecutionStatus.NEW);
    UpdateOperations<WorkflowExecution> updateOps =
        wingsPersistence.createUpdateOperations(WorkflowExecution.class).set("status", ExecutionStatus.RUNNING);

    wingsPersistence.update(query, updateOps);

    notifyWorkflowExecution(workflowExecution.getAppId(), workflowExecutionId);
    return wingsPersistence.get(WorkflowExecution.class, workflowExecution.getAppId(), workflowExecutionId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution triggerEnvExecution(String appId, String envId, ExecutionArgs executionArgs) {
    return triggerEnvExecution(appId, envId, executionArgs, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Workflow> void deleteWorkflow(Class<T> cls, String appId, String workflowId) {
    UpdateOperations<T> ops = wingsPersistence.createUpdateOperations(cls);
    ops.set("active", false);
    wingsPersistence.update(
        wingsPersistence.createQuery(cls).field("appId").equal(appId).field(ID_KEY).equal(workflowId), ops);
  }

  @Override
  public void incrementInProgressCount(String appId, String workflowExecutionId, int inc) {
    UpdateOperations<WorkflowExecution> ops = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    ops.inc("breakdown.inprogress", inc);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .field("appId")
                                .equal(appId)
                                .field(ID_KEY)
                                .equal(workflowExecutionId),
        ops);
  }

  @Override
  public void incrementSuccess(String appId, String workflowExecutionId, int inc) {
    UpdateOperations<WorkflowExecution> ops = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    ops.inc("breakdown.success", inc);
    ops.inc("breakdown.inprogress", -1 * inc);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .field("appId")
                                .equal(appId)
                                .field(ID_KEY)
                                .equal(workflowExecutionId),
        ops);
  }

  @Override
  public void incrementFailed(String appId, String workflowExecutionId, Integer inc) {
    UpdateOperations<WorkflowExecution> ops = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    ops.inc("breakdown.failed", inc);
    ops.inc("breakdown.inprogress", -1 * inc);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .field("appId")
                                .equal(appId)
                                .field(ID_KEY)
                                .equal(workflowExecutionId),
        ops);
  }

  /**
   * Sets static configuration.
   *
   * @param staticConfiguration the static configuration
   */
  public void setStaticConfiguration(StaticConfiguration staticConfiguration) {
    this.staticConfiguration = staticConfiguration;
  }

  /**
   * Trigger env execution workflow execution.
   *
   * @param appId                   the app id
   * @param envId                   the env id
   * @param executionArgs           the execution args
   * @param workflowExecutionUpdate the workflow execution update
   * @return the workflow execution
   */
  WorkflowExecution triggerEnvExecution(
      String appId, String envId, ExecutionArgs executionArgs, WorkflowExecutionUpdate workflowExecutionUpdate) {
    if (executionArgs.getWorkflowType() == WorkflowType.ORCHESTRATION) {
      logger.debug("Received an orchestrated execution request");
      if (executionArgs.getOrchestrationId() == null) {
        logger.error("orchestrationId is null for an orchestrated execution");
        throw new WingsException(
            ErrorCodes.INVALID_REQUEST, "message", "orchestrationId is null for an orchestrated execution");
      }
      return triggerOrchestrationExecution(appId, envId, executionArgs.getOrchestrationId(), executionArgs);
    } else if (executionArgs.getWorkflowType() == WorkflowType.SIMPLE) {
      logger.debug("Received an simple execution request");
      if (executionArgs.getServiceId() == null) {
        logger.error("serviceId is null for a simple execution");
        throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "serviceId is null for a simple execution");
      }
      if (executionArgs.getServiceInstances() == null || executionArgs.getServiceInstances().size() == 0) {
        logger.error("serviceInstances are empty for a simple execution");
        throw new WingsException(
            ErrorCodes.INVALID_REQUEST, "message", "serviceInstances are empty for a simple execution");
      }

      return triggerSimpleExecution(appId, envId, executionArgs, workflowExecutionUpdate);

    } else {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args", "workflowType");
    }
  }

  /**
   * Trigger simple execution workflow execution.
   *
   * @param appId         the app id
   * @param envId         the env id
   * @param executionArgs the execution args
   * @return the workflow execution
   */
  private WorkflowExecution triggerSimpleExecution(
      String appId, String envId, ExecutionArgs executionArgs, WorkflowExecutionUpdate workflowExecutionUpdate) {
    Workflow workflow = readLatestSimpleWorkflow(appId, envId);
    String orchestrationId = workflow.getUuid();

    StateMachine stateMachine = readLatest(appId, orchestrationId, null);
    if (stateMachine == null) {
      throw new WingsException("No stateMachine associated with " + orchestrationId);
    }

    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setAppId(appId);
    workflowExecution.setEnvId(envId);
    workflowExecution.setWorkflowType(WorkflowType.SIMPLE);
    workflowExecution.setStateMachineId(stateMachine.getUuid());
    workflowExecution.setTotal(executionArgs.getServiceInstances().size());
    workflowExecution.setName(workflow.getName());
    workflowExecution.setWorkflowId(workflow.getUuid());
    workflowExecution.setExecutionArgs(executionArgs);

    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);
    stdParams.setEnvId(envId);
    if (executionArgs.getArtifacts() != null) {
      stdParams.setArtifactIds(
          executionArgs.getArtifacts().stream().map(Artifact::getUuid).collect(Collectors.toList()));
    }
    stdParams.setExecutionCredential(executionArgs.getExecutionCredential());

    SimpleWorkflowParam simpleOrchestrationParams = new SimpleWorkflowParam();
    simpleOrchestrationParams.setServiceId(executionArgs.getServiceId());
    if (executionArgs.getServiceInstances() != null) {
      simpleOrchestrationParams.setInstanceIds(
          executionArgs.getServiceInstances().stream().map(ServiceInstance::getUuid).collect(Collectors.toList()));
    }
    simpleOrchestrationParams.setExecutionStrategy(executionArgs.getExecutionStrategy());
    simpleOrchestrationParams.setCommandName(executionArgs.getCommandName());
    return triggerExecution(
        workflowExecution, stateMachine, workflowExecutionUpdate, stdParams, simpleOrchestrationParams);
  }

  /**
   * Read latest simple workflow orchestration.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the orchestration
   */
  Orchestration readLatestSimpleWorkflow(String appId, String envId) {
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
      workflow = createDefaultSimpleWorkflow(appId, envId);
    }

    return workflow;
  }

  private Orchestration createDefaultSimpleWorkflow(String appId, String envId) {
    Orchestration orchestration = new Orchestration();
    orchestration.setName(Constants.SIMPLE_ORCHESTRATION_NAME);
    orchestration.setDescription(Constants.SIMPLE_ORCHESTRATION_DESC);
    orchestration.setWorkflowType(WorkflowType.SIMPLE);
    orchestration.setAppId(appId);
    orchestration.setEnvironment(environmentService.get(appId, envId, false));

    Graph graph = staticConfiguration.defaultSimpleWorkflow();
    orchestration.setGraph(graph);

    return createWorkflow(Orchestration.class, orchestration);
  }

  private List<WorkflowExecution> getRunningWorkflowExecutions(
      WorkflowType workflowType, String appId, String workflowId) {
    PageRequest<WorkflowExecution> pageRequest = new PageRequest<>();

    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    filter.setFieldValues(appId);
    filter.setOp(Operator.EQ);
    pageRequest.addFilter(filter);

    filter = new SearchFilter();
    filter.setFieldName("workflowId");
    filter.setFieldValues(workflowId);
    filter.setOp(Operator.EQ);
    pageRequest.addFilter(filter);

    filter = new SearchFilter();
    filter.setFieldName("workflowType");
    filter.setFieldValues(workflowType);
    filter.setOp(Operator.EQ);
    pageRequest.addFilter(filter);

    filter = new SearchFilter();
    filter.setFieldName("status");
    List<Object> statuses = new ArrayList<>();
    statuses.add(ExecutionStatus.NEW);
    statuses.add(ExecutionStatus.RUNNING);
    filter.setFieldValues(statuses);
    filter.setOp(Operator.IN);
    pageRequest.addFilter(filter);

    PageResponse<WorkflowExecution> pageResponse = wingsPersistence.query(WorkflowExecution.class, pageRequest);
    if (pageResponse == null) {
      return null;
    }
    return pageResponse.getResponse();
  }

  @Override
  public ExecutionEvent triggerExecutionEvent(ExecutionEvent executionEvent) {
    String executionUuid = executionEvent.getExecutionUuid();
    WorkflowExecution workflowExecution =
        wingsPersistence.get(WorkflowExecution.class, executionEvent.getAppId(), executionUuid);
    if (workflowExecution == null) {
      throw new WingsException(
          ErrorCodes.INVALID_ARGUMENT, "args", "no workflowExecution for executionUuid:" + executionUuid);
    }

    return stateMachineExecutionEventManager.registerExecutionEvent(executionEvent);
  }

  @Override
  public RequiredExecutionArgs getRequiredExecutionArgs(String appId, String envId, ExecutionArgs executionArgs) {
    if (executionArgs.getWorkflowType() == null) {
      logger.error("workflowType is null");
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "workflowType is null");
    }

    if (executionArgs.getWorkflowType() == WorkflowType.ORCHESTRATION) {
      logger.debug("Received an orchestrated execution request");
      if (executionArgs.getOrchestrationId() == null) {
        logger.error("orchestrationId is null for an orchestrated execution");
        throw new WingsException(
            ErrorCodes.INVALID_REQUEST, "message", "orchestrationId is null for an orchestrated execution");
      }

      Orchestration orchestration =
          wingsPersistence.get(Orchestration.class, appId, executionArgs.getOrchestrationId());
      if (orchestration == null) {
        logger.error("Invalid orchestrationId");
        throw new WingsException(
            ErrorCodes.INVALID_REQUEST, "message", "Invalid orchestrationId: " + executionArgs.getOrchestrationId());
      }

      StateMachine stateMachine = readLatest(appId, executionArgs.getOrchestrationId(), null);
      if (stateMachine == null) {
        throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Associated state machine not found");
      }
      return stateMachineExecutionSimulator.getRequiredExecutionArgs(appId, envId, stateMachine, executionArgs);

    } else if (executionArgs.getWorkflowType() == WorkflowType.SIMPLE) {
      logger.debug("Received an simple execution request");
      if (executionArgs.getServiceId() == null) {
        logger.error("serviceId is null for a simple execution");
        throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "serviceId is null for a simple execution");
      }
      if (executionArgs.getServiceInstances() == null || executionArgs.getServiceInstances().size() == 0) {
        logger.error("serviceInstances are empty for a simple execution");
        throw new WingsException(
            ErrorCodes.INVALID_REQUEST, "message", "serviceInstances are empty for a simple execution");
      }
      if (executionArgs.getCommandName() == null) {
        logger.error("commandName is null for a simple execution");
        throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "commandName is null for a simple execution");
      }
      Command command =
          serviceResourceService.getCommandByName(appId, executionArgs.getServiceId(), executionArgs.getCommandName());

      RequiredExecutionArgs requiredExecutionArgs = new RequiredExecutionArgs();
      if (command.isArtifactNeeded()) {
        requiredExecutionArgs.getEntityTypes().add(EntityType.ARTIFACT);
      }
      requiredExecutionArgs.getEntityTypes().add(EntityType.SSH_USER);
      requiredExecutionArgs.getEntityTypes().add(EntityType.SSH_PASSWORD);
      return requiredExecutionArgs;
    }

    return null;
  }

  private void notifyWorkflowExecution(String appId, String workflowExecutionId) {
    executorService.execute(new WorkflowExecutionEntryMaker(appId, workflowExecutionId));
  }

  private class WorkflowExecutionEntryMaker implements Runnable {
    private String workflowExecutionId;
    private String appId;

    /**
     * Instantiates a new Workflow execution entry maker.
     *
     * @param appId               the app id
     * @param workflowExecutionId the workflow execution id
     */
    public WorkflowExecutionEntryMaker(String appId, String workflowExecutionId) {
      this.appId = appId;
      this.workflowExecutionId = workflowExecutionId;
    }

    @Override
    public void run() {
      WorkflowExecution workflowExecution = getExecutionDetails(appId, workflowExecutionId);
      EntityType entityType = EntityType.ORCHESTRATED_DEPLOYMENT;
      if (workflowExecution.getWorkflowType() == WorkflowType.SIMPLE) {
        entityType = EntityType.SIMPLE_DEPLOYMENT;
      }
      History history = History.Builder.aHistory()
                            .withAppId(appId)
                            .withEventType(EventType.CREATED)
                            .withEntityType(entityType)
                            .withEntityId(workflowExecution.getUuid())
                            .withEntityName(workflowExecution.getName())
                            .withEntityNewValue(workflowExecution)
                            .withShortDescription(workflowExecution.getName() + " started")
                            .withTitle(workflowExecution.getName() + " started")
                            .build();
      historyService.create(history);
    }
  }
}
