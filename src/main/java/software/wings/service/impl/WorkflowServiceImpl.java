/**
 *
 */

package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCodes.INVALID_PIPELINE;
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
import software.wings.beans.ErrorCodes;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Link;
import software.wings.beans.Graph.Node;
import software.wings.beans.Orchestration;
import software.wings.beans.Pipeline;
import software.wings.beans.ReadPref;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsDeque;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionCallback;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.sm.TransitionType;
import software.wings.sm.WorkflowStandardParams;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * The Class WorkflowServiceImpl.
 *
 * @author Rishi
 */
@Singleton
@ValidateOnExecution
public class WorkflowServiceImpl implements WorkflowService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;
  @Inject private StateMachineExecutor stateMachineExecutor;
  @Inject private PluginManager pluginManager;
  @Inject private EnvironmentService environmentService;
  @Inject private StaticConfiguration staticConfiguration;

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
  public Map<StateTypeScope, List<StateTypeDescriptor>> stencils(StateTypeScope... stateTypeScopes) {
    Map<StateTypeScope, List<StateTypeDescriptor>> mapByScope = loadStateTypes();

    if (ArrayUtils.isEmpty(stateTypeScopes)) {
      return new HashMap<>(mapByScope);
    } else {
      Map<StateTypeScope, List<StateTypeDescriptor>> maps = new HashMap<>();
      for (StateTypeScope scope : stateTypeScopes) {
        maps.put(scope, mapByScope.get(scope));
      }
      return maps;
    }
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
      pageRequest.addFilter(SearchFilter.Builder.aSearchFilter()
                                .withField("workflowType", Operator.EQ, WorkflowType.ORCHESTRATION)
                                .build());
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
      populateGraph(workflowExecution, null);
    }
    return res;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution getExecutionDetails(String appId, String workflowExecutionId) {
    return getExecutionDetails(appId, workflowExecutionId, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution getExecutionDetails(
      String appId, String workflowExecutionId, List<String> expandedGroupIds) {
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionId);
    if (workflowExecution != null) {
      populateGraph(workflowExecution, expandedGroupIds);
    }
    return workflowExecution;
  }

  private void populateGraph(WorkflowExecution workflowExecution, List<String> expandedGroupIds) {
    StateMachine sm =
        wingsPersistence.get(StateMachine.class, workflowExecution.getAppId(), workflowExecution.getStateMachineId());

    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .field("appId")
                                              .equal(workflowExecution.getAppId())
                                              .field("executionUuid")
                                              .equal(workflowExecution.getUuid());
    if (expandedGroupIds == null || expandedGroupIds.isEmpty()) {
      query.field("parentInstanceId").doesNotExist();
    } else {
      query.or(
          query.criteria("parentInstanceId").doesNotExist(), query.criteria("parentInstanceId").in(expandedGroupIds));
    }
    query.order("createdAt");

    List<StateExecutionInstance> res = wingsPersistence.executeGetListQuery(query);

    if (res != null && res.size() > 0) {
      if (res.size() == 1
          && (StateType.REPEAT.name().equals(res.get(0).getStateType())
                 || StateType.FORK.name().equals(res.get(0).getStateType()))) {
        expandedGroupIds = Lists.newArrayList(res.get(0).getUuid());
        populateGraph(workflowExecution, expandedGroupIds);
        return;
      }
    }
    workflowExecution.setGraph(generateGraph(res, sm.getInitialStateName(), expandedGroupIds));
  }

  private Graph generateGraph(
      List<StateExecutionInstance> response, String originState, List<String> expandedGroupIds) {
    if (response == null || response.size() == 0) {
      return null;
    }

    String originNodeId = null;
    Graph graph = new Graph();
    List<Node> nodes = new ArrayList<>();
    List<Link> links = new ArrayList<>();
    List<String> collapsedInstanceIds = new ArrayList<>();
    for (StateExecutionInstance instance : response) {
      Node node = new Node();
      node.setId(instance.getUuid());
      node.setName(instance.getStateName());
      node.setType(instance.getStateType());
      node.setStatus(String.valueOf(instance.getStatus()).toLowerCase());
      if (instance.getStateExecutionData() != null) {
        node.setExecutionSummary(instance.getStateExecutionData().getExecutionSummary());
        node.setExecutionDetails(instance.getStateExecutionData().getExecutionDetails());
      }
      if ((StateType.REPEAT.name().equals(instance.getStateType())
              || StateType.FORK.name().equals(instance.getStateType()))
          && (expandedGroupIds == null || !expandedGroupIds.contains(instance.getUuid()))) {
        node.setExpanded(false);
        collapsedInstanceIds.add(instance.getUuid());
      } else {
        node.setExpanded(true);
      }

      nodes.add(node);
      if (node.getName().equals(originState)) {
        originNodeId = node.getId();
      }

      Link link = new Link();
      link.setTo(instance.getUuid());

      String fromInstanceId = null;
      if (instance.getParentInstanceId() != null) {
        // This is scenario like fork, repeat or sub workflow
        node = new Node();
        node.setId(UUIDGenerator.getUuid());
        node.setName(instance.getContextElementName());
        node.setType("element");
        node.setStatus(String.valueOf(instance.getStatus()).toLowerCase());
        nodes.add(node);

        links.add(Link.Builder.aLink()
                      .withId(instance.getParentInstanceId() + "-" + node.getId())
                      .withFrom(instance.getParentInstanceId())
                      .withTo(node.getId())
                      .withType(TransitionType.REPEAT.name().toLowerCase())
                      .build());

        fromInstanceId = node.getId();

      } else if (instance.getPrevInstanceId() != null) {
        fromInstanceId = instance.getPrevInstanceId();
      }
      // TODO : additional link needed for serial repeat

      if (fromInstanceId != null) {
        link.setId(fromInstanceId + "-" + instance.getUuid());
        link.setFrom(fromInstanceId);
        link.setType(node.getStatus());
        links.add(link);
      }
    }
    graph.setNodes(nodes);
    graph.setLinks(links);
    Map<String, Node> nodesMap = graph.getNodesMap();
    links.forEach(link -> {
      if (link.getType() == null) {
        link.setType(nodesMap.get(link.getFrom()).getStatus());
      }
    });
    graph.repaint(originNodeId);
    return graph;
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
    String name = "";
    if (orchestration.getName() != null) {
      name = orchestration.getName() + " ";
    }
    try {
      name += new SimpleDateFormat("MM/dd/yyyy hh:mm aa").format(new Date());
    } catch (Exception e) {
      throw new WingsException("Error in date formatting");
    }
    workflowExecution.setName(name);
    workflowExecution.setWorkflowType(WorkflowType.ORCHESTRATION);
    workflowExecution.setStateMachineId(stateMachine.getUuid());

    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);
    stdParams.setEnvId(envId);
    stdParams.setArtifactIds(executionArgs.getArtifactIds());
    stdParams.setExecutionCredential(executionArgs.getExecutionCredential());

    return triggerExecution(workflowExecution, stateMachine, workflowExecutionUpdate, stdParams);
  }

  private WorkflowExecution triggerExecution(WorkflowExecution workflowExecution, StateMachine stateMachine,
      WorkflowExecutionUpdate workflowExecutionUpdate, ContextElement... contextElements) {
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
      logger.info("Received an orchestrated execution request");
      if (executionArgs.getOrchestrationId() == null) {
        logger.error("orchestrationId is null for an orchestrated execution");
        throw new WingsException(
            ErrorCodes.INVALID_REQUEST, "message", "orchestrationId is null for an orchestrated execution");
      }
      return triggerOrchestrationExecution(appId, envId, executionArgs.getOrchestrationId(), executionArgs);
    } else if (executionArgs.getWorkflowType() == WorkflowType.SIMPLE) {
      logger.info("Received an simple execution request");
      if (executionArgs.getServiceId() == null) {
        logger.error("serviceId is null for a simple execution");
        throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "serviceId is null for a simple execution");
      }
      if (executionArgs.getServiceInstanceIds() == null || executionArgs.getServiceInstanceIds().size() == 0) {
        logger.error("serviceInstanceIds is empty for a simple execution");
        throw new WingsException(
            ErrorCodes.INVALID_REQUEST, "message", "serviceInstanceIds is empty for a simple execution");
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

    String name = "";
    if (workflow.getName() != null) {
      name = workflow.getName() + " ";
    }
    try {
      name += new SimpleDateFormat("MM/dd/yyyy hh:mm aa").format(new Date());
    } catch (Exception e) {
      throw new WingsException("Error in date formatting");
    }
    workflowExecution.setName(name);
    workflowExecution.setWorkflowId(workflow.getUuid());

    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);
    stdParams.setEnvId(envId);
    stdParams.setArtifactIds(executionArgs.getArtifactIds());
    stdParams.setExecutionCredential(executionArgs.getExecutionCredential());

    SimpleWorkflowParam simpleOrchestrationParams = new SimpleWorkflowParam();
    simpleOrchestrationParams.setServiceId(executionArgs.getServiceId());
    simpleOrchestrationParams.setInstanceIds(executionArgs.getServiceInstanceIds());
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
    orchestration.setEnvironment(environmentService.get(appId, envId));

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

  /**
   * Sets static configuration.
   *
   * @param staticConfiguration the static configuration
   */
  public void setStaticConfiguration(StaticConfiguration staticConfiguration) {
    this.staticConfiguration = staticConfiguration;
  }
}
