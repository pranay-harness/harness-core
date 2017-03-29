package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static software.wings.beans.FailureStrategy.FailureStrategyBuilder.aFailureStrategy;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowExecutionFilter.WorkflowExecutionFilterBuilder.aWorkflowExecutionFilter;
import static software.wings.beans.WorkflowFailureStrategy.WorkflowFailureStrategyBuilder.aWorkflowFailureStrategy;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.APP_ID;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.CustomOrchestrationWorkflow.CustomOrchestrationWorkflowBuilder;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureType;
import software.wings.beans.Graph;
import software.wings.beans.RepairActionCode;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowFailureStrategy;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.State;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionSimulator;
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

/**
 * The Class WorkflowServiceTest.
 *
 * @author Rishi
 */
@Listeners(NotifyEventListener.class)
public class WorkflowServiceTest extends WingsBaseTest {
  private static String appId = UUIDGenerator.getUuid();
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @InjectMocks @Inject private WorkflowService workflowService;

  @Mock private WingsPersistence wingsPersistence;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceInstanceService serviceInstanceService;
  @Mock private StateMachineExecutionSimulator stateMachineExecutionSimulator;
  @Mock private InfrastructureMappingService infrastructureMappingService;

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
      services = newArrayList(
          wingsPersistence.saveAndGet(Service.class, aService().withAppId(appId).withName("catalog").build()),
          wingsPersistence.saveAndGet(Service.class, aService().withAppId(appId).withName("content").build()));
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

    sm = workflowService.createStateMachine(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    String smId = sm.getUuid();
    sm = wingsPersistence.get(StateMachine.class, smId);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();
    System.out.println("All Done!");
  }
  //
  //  /**
  //   * Should read simple workflow.
  //   */
  //  @Test
  //  public void shouldReadSimpleWorkflow() {
  //    Application app = wingsPersistence.saveAndGet(Application.class, anApplication().withName("App1").build());
  //    Environment env = wingsPersistence.saveAndGet(Environment.class,
  //    Builder.anEnvironment().withAppId(app.getUuid()).build());
  //
  //    Workflow workflow = workflowService.readLatestSimpleWorkflow(app.getUuid());
  //    assertThat(workflow).isNotNull();
  //    assertThat(workflow.getWorkflowType()).isEqualTo(WorkflowType.SIMPLE);
  //    assertThat(workflow.getGraph()).isNotNull();
  //    assertThat(workflow.getGraph().getNodes()).isNotNull();
  //    assertThat(workflow.getGraph().getNodes().size()).isEqualTo(2);
  //    assertThat(workflow.getGraph().getLinks()).isNotNull();
  //    assertThat(workflow.getGraph().getLinks().size()).isEqualTo(1);
  //  }

  //  /**
  //   * Should createStateMachine pipeline with graph.
  //   */
  //  @Test
  //  public void shouldCreatePipelineWithGraph() {
  //    createPipeline();
  //  }

  //  /**
  //   * Should listStateMachines pipeline
  //   */
  //  @Test
  //  public void shouldListPipeline() {
  //    createPipelineNoGraph();
  //    createPipeline();
  //    createPipelineNoGraph();
  //    createPipeline();
  //    PageRequest<Pipeline> req = new PageRequest<>();
  //    req.addFilter(SearchFilter.Builder.aSearchFilter().withField("appId", Operator.EQ, appId).build());
  //    PageResponse<Pipeline> res = workflowService.listPipelines(req);
  //    assertThat(res).isNotNull();
  //    assertThat(res.getResponse()).isNotNull();
  //    assertThat(res.size()).isEqualTo(4);
  //  }

  //  /**
  //   * Should read pipeline
  //   */
  //  @Test
  //  public void shouldReadPipeline() {
  //    Pipeline pipeline = createPipelineNoGraph();
  //    Pipeline pipeline2 = workflowService.readPipeline(appId, pipeline.getUuid());
  //    assertThat(pipeline2).isNotNull();
  //    assertThat(pipeline2).isEqualToComparingFieldByField(pipeline);
  //  }

  //  /**
  //   * Should update pipeline with no graph.
  //   */
  //  @Test
  //  public void shouldUpdatePipelineWithNoGraph() {
  //    Pipeline pipeline = createPipelineNoGraph();
  //    Graph graph = createInitialGraph();
  //    pipeline.setGraph(graph);
  //
  //    updatePipeline(pipeline, 1);
  //  }

  //  /**
  //   * Should update pipeline with graph.
  //   */
  //  @Test
  //  public void shouldUpdatePipelineWithGraph() {
  //    Pipeline pipeline = createPipeline();
  //    updatePipeline(pipeline, 2);
  //  }

