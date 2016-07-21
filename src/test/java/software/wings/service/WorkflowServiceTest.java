package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.Orchestration.Builder.anOrchestration;
import static software.wings.beans.Pipeline.Builder.aPipeline;
import static software.wings.utils.WingsTestConstants.APP_ID;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.Graph;
import software.wings.beans.Orchestration;
import software.wings.beans.Pipeline;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.State;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineTest.StateSync;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeScope;
import software.wings.sm.Transition;
import software.wings.sm.TransitionType;
import software.wings.stencils.Stencil;
import software.wings.utils.JsonUtils;
import software.wings.waitnotify.NotifyEventListener;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * The Class WorkflowServiceTest.
 *
 * @author Rishi
 */
@Listeners(NotifyEventListener.class)
public class WorkflowServiceTest extends WingsBaseTest {
  private static String appId = UUIDGenerator.getUuid();
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WorkflowService workflowService;
  @Inject private WingsPersistence wingsPersistence;

  private Environment env;
  private List<Service> services;

  /**
   * Gets environment.
   *
   * @return the environment
   */
  public Environment getEnvironment() {
    if (env == null) {
      env = wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(appId).build());
    }
    return env;
  }

  /**
   * Gets services.
   *
   * @return the services
   */
  public List<Service> getServices() {
    if (services == null) {
      services = Lists.newArrayList(wingsPersistence.saveAndGet(Service.class,
                                        Service.Builder.aService().withAppId(appId).withName("catalog").build()),
          wingsPersistence.saveAndGet(
              Service.class, Service.Builder.aService().withAppId(appId).withName("content").build()));
    }
    return services;
  }

  /**
   * Should save and read.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldSaveAndRead() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateSync("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);
    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateC)
                         .build());

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    String smId = sm.getUuid();
    sm = wingsPersistence.get(StateMachine.class, smId);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();
    System.out.println("All Done!");
  }

  /**
   * Should create pipeline with graph.
   */
  @Test
  public void shouldCreatePipelineWithGraph() {
    createPipeline();
  }

  /**
   * Should list pipeline
   */
  @Test
  public void shouldListPipeline() {
    createPipelineNoGraph();
    createPipeline();
    createPipelineNoGraph();
    createPipeline();
    PageRequest<Pipeline> req = new PageRequest<>();
    req.addFilter(SearchFilter.Builder.aSearchFilter().withField("appId", Operator.EQ, appId).build());
    PageResponse<Pipeline> res = workflowService.listPipelines(req);
    assertThat(res).isNotNull();
    assertThat(res.getResponse()).isNotNull();
    assertThat(res.size()).isEqualTo(4);
  }

  /**
   * Should read pipeline
   */
  @Test
  public void shouldReadPipeline() {
    Pipeline pipeline = createPipelineNoGraph();
    Pipeline pipeline2 = workflowService.readPipeline(appId, pipeline.getUuid());
    assertThat(pipeline2).isNotNull();
    assertThat(pipeline2).isEqualToComparingFieldByField(pipeline);
  }

  /**
   * Should update pipeline with no graph.
   */
  @Test
  public void shouldUpdatePipelineWithNoGraph() {
    Pipeline pipeline = createPipelineNoGraph();
    Graph graph = createInitialGraph();
    pipeline.setGraph(graph);

    updatePipeline(pipeline, 1);
  }

  /**
   * Should update pipeline with graph.
   */
  @Test
  public void shouldUpdatePipelineWithGraph() {
    Pipeline pipeline = createPipeline();
    updatePipeline(pipeline, 2);
  }

  /**
   * Update pipeline.
   *
   * @param pipeline   the pipeline
   * @param graphCount the graph count
   */
  public void updatePipeline(Pipeline pipeline, int graphCount) {
    pipeline.setDescription("newDescription");
    pipeline.setName("pipeline2");

    List<Service> newServices = Lists.newArrayList(
        wingsPersistence.saveAndGet(Service.class, Service.Builder.aService().withAppId(appId).withName("ui").build()),
        wingsPersistence.saveAndGet(
            Service.class, Service.Builder.aService().withAppId(appId).withName("server").build()));
    pipeline.setServices(newServices);

    Graph graph = pipeline.getGraph();
    graph.getNodes().add(aNode()
                             .withId("n5")
                             .withName("PROD")
                             .withX(350)
                             .withY(50)
                             .addProperty("envId", "34567")
                             .withType(StateType.ENV_STATE.name())
                             .build());
    graph.getLinks().add(aLink().withId("l3").withFrom("n4").withTo("n5").withType("success").build());

    Pipeline updatedPipeline = workflowService.updatePipeline(pipeline);
    assertThat(updatedPipeline)
        .isNotNull()
        .extracting(Pipeline::getUuid, Pipeline::getName, Pipeline::getDescription, Pipeline::getServices)
        .containsExactly(pipeline.getUuid(), "pipeline2", "newDescription", newServices);

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(pipeline.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res).isNotNull().hasSize(graphCount);

    StateMachine sm = workflowService.readLatest(appId, updatedPipeline.getUuid(), null);
    assertThat(sm.getGraph()).isEqualTo(graph);
  }

  private Pipeline createPipeline() {
    Graph graph = createInitialGraph();
    Pipeline pipeline = aPipeline()
                            .withAppId(appId)
                            .withName("pipeline1")
                            .withDescription("Sample Pipeline")
                            .addServices("service1", "service2")
                            .withGraph(graph)
                            .build();

    pipeline = workflowService.createWorkflow(Pipeline.class, pipeline);
    assertThat(pipeline).isNotNull();
    assertThat(pipeline.getUuid()).isNotNull();

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(pipeline.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res)
        .isNotNull()
        .hasSize(1)
        .doesNotContainNull()
        .extracting(StateMachine::getGraph)
        .doesNotContainNull()
        .containsExactly(graph);
    return pipeline;
  }

  /**
   * @return
   */
  private Graph createInitialGraph() {
    return aGraph()
        .addNodes(aNode()
                      .withId("n1")
                      .withName("BUILD")
                      .withX(200)
                      .withY(50)
                      .withType(StateType.BUILD.name())
                      .withOrigin(true)
                      .build())
        .addNodes(aNode()
                      .withId("n2")
                      .withName("IT")
                      .withX(250)
                      .withY(50)
                      .withType(StateType.ENV_STATE.name())
                      .addProperty("envId", "12345")
                      .build())
        .addNodes(aNode()
                      .withId("n3")
                      .withName("QA")
                      .withX(300)
                      .withY(50)
                      .withType(StateType.ENV_STATE.name())
                      .addProperty("envId", "23456")
                      .build())
        .addNodes(aNode()
                      .withId("n4")
                      .withName("UAT")
                      .withX(300)
                      .withY(50)
                      .withType(StateType.ENV_STATE.name())
                      .addProperty("envId", "34567")
                      .build())
        .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
        .addLinks(aLink().withId("l2").withFrom("n2").withTo("n3").withType("success").build())
        .addLinks(aLink().withId("l3").withFrom("n3").withTo("n4").withType("success").build())
        .build();
  }

  private Pipeline createPipelineNoGraph() {
    Pipeline pipeline = new Pipeline();
    pipeline.setAppId(appId);
    pipeline.setName("pipeline1");
    pipeline.setDescription("Sample Pipeline");

    pipeline.setServices(getServices());

    pipeline = workflowService.createWorkflow(Pipeline.class, pipeline);
    assertThat(pipeline).isNotNull();
    assertThat(pipeline.getUuid()).isNotNull();

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(pipeline.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res).isNotNull().hasSize(0);

    return pipeline;
  }

  /**
   * Should create orchestration.
   */
  @Test
  public void shouldCreateOrchestration() {
    createOrchestration();
  }

  /**
   * Should update orchestration.
   */
  @Test
  public void shouldUpdateOrchestration() {
    Orchestration orchestration = createOrchestration();
    String uuid = orchestration.getUuid();
    orchestration = workflowService.readOrchestration(appId, orchestration.getEnvironment().getUuid(), uuid);

    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();
    assertThat(orchestration.getUuid()).isEqualTo(uuid);

    orchestration.setName("orchestration2");
    orchestration.setDescription(null);
    Graph graph = orchestration.getGraph();

    graph.getNodes().add(aNode().withId("n5").withName("http").withX(350).withType(StateType.HTTP.name()).build());
    graph.getLinks().add(aLink().withId("l3").withFrom("n3").withTo("n5").withType("success").build());

    Orchestration updatedOrchestration = workflowService.updateOrchestration(orchestration);
    assertThat(updatedOrchestration).isNotNull();
    assertThat(updatedOrchestration.getUuid()).isNotNull();
    assertThat(updatedOrchestration.getUuid()).isEqualTo(orchestration.getUuid());
    assertThat(updatedOrchestration.getName()).isEqualTo("orchestration2");
    assertThat(updatedOrchestration.getDescription()).isNull();
    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(orchestration.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res).isNotNull().hasSize(2);

    StateMachine sm = workflowService.readLatest(appId, updatedOrchestration.getUuid(), null);
    assertThat(sm.getGraph()).isNotNull().isEqualTo(graph);
  }

  /**
   * Should delete orchestration.
   */
  @Test
  public void shouldDeleteOrchestration() {
    Orchestration orchestration = createOrchestration();
    String uuid = orchestration.getUuid();
    workflowService.deleteWorkflow(Orchestration.class, appId, uuid);
    orchestration = workflowService.readOrchestration(appId, orchestration.getEnvironment().getUuid(), uuid);
    assertThat(orchestration).isNull();
  }

  private Orchestration createOrchestration() {
    Graph graph = aGraph()
                      .addNodes(aNode()
                                    .withId("n1")
                                    .withName("stop")
                                    .withX(200)
                                    .withY(50)
                                    .withType(StateType.ENV_STATE.name())
                                    .withOrigin(true)
                                    .build())
                      .addNodes(aNode()
                                    .withId("n2")
                                    .withName("wait")
                                    .withX(250)
                                    .withY(50)
                                    .withType(StateType.WAIT.name())
                                    .addProperty("duration", 1l)
                                    .build())
                      .addNodes(aNode()
                                    .withId("n3")
                                    .withName("start")
                                    .withX(300)
                                    .withY(50)
                                    .withType(StateType.ENV_STATE.name())
                                    .build())
                      .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
                      .addLinks(aLink().withId("l2").withFrom("n2").withTo("n3").withType("success").build())
                      .build();

    Orchestration orchestration = anOrchestration()
                                      .withAppId(appId)
                                      .withName("workflow1")
                                      .withDescription("Sample Workflow")
                                      .withEnvironment(getEnvironment())
                                      .withGraph(graph)
                                      .withServices(getServices())
                                      .build();

    orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(orchestration.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res)
        .isNotNull()
        .hasSize(1)
        .doesNotContainNull()
        .extracting(StateMachine::getGraph)
        .doesNotContainNull()
        .containsExactly(graph);

    return orchestration;
  }

  /**
   * Stencils.
   *
   * @throws IllegalAccessException    the illegal access exception
   * @throws IllegalArgumentException  the illegal argument exception
   * @throws InvocationTargetException the invocation target exception
   * @throws IntrospectionException    the introspection exception
   */
  @Test
  public void stencils()
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
    Map<StateTypeScope, List<Stencil>> stencils = workflowService.stencils(APP_ID);
    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(2).containsKeys(
        StateTypeScope.ORCHESTRATION_STENCILS, StateTypeScope.PIPELINE_STENCILS);
    assertThat(stencils.get(StateTypeScope.ORCHESTRATION_STENCILS))
        .extracting(Stencil::getType)
        .contains("REPEAT", "FORK");
  }

  /**
   * Stencils for pipeline.
   *
   * @throws IllegalAccessException    the illegal access exception
   * @throws IllegalArgumentException  the illegal argument exception
   * @throws InvocationTargetException the invocation target exception
   * @throws IntrospectionException    the introspection exception
   */
  @Test
  public void stencilsForPipeline()
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
    Map<StateTypeScope, List<Stencil>> stencils = workflowService.stencils(APP_ID, StateTypeScope.PIPELINE_STENCILS);
    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(1).containsKeys(StateTypeScope.PIPELINE_STENCILS);
    assertThat(stencils.get(StateTypeScope.PIPELINE_STENCILS))
        .extracting(Stencil::getType)
        .contains("BUILD", "ENV_STATE")
        .doesNotContain("REPEAT", "FORK");
  }

  /**
   * Stencils for orchestration.
   *
   * @throws IllegalAccessException    the illegal access exception
   * @throws IllegalArgumentException  the illegal argument exception
   * @throws InvocationTargetException the invocation target exception
   * @throws IntrospectionException    the introspection exception
   */
  @Test
  public void stencilsForOrchestration()
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
    Map<StateTypeScope, List<Stencil>> stencils =
        workflowService.stencils(APP_ID, StateTypeScope.ORCHESTRATION_STENCILS);
    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(1).containsKeys(StateTypeScope.ORCHESTRATION_STENCILS);
    assertThat(stencils.get(StateTypeScope.ORCHESTRATION_STENCILS))
        .extracting(Stencil::getType)
        .doesNotContain("BUILD", "ENV_STATE")
        .contains("REPEAT", "FORK");
  }
}
