/**
 *
 */

package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.dl.MongoHelper.setUnset;

import com.google.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.api.SimpleOrchestrationParams;
import software.wings.beans.ErrorConstants;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionArgs.OrchestrationType;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Link;
import software.wings.beans.Graph.Node;
import software.wings.beans.Orchestration;
import software.wings.beans.Pipeline;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionType;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsDeque;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * @author Rishi
 */
@Singleton
@ValidateOnExecution
public class WorkflowServiceImpl implements WorkflowService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;
  @Inject private StateMachineExecutor stateMachineExecutor;
  @Inject private PluginManager pluginManager;
  private Map<StateTypeScope, List<StateTypeDescriptor>> cachedStencils;
  private Map<String, StateTypeDescriptor> cachedStencilMap;

  @Override
  public StateMachine create(StateMachine stateMachine) {
    stateMachine.validate();
    return wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
  }

  @Override
  public PageResponse<StateMachine> list(PageRequest<StateMachine> req) {
    return wingsPersistence.query(StateMachine.class, req);
  }

  @Override
  public void trigger(String appId, String stateMachineId, String executionUuid) {
    stateMachineExecutor.execute(appId, stateMachineId, executionUuid);
  }

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

  /**
   * @return
   */
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

  @Override
  public PageResponse<Pipeline> listPipelines(PageRequest<Pipeline> pageRequest) {
    return wingsPersistence.query(Pipeline.class, pageRequest);
  }

  @Override
  public <T extends Workflow> T createWorkflow(Class<T> cls, T workflow) {
    Graph graph = workflow.getGraph();
    workflow = wingsPersistence.saveAndGet(cls, workflow);
    if (graph != null) {
      StateMachine stateMachine = new StateMachine(workflow, graph, stencilMap());
      stateMachine = wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
      workflow.setGraph(stateMachine.getGraph());
    }
    return workflow;
  }

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

  @Override
  public Pipeline readPipeline(String appId, String pipelineId) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    StateMachine stateMachine = readLatest(pipelineId, null);
    if (stateMachine != null) {
      pipeline.setGraph(stateMachine.getGraph());
    }
    return pipeline;
  }

  @Override
  public StateMachine readLatest(String originId, String name) {
    if (StringUtils.isBlank(name)) {
      name = Constants.DEFAULT_WORKFLOW_NAME;
    }

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(originId);
    filter.setOp(Operator.EQ);
    req.addFilter(filter);

    filter = new SearchFilter();
    filter.setFieldName("name");
    filter.setFieldValues(name);
    filter.setOp(Operator.EQ);
    req.addFilter(filter);

    SortOrder order = new SortOrder();
    order.setFieldName("lastUpdatedAt");
    order.setOrderType(OrderType.DESC);
    req.addOrder(order);

    req.setLimit("1");

    PageResponse<StateMachine> res = list(req);
    if (res == null || res.size() == 0) {
      return null;
    }
    return res.get(0);
  }

  @Override
  public PageResponse<Orchestration> listOrchestration(PageRequest<Orchestration> pageRequest) {
    return wingsPersistence.query(Orchestration.class, pageRequest);
  }

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

  @Override
  public Orchestration readOrchestration(String appId, String envId, String orchestrationId) {
    Orchestration orchestration = wingsPersistence.get(Orchestration.class, appId, orchestrationId);

    if (orchestration == null) {
      return orchestration;
    }
    StateMachine stateMachine = readLatest(orchestrationId, null);

    orchestration.setGraph(stateMachine.getGraph());
    return orchestration;
  }

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
      populateGraph(workflowExecution);
    }
    return res;
  }

  @Override
  public WorkflowExecution getExecutionDetails(String appId, String workflowExecutionId) {
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionId);
    populateGraph(workflowExecution);
    return workflowExecution;
  }

  private void populateGraph(WorkflowExecution workflowExecution) {
    PageRequest<StateExecutionInstance> pageRequest = new PageRequest<>();

    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    filter.setFieldValues(workflowExecution.getAppId());
    filter.setOp(Operator.EQ);
    pageRequest.addFilter(filter);

    filter = new SearchFilter();
    filter.setFieldName("executionUuid");
    filter.setFieldValues(workflowExecution.getUuid());
    filter.setOp(Operator.EQ);
    pageRequest.addFilter(filter);

    SortOrder order = new SortOrder();
    order.setFieldName("createdAt");
    order.setOrderType(OrderType.ASC);
    pageRequest.addOrder(order);

    PageResponse<StateExecutionInstance> res = wingsPersistence.query(StateExecutionInstance.class, pageRequest);
    if (res != null && res.size() > 0) {
      workflowExecution.setGraph(generateGraph(res.getResponse()));
    }
  }

  private Graph generateGraph(List<StateExecutionInstance> response) {
    Graph graph = new Graph();
    List<Node> nodes = new ArrayList<>();
    List<Link> links = new ArrayList<>();
    for (StateExecutionInstance instance : response) {
      Node node = new Node();
      node.setId(instance.getUuid());
      node.setName(instance.getStateName());
      nodes.add(node);

      String fromInstanceId = null;
      if (instance.getPrevInstanceId() != null) {
        fromInstanceId = instance.getPrevInstanceId();
      } else if (instance.getParentInstanceId() != null) {
        // TODO: needs work for repeat element instance.
        // This is scenario like fork, repeat or sub waitnotify
        fromInstanceId = instance.getParentInstanceId();
      }
      if (fromInstanceId != null) {
        Link link = new Link();
        link.setId(fromInstanceId + "-" + instance.getUuid());
        link.setFrom(fromInstanceId);
        link.setTo(instance.getUuid());
        links.add(link);
      }
    }
    graph.setNodes(nodes);
    graph.setLinks(links);
    return graph;
  }

  @Override
  public WorkflowExecution triggerPipelineExecution(String appId, String pipelineId) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    if (pipeline == null) {
      throw new WingsException(ErrorConstants.NON_EXISTING_PIPELINE);
    }
    List<WorkflowExecution> runningWorkflowExecutions =
        getRunningWorkflowExecutions(WorkflowExecutionType.PIPELINE, appId, pipelineId);
    if (runningWorkflowExecutions != null) {
      for (WorkflowExecution workflowExecution : runningWorkflowExecutions) {
        if (workflowExecution.getStatus() == ExecutionStatus.NEW) {
          throw new WingsException(ErrorConstants.PIPELINE_ALREADY_TRIGGERED, "pilelineName", pipeline.getName());
        }
        if (workflowExecution.getStatus() == ExecutionStatus.RUNNING) {
          // Analyze if pipeline is in initial stage
        }
      }
    }

    StateMachine stateMachine = readLatest(pipelineId, null);
    if (stateMachine == null) {
      throw new WingsException("No stateMachine associated with " + pipelineId);
    }

    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setAppId(appId);
    workflowExecution.setWorkflowId(pipelineId);
    workflowExecution.setWorkflowExecutionType(WorkflowExecutionType.PIPELINE);
    workflowExecution.setStateMachineId(stateMachine.getUuid());

    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);

    return triggerExecution(workflowExecution, stateMachine, stdParams);
  }

  @Override
  public WorkflowExecution triggerOrchestrationExecution(
      String appId, String orchestrationId, ExecutionArgs executionArgs) {
    List<WorkflowExecution> runningWorkflowExecutions =
        getRunningWorkflowExecutions(WorkflowExecutionType.ORCHESTRATION, appId, orchestrationId);
    if (runningWorkflowExecutions != null && runningWorkflowExecutions.size() > 0) {
      throw new WingsException("Orchestration has already been triggered");
    }
    // TODO - validate list of artifact Ids if it's matching for all the services involved in this orchestration

    StateMachine stateMachine = readLatest(orchestrationId, null);
    if (stateMachine == null) {
      throw new WingsException("No stateMachine associated with " + orchestrationId);
    }

    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setAppId(appId);
    workflowExecution.setWorkflowId(orchestrationId);
    workflowExecution.setWorkflowExecutionType(WorkflowExecutionType.ORCHESTRATION);
    workflowExecution.setStateMachineId(stateMachine.getUuid());

    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);
    stdParams.setArtifactIds(executionArgs.getArtifactIds());
    stdParams.setExecutionCredential(executionArgs.getExecutionCredential());

    return triggerExecution(workflowExecution, stateMachine, stdParams);
  }

  private WorkflowExecution triggerExecution(
      WorkflowExecution workflowExecution, StateMachine stateMachine, ContextElement... contextElements) {
    String workflowExecutionId = wingsPersistence.save(workflowExecution);
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(workflowExecution.getAppId());
    stateExecutionInstance.setExecutionUuid(workflowExecutionId);
    stateExecutionInstance.setCallback(new WorkflowExecutionUpdate(workflowExecution.getAppId(), workflowExecutionId));

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

  @Override
  public WorkflowExecution triggerEnvExecution(String appId, String envId, ExecutionArgs executionArgs) {
    if (executionArgs.getOrchestrationType() == OrchestrationType.ORCHESTRATED) {
      logger.info("Received an orchestrated execution request");
      if (executionArgs.getOrchestrationId() == null) {
        logger.error("orchestrationId is null for an orchestrated execution");
        throw new WingsException(
            ErrorConstants.INVALID_REQUEST, "message", "orchestrationId is null for an orchestrated execution");
      }
      return triggerOrchestrationExecution(appId, executionArgs.getOrchestrationId(), executionArgs);
    } else {
      logger.info("Received an simple execution request");
      if (executionArgs.getServiceId() == null) {
        logger.error("serviceId is null for a simple execution");
        throw new WingsException(ErrorConstants.INVALID_REQUEST, "message", "serviceId is null for a simple execution");
      }
      if (executionArgs.getServiceInstanceIds() == null || executionArgs.getServiceInstanceIds().size() == 0) {
        logger.error("serviceInstanceIds is empty for a simple execution");
        throw new WingsException(
            ErrorConstants.INVALID_REQUEST, "message", "serviceInstanceIds is empty for a simple execution");
      }

      return triggerSimpleExecution(appId, envId, executionArgs);
    }
  }

  private WorkflowExecution triggerSimpleExecution(String appId, String envId, ExecutionArgs executionArgs) {
    Workflow workflow = readLatestSimpleWorkflow(appId, envId);
    String orchestrationId = workflow.getUuid();

    StateMachine stateMachine = readLatest(orchestrationId, null);
    if (stateMachine == null) {
      throw new WingsException("No stateMachine associated with " + orchestrationId);
    }

    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setAppId(appId);
    workflowExecution.setWorkflowExecutionType(WorkflowExecutionType.SIMPLE);
    workflowExecution.setStateMachineId(stateMachine.getUuid());

    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);
    stdParams.setArtifactIds(executionArgs.getArtifactIds());
    stdParams.setExecutionCredential(executionArgs.getExecutionCredential());

    SimpleOrchestrationParams simpleOrchestrationParams = new SimpleOrchestrationParams();
    simpleOrchestrationParams.setServiceId(executionArgs.getServiceId());
    simpleOrchestrationParams.setInstanceIds(executionArgs.getServiceInstanceIds());

    return triggerExecution(workflowExecution, stateMachine, stdParams, simpleOrchestrationParams);
  }

  private Workflow readLatestSimpleWorkflow(String appId, String envId) {
    return null;
  }

  private List<WorkflowExecution> getRunningWorkflowExecutions(
      WorkflowExecutionType workflowExecutionType, String appId, String workflowId) {
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
    filter.setFieldName("workflowExecutionType");
    filter.setFieldValues(workflowExecutionType);
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
  public <T extends Workflow> void deleteWorkflow(Class<T> cls, String appId, String workflowId) {
    UpdateOperations<T> ops = wingsPersistence.createUpdateOperations(cls);
    ops.set("active", false);
    wingsPersistence.update(
        wingsPersistence.createQuery(cls).field("appId").equal(appId).field(ID_KEY).equal(workflowId), ops);
  }
}