  //  /**
  //   * Update pipeline.
  //   *
  //   * @param pipeline   the pipeline
  //   * @param graphCount the graph count
  //   */
  //  public void updatePipeline(Pipeline pipeline, int graphCount) {
  //    pipeline.setDescription("newDescription");
  //    pipeline.setName("pipeline2");
  //
  //    List<Service> newServices = Lists.newArrayList(wingsPersistence.saveAndGet(Service.class,
  //    aService().withAppId(appId).withName("ui").build()),
  //        wingsPersistence.saveAndGet(Service.class, aService().withAppId(appId).withName("server").build()));
  //    pipeline.setServices(newServices);
  //
  //    Graph graph = pipeline.getGraph();
  //    graph.getNodes().add(aNode().withId("n4").withName("PROD").withX(350).withY(50).addProperty("envId",
  //    "34567").withType(StateType.ENV_STATE.name()).build());
  //    graph.getLinks().add(aLink().withId("l3").withFrom("n3").withTo("n4").withType("success").build());
  //
  //    Pipeline updatedPipeline = workflowService.updatePipeline(pipeline);
  //    assertThat(updatedPipeline).isNotNull().extracting(Pipeline::getUuid, Pipeline::getName,
  //    Pipeline::getDescription, Pipeline::getServices, Pipeline::getDefaultVersion)
  //        .containsExactly(pipeline.getUuid(), "pipeline2", "newDescription", newServices, 2);
  //
  //    PageRequest<StateMachine> req = new PageRequest<>();
  //    SearchFilter filter = new SearchFilter();
  //    filter.setFieldName("originId");
  //    filter.setFieldValues(pipeline.getUuid());
  //    filter.setOp(Operator.EQ);
  //    req.addFilter(filter);
  //    PageResponse<StateMachine> res = workflowService.listStateMachines(req);
  //
  //    assertThat(res).isNotNull().hasSize(graphCount);
  //
  //    StateMachine sm = workflowService.readLatestStateMachine(appId, updatedPipeline.getUuid());
  //    assertThat(sm.getGraph()).isEqualTo(graph);
  //
  //  }
  //
  //  private Pipeline createPipeline() {
  //    Graph graph = createInitialGraph();
  //    Pipeline pipeline =
  //        aPipeline().withAppId(appId).withName("pipeline1").withDescription("Sample
  //        Pipeline").addServices("service1", "service2").withGraph(graph).build();
  //
  //    pipeline = workflowService.createWorkflow(Pipeline.class, pipeline);
  //    assertThat(pipeline).isNotNull().hasFieldOrProperty("uuid").hasFieldOrPropertyWithValue("defaultVersion", 1);
  //
  //    PageRequest<StateMachine> req = new PageRequest<>();
  //    SearchFilter filter = new SearchFilter();
  //    filter.setFieldName("originId");
  //    filter.setFieldValues(pipeline.getUuid());
  //    filter.setOp(Operator.EQ);
  //    req.addFilter(filter);
  //    PageResponse<StateMachine> res = workflowService.listStateMachines(req);
  //
  //    assertThat(res).isNotNull().hasSize(1).doesNotContainNull().extracting(StateMachine::getGraph).doesNotContainNull().containsExactly(graph);
  //    return pipeline;
  //  }

  /**
   * @return
   */
  private Graph createInitialGraph() {
    return aGraph()
        .addNodes(aNode()
                      .withId("n1")
                      .withName("IT")
                      .withX(250)
                      .withY(50)
                      .withType(StateType.ENV_STATE.name())
                      .withOrigin(true)
                      .addProperty("envId", "12345")
                      .build())
        .addNodes(aNode()
                      .withId("n2")
                      .withName("QA")
                      .withX(300)
                      .withY(50)
                      .withType(StateType.ENV_STATE.name())
                      .addProperty("envId", "23456")
                      .build())
        .addNodes(aNode()
                      .withId("n3")
                      .withName("UAT")
                      .withX(300)
                      .withY(50)
                      .withType(StateType.ENV_STATE.name())
                      .addProperty("envId", "34567")
                      .build())
        .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
        .addLinks(aLink().withId("l2").withFrom("n2").withTo("n3").withType("success").build())
        .build();
  }

  //  private Pipeline createPipelineNoGraph() {
  //    Pipeline pipeline = new Pipeline();
  //    pipeline.setAccountId(appId);
  //    pipeline.setName("pipeline1");
  //    pipeline.setDescription("Sample Pipeline");
  //
  //    pipeline.setServices(getServices());
  //
  //    pipeline = workflowService.createWorkflow(Pipeline.class, pipeline);
  //    assertThat(pipeline).isNotNull();
  //    assertThat(pipeline.getUuid()).isNotNull();
  //
  //    PageRequest<StateMachine> req = new PageRequest<>();
  //    SearchFilter filter = new SearchFilter();
  //    filter.setFieldName("originId");
  //    filter.setFieldValues(pipeline.getUuid());
  //    filter.setOp(Operator.EQ);
  //    req.addFilter(filter);
  //    PageResponse<StateMachine> res = workflowService.listStateMachines(req);
  //
  //    assertThat(res).isNotNull().hasSize(0);
  //
  //    return pipeline;
  //  }

  /**
   * Should createStateMachine workflow.
   */
  @Test
  public void shouldCreateWorkflow() {
    createWorkflow();
  }

