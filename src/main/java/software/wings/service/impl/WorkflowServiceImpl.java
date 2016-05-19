/**
 *
 */

package software.wings.service.impl;

import com.google.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
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
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStandardParams;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.Valid;
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
  private List<StateTypeDescriptor> cachedStencils;
  private Map<String, StateTypeDescriptor> cachedStencilMap;

  @Override
  public StateMachine create(@Valid StateMachine stateMachine) {
    stateMachine.validate();
    return wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
  }

  @Override
  public StateMachine read(String smId) {
    return wingsPersistence.get(StateMachine.class, smId);
  }

  @Override
  public PageResponse<StateMachine> list(PageRequest<StateMachine> req) {
    return wingsPersistence.query(StateMachine.class, req);
  }

  @Override
  public void trigger(String smId) {
    stateMachineExecutor.execute(smId, new ExecutionStandardParams());
  }

  @Override
  public List<StateTypeDescriptor> stencils() {
    if (cachedStencils != null) {
      return cachedStencils;
    }

    List<StateTypeDescriptor> stencils = new ArrayList<StateTypeDescriptor>();
    stencils.addAll(Arrays.asList(StateType.values()));

    List<StateTypeDescriptor> plugins = pluginManager.getExtensions(StateTypeDescriptor.class);
    stencils.addAll(plugins);

    Map<String, StateTypeDescriptor> stencilDescMap = new HashMap<>();
    for (StateTypeDescriptor sd : stencils) {
      if (stencilDescMap.get(sd.getType()) != null) {
        // already present for the type
        logger.error("Duplicate implementation for the stencil: {}", sd.getType());
        throw new WingsException("Duplicate implementation for the stencil: " + sd.getType());
      }
      stencilDescMap.put(sd.getType(), sd);
    }

    this.cachedStencils = stencils;
    this.cachedStencilMap = stencilDescMap;

    return stencils;
  }

  private Map<String, StateTypeDescriptor> stencilMap() {
    if (cachedStencilMap == null) {
      stencils();
    }
    return cachedStencilMap;
  }

  @Override
  public PageResponse<Pipeline> listPipelines(PageRequest<Pipeline> req) {
    return wingsPersistence.query(Pipeline.class, req);
  }

  @Override
  public <T extends Workflow> T createWorkflow(Class<T> cls, T workflow) {
    Graph graph = workflow.getGraph();
    workflow = wingsPersistence.saveAndGet(cls, workflow);
    StateMachine stateMachine = new StateMachine(workflow.getUuid(), graph, stencilMap());
    stateMachine = wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
    workflow.setGraph(stateMachine.getGraph());
    return workflow;
  }

  @Override
  public <T extends Workflow> T updateWorkflow(Class<T> cls, T workflow) {
    Graph graph = workflow.getGraph();
    StateMachine stateMachine = new StateMachine(workflow.getUuid(), graph, stencilMap());
    stateMachine = wingsPersistence.saveAndGet(StateMachine.class, stateMachine);
    workflow.setGraph(stateMachine.getGraph());
    return workflow;
  }

  @Override
  public Pipeline readPipeline(String appId, String pipelineId) {
    return wingsPersistence.get(Pipeline.class, appId, pipelineId);
  }

  @Override
  public StateMachine readLatest(String originId, String name) {
    if (StringUtils.isBlank(name)) {
      name = Constants.DEFAULT_WORKFLOW_NAME;
    }

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValue(originId);
    filter.setOp(Operator.EQ);
    req.getFilters().add(filter);

    filter = new SearchFilter();
    filter.setFieldName("name");
    filter.setFieldValue(name);
    filter.setOp(Operator.EQ);
    req.getFilters().add(filter);

    SortOrder order = new SortOrder();
    order.setFieldName("lastUpdatedAt");
    order.setOrderType(OrderType.DESC);
    req.getOrders().add(order);

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
  public Orchestration readOrchestration(String appId, String envId, String orchestrationId) {
    return wingsPersistence.createQuery(Orchestration.class)
        .field("appId")
        .equal(appId)
        .field("uuid")
        .equal(orchestrationId)
        .get();
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
    filter.setFieldValue(workflowExecution.getAppId());
    filter.setOp(Operator.EQ);
    pageRequest.getFilters().add(filter);

    filter = new SearchFilter();
    filter.setFieldName("workflowExecutionId");
    filter.setFieldValue(workflowExecution.getUuid());
    filter.setOp(Operator.EQ);
    pageRequest.getFilters().add(filter);

    SortOrder order = new SortOrder();
    order.setFieldName("createdAt");
    order.setOrderType(OrderType.ASC);
    pageRequest.getOrders().add(order);

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
    List<WorkflowExecution> runningWorkflowExecutions =
        getRunningWorkflowExecutions(WorkflowExecutionType.PIPELINE, appId, pipelineId);
    if (runningWorkflowExecutions != null) {
      for (WorkflowExecution workflowExecution : runningWorkflowExecutions) {
        if (workflowExecution.getStatus() == ExecutionStatus.NEW) {
          throw new WingsException("Pipeline already been triggered");
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

    ExecutionStandardParams stdParams = new ExecutionStandardParams();
    stdParams.setAppId(appId);

    return triggerExecution(workflowExecution, stateMachine, stdParams);
  }

  @Override
  public WorkflowExecution triggerOrchestrationExecution(
      String appId, String orchestrationId, List<String> artifactIds) {
    List<WorkflowExecution> runningWorkflowExecutions =
        getRunningWorkflowExecutions(WorkflowExecutionType.ORCHESTRATION, appId, orchestrationId);
    if (runningWorkflowExecutions != null && runningWorkflowExecutions.size() > 0) {
      for (WorkflowExecution workflowExecution : runningWorkflowExecutions) {
        throw new WingsException("Orchestration has already been triggered");
      }
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

    ExecutionStandardParams stdParams = new ExecutionStandardParams();
    stdParams.setAppId(appId);
    stdParams.setArtifactIds(artifactIds);

    return triggerExecution(workflowExecution, stateMachine, stdParams);
  }

  private WorkflowExecution triggerExecution(
      WorkflowExecution workflowExecution, StateMachine stateMachine, ExecutionStandardParams stdParams) {
    String workflowExecutionId = wingsPersistence.save(workflowExecution);
    stdParams.setWorkflowExecutionId(workflowExecutionId);
    stdParams.setCallback(new WorkflowExecutionUpdate(workflowExecution.getAppId(), workflowExecutionId));
    stateMachineExecutor.execute(stateMachine, stdParams);

    // TODO: findAndModify
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .field("appId")
                                         .equal(workflowExecution.getAppId())
                                         .field("uuid")
                                         .equal(workflowExecutionId)
                                         .field("status")
                                         .equal(ExecutionStatus.NEW);
    UpdateOperations<WorkflowExecution> updateOps =
        wingsPersistence.createUpdateOperations(WorkflowExecution.class).set("status", ExecutionStatus.RUNNING);

    wingsPersistence.update(query, updateOps);

    return wingsPersistence.get(WorkflowExecution.class, workflowExecution.getAppId(), workflowExecutionId);
  }

  private List<WorkflowExecution> getRunningWorkflowExecutions(
      WorkflowExecutionType workflowExecutionType, String appId, String workflowId) {
    PageRequest<WorkflowExecution> pageRequest = new PageRequest<>();

    SearchFilter filter = new SearchFilter();
    filter.setFieldName("appId");
    filter.setFieldValue(appId);
    filter.setOp(Operator.EQ);
    pageRequest.getFilters().add(filter);

    filter = new SearchFilter();
    filter.setFieldName("workflowId");
    filter.setFieldValue(workflowId);
    filter.setOp(Operator.EQ);
    pageRequest.getFilters().add(filter);

    filter = new SearchFilter();
    filter.setFieldName("workflowExecutionType");
    filter.setFieldValue(workflowExecutionType);
    filter.setOp(Operator.EQ);
    pageRequest.getFilters().add(filter);

    filter = new SearchFilter();
    filter.setFieldName("status");
    List<Object> statuses = new ArrayList<>();
    statuses.add(ExecutionStatus.NEW);
    statuses.add(ExecutionStatus.RUNNING);
    filter.setFieldValues(statuses);
    filter.setOp(Operator.IN);
    pageRequest.getFilters().add(filter);

    PageResponse<WorkflowExecution> pageResponse = wingsPersistence.query(WorkflowExecution.class, pageRequest);
    if (pageResponse == null) {
      return null;
    }
    return pageResponse.getResponse();
  }
}
