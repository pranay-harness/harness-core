/**
 *
 */

package software.wings.service.impl;

import static java.util.stream.Collectors.toMap;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCodes.INVALID_PIPELINE;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.MongoHelper.setUnset;

import com.google.inject.Singleton;

import org.apache.commons.jexl3.JxltEngine.Exception;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.app.StaticConfiguration;
import software.wings.beans.Graph;
import software.wings.beans.Orchestration;
import software.wings.beans.Pipeline;
import software.wings.beans.ReadPref;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
  @Inject private EnvironmentService environmentService;
  @Inject private WorkflowExecutionService workflowExecutionService;

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
  public <T extends Workflow> boolean deleteWorkflow(Class<T> cls, String appId, String workflowId) {
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
   * @param envId the env id
   * @return the orchestration
   */
  @Override
  public Orchestration readLatestSimpleWorkflow(String appId, String envId) {
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
    return listOrchestration(pageRequest)
        .stream()
        .collect(toMap(Orchestration::getUuid, o -> (o.getEnvironment().getName() + ":" + o.getName())));
  }
}