  /**
   * Should update workflow.
   */
  @Test
  public void shouldUpdateWorkflow() {
    Workflow workflow = createWorkflow();
    String uuid = workflow.getUuid();
    workflow = workflowService.readWorkflow(appId, uuid, null);

    assertThat(workflow).isNotNull();
    assertThat(workflow.getUuid()).isNotNull();
    assertThat(workflow.getUuid()).isEqualTo(uuid);

    workflow.setName("workflow2");
    workflow.setDescription(null);

    Graph graph = ((CustomOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getGraph();
    graph.getNodes().add(aNode().withId("n5").withName("http").withX(350).withType(StateType.HTTP.name()).build());
    graph.getLinks().add(aLink().withId("l3").withFrom("n3").withTo("n5").withType("success").build());

    Workflow updatedWorkflow = workflowService.updateWorkflow(workflow);
    assertThat(updatedWorkflow).isNotNull().hasFieldOrPropertyWithValue("defaultVersion", 2);
    assertThat(updatedWorkflow.getUuid()).isNotNull();
    assertThat(updatedWorkflow.getUuid()).isEqualTo(workflow.getUuid());
    assertThat(updatedWorkflow.getName()).isEqualTo("workflow2");
    assertThat(updatedWorkflow.getDescription()).isNull();
    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(workflow.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.listStateMachines(req);

    assertThat(res).isNotNull().hasSize(2);

    StateMachine sm = workflowService.readLatestStateMachine(appId, updatedWorkflow.getUuid());
    assertThat(sm.getOrchestrationWorkflow()).isNotNull().hasFieldOrPropertyWithValue("graph", graph);
  }

  /**
   * Should delete workflow.
   */
  @Test
  public void shouldDeleteWorkflow() {
    Workflow workflow = createWorkflow();
    String uuid = workflow.getUuid();
    workflowService.deleteWorkflow(appId, uuid);
    workflow = workflowService.readWorkflow(appId, uuid, null);
    assertThat(workflow).isNull();
  }

  private Workflow createWorkflow() {
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

    Workflow workflow =
        aWorkflow()
            .withAppId(appId)
            .withName("workflow1")
            .withDescription("Sample Workflow")
            .withOrchestrationWorkflow(
                CustomOrchestrationWorkflowBuilder.aCustomOrchestrationWorkflow().withGraph(graph).build())
            .withServices(getServices())
            .build();

    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull().hasFieldOrProperty("uuid").hasFieldOrPropertyWithValue("defaultVersion", 1);

    PageRequest<StateMachine> req = aPageRequest().addFilter("originId", Operator.EQ, workflow.getUuid()).build();
    PageResponse<StateMachine> res = workflowService.listStateMachines(req);

    assertThat(res)
        .isNotNull()
        .hasSize(1)
        .doesNotContainNull()
        .extracting(StateMachine::getOrchestrationWorkflow)
        .doesNotContainNull()
        .extracting(orchestrationWorkflow
            -> orchestrationWorkflow instanceof CustomOrchestrationWorkflow
                && ((CustomOrchestrationWorkflow) orchestrationWorkflow).getGraph().equals(graph));

    return workflow;
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
    Map<StateTypeScope, List<Stencil>> stencils = workflowService.stencils(APP_ID, null, null);
    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(3).containsKeys(
        StateTypeScope.ORCHESTRATION_STENCILS, StateTypeScope.PIPELINE_STENCILS, StateTypeScope.NONE);
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
    Map<StateTypeScope, List<Stencil>> stencils =
        workflowService.stencils(APP_ID, null, null, StateTypeScope.PIPELINE_STENCILS);
    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(1).containsKeys(StateTypeScope.PIPELINE_STENCILS);
    assertThat(stencils.get(StateTypeScope.PIPELINE_STENCILS))
        .extracting(Stencil::getType)
        .contains("APPROVAL", "ENV_STATE")
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
        workflowService.stencils(APP_ID, null, null, StateTypeScope.ORCHESTRATION_STENCILS);
    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(1).containsKeys(StateTypeScope.ORCHESTRATION_STENCILS);
    assertThat(stencils.get(StateTypeScope.ORCHESTRATION_STENCILS))
        .extracting(Stencil::getType)
        .doesNotContain("BUILD", "ENV_STATE")
        .contains("REPEAT", "FORK");
  }

  @Test
  public void shouldCreateWorkflowFailure() {
    WorkflowFailureStrategy workflowFailureStrategy = createAndAssertWorkflowFailureStrategy(appId);
  }

  @Test
  public void shouldListWorkflowFailureStrategies() {
    String appId = UUIDGenerator.getUuid();
    createAndAssertWorkflowFailureStrategy(appId);
    createAndAssertWorkflowFailureStrategy(appId);
    createAndAssertWorkflowFailureStrategy(appId);
    createAndAssertWorkflowFailureStrategy(UUIDGenerator.getUuid());

    PageRequest<WorkflowFailureStrategy> pageRequest = aPageRequest().addFilter("appId", Operator.EQ, appId).build();
    PageResponse<WorkflowFailureStrategy> pageResponse = workflowService.listWorkflowFailureStrategies(pageRequest);
    assertThat(pageResponse)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting("appId")
        .containsExactly(appId, appId, appId);
  }

  @Test
  public void shouldListWorkflowFailureStrategiesByAppId() {
    String appId = UUIDGenerator.getUuid();
    createAndAssertWorkflowFailureStrategy(appId);
    createAndAssertWorkflowFailureStrategy(appId);
    createAndAssertWorkflowFailureStrategy(appId);
    createAndAssertWorkflowFailureStrategy(UUIDGenerator.getUuid());

    List<WorkflowFailureStrategy> res = workflowService.listWorkflowFailureStrategies(appId);
    assertThat(res).isNotNull().hasSize(3).doesNotContainNull().extracting("appId").containsExactly(
        appId, appId, appId);
  }

  @Test
  public void shouldDeleteWorkflowFailureStrategy() {
    String appId = UUIDGenerator.getUuid();
    WorkflowFailureStrategy workflowFailureStrategy = createAndAssertWorkflowFailureStrategy(appId);
    boolean deleted = workflowService.deleteWorkflowFailureStrategy(appId, workflowFailureStrategy.getUuid());
    assertThat(deleted).isTrue();
  }

  private WorkflowFailureStrategy createAndAssertWorkflowFailureStrategy(String appId) {
    WorkflowFailureStrategy workflowFailureStrategy =
        aWorkflowFailureStrategy()
            .withAppId(appId)
            .withName("Non-Prod Failure Strategy")
            .addFailureStrategy(aFailureStrategy()
                                    .addFailureTypes(FailureType.CONNECTIVITY)
                                    .addFailureTypes(FailureType.AUTHENTICATION)
                                    .withExecutionScope(ExecutionScope.WORKFLOW)
                                    .withRepairActionCode(RepairActionCode.ROLLBACK_WORKFLOW)
                                    .build())
            .withWorkflowExecutionFilter(aWorkflowExecutionFilter().addWorkflowId("workflow1").addEnvId("env1").build())
            .build();

    WorkflowFailureStrategy created = workflowService.createStateMachine(workflowFailureStrategy);
    assertThat(created).isNotNull().hasFieldOrProperty("uuid").isEqualToComparingFieldByField(workflowFailureStrategy);
    return created;
  }
  //
  //  @Test
  //  public void shouldCreateWorkflowWorkflow() {
  //    CanaryWorkflowWorkflow workflow = createWorkflowWorkflow();
  //    logger.info(JsonUtils.asJson(workflow));
  //  }
  //
  //  public Workflow createWorkflowWorkflow() {
  //
  //    Workflow workflowWorkflow = aWorkflow().withAppId(APP_ID).withWorkflowWorkflowType(WorkflowWorkflowType.CANARY)
  //        .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT,
  //        Constants.PRE_DEPLOYMENT).build()).withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT,
  //        Constants.POST_DEPLOYMENT)
  //            .build()).build();
  //
  //    CanaryWorkflowWorkflow workflowWorkflow2 = workflowService.createWorkflowWorkflow(workflowWorkflow);
  //    assertThat(workflowWorkflow2).isNotNull().hasFieldOrProperty("uuid").hasFieldOrProperty("preDeploymentSteps").hasFieldOrProperty("postDeploymentSteps")
  //        .hasFieldOrProperty("graph");
  //    return workflowWorkflow2;
  //  }
  //
  //  @Test
  //  public void shouldDeleteWorkflowWorkflow() {
  //    CanaryWorkflowWorkflow workflowWorkflow = createWorkflowWorkflow();
  //    workflowService.deleteWorkflowWorkflow(workflowWorkflow.getAppId(), workflowWorkflow.getUuid());
  //
  //    workflowWorkflow = workflowService.readWorkflow(appId, workflowWorkflow.getUuid());
  //    assertThat(workflowWorkflow).isNull();
  //
  //  }
  //
  //  @Test
  //  public void shouldListWorkflowWorkflow() {
  //    CanaryWorkflowWorkflow workflowWorkflow1 = createWorkflowWorkflow();
  //    CanaryWorkflowWorkflow workflowWorkflow2 = createWorkflowWorkflow();
  //    CanaryWorkflowWorkflow workflowWorkflow3 = createWorkflowWorkflow();
  //
  //    PageResponse<CanaryWorkflowWorkflow> response =
  //        workflowService.listWorkflowWorkflows(aPageRequest().withLimit("2").addOrder("createdAt",
  //        OrderType.ASC).build());
  //
  //    assertThat(response).isNotNull().hasSize(2).extracting("uuid").containsExactly(workflowWorkflow1.getUuid(),
  //    workflowWorkflow2.getUuid()); assertThat(response.getTotal()).isEqualTo(3);
  //  }
  //
  //  @Test
  //  public void shouldUpdateBasic() {
  //    CanaryWorkflowWorkflow workflowWorkflow1 = createWorkflowWorkflow();
  //    String name2 = "Name2";
  //
  //    CanaryWorkflowWorkflow workflowWorkflow2 =
  //        aWorkflow().withAppId(APP_ID).withUuid(workflowWorkflow1.getUuid()).withName(name2).build();
  //
  //    workflowService.updateWorkflowWorkflowBasic(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid(),
  //    workflowWorkflow2);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow3 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow2).isNotNull().hasFieldOrPropertyWithValue("name", name2);
  //  }
  //
  //  @Test
  //  public void shouldUpdatePreDeployment() {
  //    CanaryWorkflowWorkflow workflowWorkflow1 = createWorkflowWorkflow();
  //
  //    PhaseStep phaseStep = aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).withStepsInParallel(true).build();
  //    PhaseStep updated = workflowService.updatePreDeployment(workflowWorkflow1.getAppId(),
  //    workflowWorkflow1.getUuid(), phaseStep);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow2 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow2).isNotNull().hasFieldOrPropertyWithValue("preDeploymentSteps", phaseStep);
  //  }
  //
  //  @Test
  //  public void shouldUpdatePostDeployment() {
  //    CanaryWorkflowWorkflow workflowWorkflow1 = createWorkflowWorkflow();
  //
  //    PhaseStep phaseStep = aPhaseStep(POST_DEPLOYMENT, Constants.PRE_DEPLOYMENT).withStepsInParallel(true).build();
  //    PhaseStep updated = workflowService.updatePostDeployment(workflowWorkflow1.getAppId(),
  //    workflowWorkflow1.getUuid(), phaseStep);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow2 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow2).isNotNull().hasFieldOrPropertyWithValue("postDeploymentSteps", phaseStep);
  //  }
  //
  //  @Test
  //  public void shouldCreateWorkflowPhase() {
  //    when(serviceResourceServiceMock.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
  //    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(
  //        anAwsInfrastructureMapping().withUuid(INFRA_MAPPING_ID).withDeploymentType(SSH.name()).withComputeProviderType(SettingVariableTypes.AWS.name())
  //            .build());
  //    CanaryWorkflowWorkflow workflowWorkflow1 = createWorkflowWorkflow();
  //    WorkflowPhase workflowPhase =
  //    aWorkflowPhase().withServiceId(SERVICE_ID).withInfraMappingId(INFRA_MAPPING_ID).build();
  //
  //    workflowService.createWorkflowPhase(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid(), workflowPhase);
  //    CanaryWorkflowWorkflow workflowWorkflow2 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow2).isNotNull();
  //    assertThat(workflowWorkflow2.getWorkflowPhases()).isNotNull().hasSize(workflowWorkflow1.getWorkflowPhases().size()
  //    + 1);
  //
  //    WorkflowPhase workflowPhase2 =
  //    workflowWorkflow2.getWorkflowPhases().get(workflowWorkflow2.getWorkflowPhases().size() - 1);
  //    assertThat(workflowPhase2).isNotNull().hasFieldOrPropertyWithValue("name", Constants.PHASE_NAME_PREFIX + 1);
  //    assertThat(workflowPhase2.getPhaseSteps()).isNotNull().hasSize(6);
  //
  //  }
  //
  //  @Test
  //  public void shouldUpdateWorkflowPhase() {
  //    when(serviceResourceServiceMock.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
  //    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(
  //        anAwsInfrastructureMapping().withUuid(INFRA_MAPPING_ID).withDeploymentType(SSH.name()).withComputeProviderType(SettingVariableTypes.AWS.name())
  //            .build());
  //
  //    CanaryWorkflowWorkflow workflowWorkflow1 = createWorkflowWorkflow();
  //    WorkflowPhase workflowPhase =
  //    aWorkflowPhase().withName("phase1").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
  //    workflowService.createWorkflowPhase(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid(), workflowPhase);
  //
  //    WorkflowPhase workflowPhase2 =
  //    aWorkflowPhase().withName("phase2").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
  //    workflowService.createWorkflowPhase(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid(), workflowPhase2);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow2 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow2).isNotNull();
  //    assertThat(workflowWorkflow2.getWorkflowPhases()).isNotNull().hasSize(2);
  //
  //    workflowPhase2 = workflowWorkflow2.getWorkflowPhases().get(1);
  //    workflowPhase2.setName("phase2-changed");
  //
  //    workflowService.updateWorkflowPhase(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid(), workflowPhase2);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow3 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow3).isNotNull();
  //    assertThat(workflowWorkflow3.getWorkflowPhases()).isNotNull().hasSize(2);
  //    assertThat(workflowWorkflow3.getWorkflowPhases().get(1)).hasFieldOrPropertyWithValue("name", "phase2-changed");
  //
  //  }
  //
  //
  //  @Test
  //  public void shouldDeleteWorkflowPhase() {
  //    when(serviceResourceServiceMock.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
  //    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(
  //        anAwsInfrastructureMapping().withUuid(INFRA_MAPPING_ID).withDeploymentType(SSH.name()).withComputeProviderType(SettingVariableTypes.AWS.name())
  //            .build());
  //
  //    CanaryWorkflowWorkflow workflowWorkflow1 = createWorkflowWorkflow();
  //    WorkflowPhase workflowPhase =
  //    aWorkflowPhase().withName("phase1").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
  //    workflowService.createWorkflowPhase(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid(), workflowPhase);
  //
  //    WorkflowPhase workflowPhase2 =
  //    aWorkflowPhase().withName("phase2").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
  //    workflowService.createWorkflowPhase(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid(), workflowPhase2);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow2 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow2).isNotNull();
  //    assertThat(workflowWorkflow2.getWorkflowPhases()).isNotNull().hasSize(2);
  //
  //    WorkflowPhase phase1 = workflowWorkflow2.getWorkflowPhases().get(1);
  //
  //    assertThat(workflowWorkflow2.getGraph().getSubworkflows()).isNotNull().containsKeys(phase1.getUuid());
  //    phase1.getPhaseSteps().forEach(phaseStep -> {
  //      assertThat(workflowWorkflow2.getGraph().getSubworkflows()).containsKeys(phaseStep.getUuid());
  //    });
  //
  //    workflowService.deleteWorkflowPhase(APP_ID, workflowWorkflow2.getUuid(), phase1.getUuid());
  //
  //    CanaryWorkflowWorkflow workflowWorkflow3 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow2.getUuid());
  //
  //    assertThat(workflowWorkflow3.getGraph().getNodes()).isNotNull().extracting("name").isNotNull().doesNotContain(phase1.getName());
  //    assertThat(workflowWorkflow3.getGraph().getSubworkflows()).isNotNull().doesNotContainKeys(phase1.getUuid());
  //    phase1.getPhaseSteps().forEach(phaseStep -> {
  //      assertThat(workflowWorkflow3.getGraph().getSubworkflows()).doesNotContainKeys(phaseStep.getUuid());
  //    });
  //
  //  }
  //
  //
  //  @Test
  //  public void shouldUpdateWorkflowPhaseRollback() {
  //    when(serviceResourceServiceMock.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
  //    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(
  //        anAwsInfrastructureMapping().withUuid(INFRA_MAPPING_ID).withDeploymentType(SSH.name()).withComputeProviderType(SettingVariableTypes.AWS.name())
  //            .build());
  //
  //    CanaryWorkflowWorkflow workflowWorkflow1 = createWorkflowWorkflow();
  //    WorkflowPhase workflowPhase =
  //    aWorkflowPhase().withName("phase1").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
  //    workflowService.createWorkflowPhase(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid(), workflowPhase);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow2 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow2).isNotNull();
  //    assertThat(workflowWorkflow2.getWorkflowPhases()).isNotNull().hasSize(1);
  //
  //    WorkflowPhase workflowPhase2 = workflowWorkflow2.getWorkflowPhases().get(0);
  //    assertThat(workflowPhase2).isNotNull();
  //
  //    assertThat(workflowWorkflow2.getRollbackWorkflowPhaseIdMap()).isNotNull().containsKeys(workflowPhase2.getUuid());
  //
  //    WorkflowPhase rollbackPhase = workflowWorkflow2.getRollbackWorkflowPhaseIdMap().get(workflowPhase2.getUuid());
  //    assertThat(rollbackPhase).isNotNull();
  //
  //    int size = rollbackPhase.getPhaseSteps().size();
  //    rollbackPhase.getPhaseSteps().remove(0);
  //
  //    workflowService.updateWorkflowPhaseRollback(APP_ID, workflowWorkflow2.getUuid(), workflowPhase2.getUuid(),
  //    rollbackPhase);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow3 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow3).isNotNull();
  //    assertThat(workflowWorkflow3.getWorkflowPhases()).isNotNull().hasSize(1);
  //    assertThat(workflowWorkflow3.getWorkflowPhases().get(0)).isNotNull().hasFieldOrPropertyWithValue("uuid",
  //    workflowPhase2.getUuid());
  //
  //    assertThat(workflowWorkflow3.getRollbackWorkflowPhaseIdMap()).isNotNull().containsKeys(workflowWorkflow3.getWorkflowPhases().get(0).getUuid());
  //    WorkflowPhase rollbackPhase2 = workflowWorkflow3.getRollbackWorkflowPhaseIdMap().get(workflowPhase2.getUuid());
  //    assertThat(rollbackPhase2).isNotNull().hasFieldOrPropertyWithValue("uuid", rollbackPhase.getUuid());
  //    assertThat(rollbackPhase2.getPhaseSteps()).hasSize(size - 1);
  //
  //  }
  //
  //  @Test
  //  public void shouldUpdateNode() {
  //
  //    CanaryWorkflowWorkflow workflowWorkflow =
  //    aWorkflow().withAppId(APP_ID).withWorkflowWorkflowType(WorkflowWorkflowType.CANARY)
  //        .withPreDeploymentSteps(
  //            aPhaseStep(PRE_DEPLOYMENT,
  //            Constants.PRE_DEPLOYMENT).addStep(aNode().withType("HTTP").withName("http").addProperty("URL",
  //            "http://www.google.com").build())
  //                .build()).withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT,
  //                Constants.POST_DEPLOYMENT).build()).build();
  //
  //    CanaryWorkflowWorkflow workflowWorkflow2 = workflowService.createWorkflowWorkflow(workflowWorkflow);
  //    assertThat(workflowWorkflow2).isNotNull().hasFieldOrProperty("uuid").hasFieldOrProperty("graph").hasFieldOrProperty("preDeploymentSteps")
  //        .hasFieldOrProperty("postDeploymentSteps");
  //
  //    assertThat(workflowWorkflow2.getGraph().getSubworkflows()).isNotNull().containsKeys(workflowWorkflow2.getPreDeploymentSteps().getUuid())
  //        .containsKeys(workflowWorkflow2.getPostDeploymentSteps().getUuid());
  //
  //    Graph graph =
  //    workflowWorkflow2.getGraph().getSubworkflows().get(workflowWorkflow2.getPreDeploymentSteps().getUuid());
  //    assertThat(graph).isNotNull();
  //    assertThat(graph.getNodes()).isNotNull().hasSize(1);
  //    Node node = graph.getNodes().get(0);
  //    assertThat(node).isNotNull().hasFieldOrProperty("id").hasFieldOrPropertyWithValue("type", "HTTP");
  //    assertThat(node.getProperties()).isNotNull().containsKey("URL").containsValue("http://www.google.com");
  //    node.getProperties().put("URL", "http://www.yahoo.com");
  //
  //    workflowService
  //        .updateGraphNode(workflowWorkflow2.getAppId(), workflowWorkflow2.getUuid(),
  //        workflowWorkflow2.getPreDeploymentSteps().getUuid(), node);
  //
  //    workflowWorkflow2 = workflowService.readWorkflow(workflowWorkflow2.getAppId(), workflowWorkflow2.getUuid());
  //    assertThat(workflowWorkflow2).isNotNull().hasFieldOrProperty("uuid").hasFieldOrProperty("graph").hasFieldOrProperty("preDeploymentSteps")
  //        .hasFieldOrProperty("postDeploymentSteps");
  //
  //    assertThat(workflowWorkflow2.getGraph().getSubworkflows()).isNotNull().containsKeys(workflowWorkflow2.getPreDeploymentSteps().getUuid())
  //        .containsKeys(workflowWorkflow2.getPostDeploymentSteps().getUuid());
  //
  //    graph = workflowWorkflow2.getGraph().getSubworkflows().get(workflowWorkflow2.getPreDeploymentSteps().getUuid());
  //    assertThat(graph).isNotNull();
  //    assertThat(graph.getNodes()).isNotNull().hasSize(1);
  //    node = graph.getNodes().get(0);
  //    assertThat(node).isNotNull().hasFieldOrProperty("id").hasFieldOrPropertyWithValue("type", "HTTP");
  //    assertThat(node.getProperties()).isNotNull().containsKey("URL").containsValue("http://www.yahoo.com");
  //
  //  }
  //
  //  @Test
  //  public void shouldHaveGraph() {
  //    when(serviceResourceServiceMock.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
  //    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(
  //        anAwsInfrastructureMapping().withUuid(INFRA_MAPPING_ID).withDeploymentType(SSH.name()).withComputeProviderType(SettingVariableTypes.AWS.name())
  //            .build());
  //
  //    CanaryWorkflowWorkflow workflowWorkflow1 = createWorkflowWorkflow();
  //
  //    WorkflowPhase workflowPhase =
  //    aWorkflowPhase().withName("phase1").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
  //    workflowService.createWorkflowPhase(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid(), workflowPhase);
  //
  //    WorkflowPhase workflowPhase2 =
  //    aWorkflowPhase().withName("phase2").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
  //    workflowService.createWorkflowPhase(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid(), workflowPhase2);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow2 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow2).isNotNull();
  //    assertThat(workflowWorkflow2.getWorkflowPhases()).isNotNull().hasSize(2);
  //
  //    workflowPhase2 = workflowWorkflow2.getWorkflowPhases().get(1);
  //    workflowPhase2.setName("phase2-changed");
  //
  //    workflowService.updateWorkflowPhase(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid(), workflowPhase2);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow3 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow3).isNotNull();
  //    assertThat(workflowWorkflow3.getWorkflowPhases()).isNotNull().hasSize(2);
  //    assertThat(workflowWorkflow3.getWorkflowPhases().get(1)).hasFieldOrPropertyWithValue("name", "phase2-changed");
  //
  //    Graph graph = workflowWorkflow3.getGraph();
  //    assertThat(graph).isNotNull();
  //    assertThat(graph.getNodes()).isNotNull().hasSize(6).doesNotContainNull();
  //    assertThat(graph.getLinks()).isNotNull().hasSize(3).doesNotContainNull();
  //    assertThat(graph.getNodes().get(0).getId()).isEqualTo(workflowWorkflow3.getPreDeploymentSteps().getUuid());
  //    assertThat(graph.getNodes().get(1).getId()).isEqualTo(workflowWorkflow3.getWorkflowPhaseIds().get(0));
  //    assertThat(graph.getNodes().get(3).getId()).isEqualTo(workflowWorkflow3.getWorkflowPhaseIds().get(1));
  //    assertThat(graph.getNodes().get(5).getId()).isEqualTo(workflowWorkflow3.getPostDeploymentSteps().getUuid());
  //    logger.info("Graph Nodes: {}", graph.getNodes());
  //    assertThat(graph.getSubworkflows()).isNotNull()
  //        .containsKeys(workflowWorkflow3.getPreDeploymentSteps().getUuid(),
  //        workflowWorkflow3.getWorkflowPhaseIds().get(0),
  //            workflowWorkflow3.getWorkflowPhaseIds().get(1), workflowWorkflow3.getPostDeploymentSteps().getUuid());
  //  }
  //
  //  @Test
  //  public void shouldUpdateNotificationRules() {
  //    CanaryWorkflowWorkflow workflowWorkflow1 = createWorkflowWorkflow();
  //
  //    List<NotificationRule> notificationRules = newArrayList(aNotificationRule().build());
  //    List<NotificationRule> updated =
  //        workflowService.updateNotificationRules(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid(),
  //        notificationRules);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow2 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow2).isNotNull().hasFieldOrPropertyWithValue("notificationRules", notificationRules);
  //  }
  //
  //
  //  @Test
  //  public void shouldUpdateFailureStrategies() {
  //    CanaryWorkflowWorkflow workflowWorkflow1 = createWorkflowWorkflow();
  //
  //    List<FailureStrategy> failureStrategies = newArrayList(aFailureStrategy().build());
  //    List<FailureStrategy> updated =
  //        workflowService.updateFailureStrategies(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid(),
  //        failureStrategies);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow2 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow2).isNotNull().hasFieldOrPropertyWithValue("failureStrategies", failureStrategies);
  //  }
  //
  //
  //  @Test
  //  public void shouldUpdateUserVariables() {
  //    CanaryWorkflowWorkflow workflowWorkflow1 = createWorkflowWorkflow();
  //
  //    List<Variable> userVariables = newArrayList(aVariable().build());
  //    List<Variable> updated = workflowService.updateUserVariables(workflowWorkflow1.getAppId(),
  //    workflowWorkflow1.getUuid(), userVariables);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow2 =
  //        workflowService.readWorkflow(workflowWorkflow1.getAppId(), workflowWorkflow1.getUuid());
  //    assertThat(workflowWorkflow2).isNotNull().hasFieldOrPropertyWithValue("userVariables", userVariables);
  //  }
  //
  //
  //  @Test
  //  public void shouldCreateComplexWorkflow() {
  //    when(serviceResourceServiceMock.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
  //    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(
  //        anAwsInfrastructureMapping().withUuid(INFRA_MAPPING_ID).withDeploymentType(SSH.name()).withComputeProviderType(SettingVariableTypes.AWS.name())
  //            .build());
  //
  //    CanaryWorkflowWorkflow workflowWorkflow =
  //    aWorkflow().withAppId(APP_ID).withWorkflowWorkflowType(WorkflowWorkflowType.CANARY)
  //        .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build()).addWorkflowPhases(
  //            aWorkflowPhase().withName("Phase1").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).withDeploymentType(SSH).build())
  //        .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build()).build();
  //
  //    CanaryWorkflowWorkflow workflowWorkflow2 = workflowService.createWorkflowWorkflow(workflowWorkflow);
  //    assertThat(workflowWorkflow2).isNotNull().hasFieldOrProperty("uuid").hasFieldOrProperty("preDeploymentSteps").hasFieldOrProperty("postDeploymentSteps")
  //        .hasFieldOrProperty("graph");
  //
  //    CanaryWorkflowWorkflow workflowWorkflow3 =
  //        workflowService.readWorkflow(workflowWorkflow2.getAppId(), workflowWorkflow2.getUuid());
  //    assertThat(workflowWorkflow3).isNotNull();
  //    assertThat(workflowWorkflow3.getWorkflowPhases()).isNotNull().hasSize(1);
  //
  //    WorkflowPhase workflowPhase = workflowWorkflow3.getWorkflowPhases().get(0);
  //    PhaseStep deployPhaseStep =
  //        workflowPhase.getPhaseSteps().stream().filter(ps -> ps.getPhaseStepType() ==
  //        PhaseStepType.DEPLOY_SERVICE).collect(Collectors.toList()).get(0);
  //
  //    deployPhaseStep.getSteps().add(aNode().withType("HTTP").withName("http").addProperty("url",
  //    "www.google.com").build());
  //
  //    workflowService.updateWorkflowPhase(workflowWorkflow2.getAppId(), workflowWorkflow2.getUuid(), workflowPhase);
  //
  //    CanaryWorkflowWorkflow workflowWorkflow4 =
  //        workflowService.readWorkflow(workflowWorkflow2.getAppId(), workflowWorkflow2.getUuid());
  //    workflowService
  //        .deleteWorkflowPhase(workflowWorkflow4.getAppId(), workflowWorkflow4.getUuid(),
  //        workflowWorkflow4.getWorkflowPhaseIds().get(0));
  //
  //    CanaryWorkflowWorkflow workflowWorkflow5 =
  //        workflowService.readWorkflow(workflowWorkflow2.getAppId(), workflowWorkflow2.getUuid());
  //    assertThat(workflowWorkflow5).isNotNull();
  //
  //    logger.info("Graph Json : \n {}", JsonUtils.asJson(workflowWorkflow4.getGraph()));
  //
  //  }
}
