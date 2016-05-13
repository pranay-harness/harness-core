/**
 *
 */

package software.wings.service.impl;

import com.google.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.beans.Graph;
import software.wings.beans.Orchestration;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Pipeline;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.Workflow;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.WorkflowService;
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
    stateMachineExecutor.execute(smId);
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
  public Orchestration readOrchestration(String appId, String orchestrationId) {
    return wingsPersistence.get(Orchestration.class, appId, orchestrationId);
  }
}
