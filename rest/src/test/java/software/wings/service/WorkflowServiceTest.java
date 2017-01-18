package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static software.wings.beans.DeploymentType.SSH;
import static software.wings.beans.FailureStrategy.FailureStrategyBuilder.aFailureStrategy;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.Orchestration.Builder.anOrchestration;
import static software.wings.beans.OrchestrationWorkflow.OrchestrationWorkflowBuilder.anOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.WorkflowExecutionFilter.WorkflowExecutionFilterBuilder.aWorkflowExecutionFilter;
import static software.wings.beans.WorkflowFailureStrategy.WorkflowFailureStrategyBuilder.aWorkflowFailureStrategy;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.APP_ID;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureStrategy;
import software.wings.beans.FailureType;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Node;
import software.wings.beans.NotificationRule;
import software.wings.beans.Orchestration;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.RepairActionCode;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.Variable;
import software.wings.beans.WorkflowFailureStrategy;
import software.wings.beans.WorkflowOrchestrationType;
import software.wings.beans.WorkflowPhase;
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
import java.util.stream.Collectors;
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
  @Inject private WingsPersistence wingsPersistence;

  @Mock private ServiceResourceService serviceResourceServiceMock;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Mock private ServiceInstanceService serviceInstanceServiceMock;
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

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();

    String smId = sm.getUuid();
    sm = wingsPersistence.get(StateMachine.class, smId);
    assertThat(sm).isNotNull().extracting(StateMachine::getUuid).doesNotContainNull();
    System.out.println("All Done!");
  }

  //  /**
  //   * Should create pipeline with graph.
  //   */
  //  @Test
  //  public void shouldCreatePipelineWithGraph() {
  //    createPipeline();
  //  }

  //  /**
  //   * Should list pipeline
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
  //    PageResponse<StateMachine> res = workflowService.list(req);
  //
  //    assertThat(res).isNotNull().hasSize(graphCount);
  //
  //    StateMachine sm = workflowService.readLatest(appId, updatedPipeline.getUuid());
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
  //    PageResponse<StateMachine> res = workflowService.list(req);
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
  //    PageResponse<StateMachine> res = workflowService.list(req);
  //
  //    assertThat(res).isNotNull().hasSize(0);
  //
  //    return pipeline;
  //  }

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
    orchestration = workflowService.readOrchestration(appId, uuid, null);

    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();
    assertThat(orchestration.getUuid()).isEqualTo(uuid);

    orchestration.setName("orchestration2");
    orchestration.setDescription(null);
    orchestration.setSetAsDefault(true);
    Graph graph = orchestration.getGraph();

    graph.getNodes().add(aNode().withId("n5").withName("http").withX(350).withType(StateType.HTTP.name()).build());
    graph.getLinks().add(aLink().withId("l3").withFrom("n3").withTo("n5").withType("success").build());

    Orchestration updatedOrchestration = workflowService.updateOrchestration(orchestration);
    assertThat(updatedOrchestration).isNotNull().hasFieldOrPropertyWithValue("defaultVersion", 2);
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

    StateMachine sm = workflowService.readLatest(appId, updatedOrchestration.getUuid());
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
    orchestration = workflowService.readOrchestration(appId, uuid, null);
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
                                      .withGraph(graph)
                                      .withServices(getServices())
                                      .build();

    orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
    assertThat(orchestration).isNotNull().hasFieldOrProperty("uuid").hasFieldOrPropertyWithValue("defaultVersion", 1);

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
    Map<StateTypeScope, List<Stencil>> stencils = workflowService.stencils(APP_ID, StateTypeScope.PIPELINE_STENCILS);
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
        workflowService.stencils(APP_ID, StateTypeScope.ORCHESTRATION_STENCILS);
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

    WorkflowFailureStrategy created = workflowService.create(workflowFailureStrategy);
    assertThat(created).isNotNull().hasFieldOrProperty("uuid").isEqualToComparingFieldByField(workflowFailureStrategy);
    return created;
  }

  @Test
  public void shouldCreateOrchestrationWorkflow() {
    OrchestrationWorkflow workflow = createOrchestrationWorkflow();
    logger.info(JsonUtils.asJson(workflow));
  }

  public OrchestrationWorkflow createOrchestrationWorkflow() {
    OrchestrationWorkflow orchestrationWorkflow =
        anOrchestrationWorkflow()
            .withAppId(APP_ID)
            .withWorkflowOrchestrationType(WorkflowOrchestrationType.CANARY)
            .withPreDeploymentSteps(aPhaseStep(PhaseStepType.PRE_DEPLOYMENT).build())
            .withPostDeploymentSteps(aPhaseStep(PhaseStepType.POST_DEPLOYMENT).build())
            .build();

    OrchestrationWorkflow orchestrationWorkflow2 = workflowService.createOrchestrationWorkflow(orchestrationWorkflow);
    assertThat(orchestrationWorkflow2)
        .isNotNull()
        .hasFieldOrProperty("uuid")
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps")
        .hasFieldOrProperty("graph");
    return orchestrationWorkflow2;
  }

  @Test
  public void shouldDeleteOrchestrationWorkflow() {
    OrchestrationWorkflow orchestrationWorkflow = createOrchestrationWorkflow();
    workflowService.deleteOrchestrationWorkflow(orchestrationWorkflow.getAppId(), orchestrationWorkflow.getUuid());

    orchestrationWorkflow = workflowService.readOrchestrationWorkflow(appId, orchestrationWorkflow.getUuid());
    assertThat(orchestrationWorkflow).isNull();
  }

  @Test
  public void shouldListOrchestrationWorkflow() {
    OrchestrationWorkflow orchestrationWorkflow1 = createOrchestrationWorkflow();
    OrchestrationWorkflow orchestrationWorkflow2 = createOrchestrationWorkflow();
    OrchestrationWorkflow orchestrationWorkflow3 = createOrchestrationWorkflow();

    PageResponse<OrchestrationWorkflow> response = workflowService.listOrchestrationWorkflows(
        aPageRequest().withLimit("2").addOrder("createdAt", OrderType.ASC).build());

    assertThat(response).isNotNull().hasSize(2).extracting("uuid").containsExactly(
        orchestrationWorkflow1.getUuid(), orchestrationWorkflow2.getUuid());
    assertThat(response.getTotal()).isEqualTo(3);
  }

  @Test
  public void shouldUpdateBasic() {
    OrchestrationWorkflow orchestrationWorkflow1 = createOrchestrationWorkflow();
    String name2 = "Name2";

    OrchestrationWorkflow orchestrationWorkflow2 =
        anOrchestrationWorkflow().withAppId(APP_ID).withUuid(orchestrationWorkflow1.getUuid()).withName(name2).build();

    workflowService.updateOrchestrationWorkflowBasic(
        orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid(), orchestrationWorkflow2);

    OrchestrationWorkflow orchestrationWorkflow3 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid());
    assertThat(orchestrationWorkflow2).isNotNull().hasFieldOrPropertyWithValue("name", name2);
  }

  @Test
  public void shouldUpdatePreDeployment() {
    OrchestrationWorkflow orchestrationWorkflow1 = createOrchestrationWorkflow();

    PhaseStep phaseStep = aPhaseStep(PhaseStepType.PRE_DEPLOYMENT).withStepsInParallel(true).build();
    PhaseStep updated = workflowService.updatePreDeployment(
        orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid(), phaseStep);

    OrchestrationWorkflow orchestrationWorkflow2 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid());
    assertThat(orchestrationWorkflow2).isNotNull().hasFieldOrPropertyWithValue("preDeploymentSteps", phaseStep);
  }

  @Test
  public void shouldUpdatePostDeployment() {
    OrchestrationWorkflow orchestrationWorkflow1 = createOrchestrationWorkflow();

    PhaseStep phaseStep = aPhaseStep(PhaseStepType.POST_DEPLOYMENT).withStepsInParallel(true).build();
    PhaseStep updated = workflowService.updatePostDeployment(
        orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid(), phaseStep);

    OrchestrationWorkflow orchestrationWorkflow2 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid());
    assertThat(orchestrationWorkflow2).isNotNull().hasFieldOrPropertyWithValue("postDeploymentSteps", phaseStep);
  }

  @Test
  public void shouldCreateWorkflowPhase() {
    OrchestrationWorkflow orchestrationWorkflow1 = createOrchestrationWorkflow();
    WorkflowPhase workflowPhase = aWorkflowPhase().withName("phase1").build();

    workflowService.createWorkflowPhase(
        orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid(), workflowPhase);
    OrchestrationWorkflow orchestrationWorkflow2 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid());
    assertThat(orchestrationWorkflow2).isNotNull();
    assertThat(orchestrationWorkflow2.getWorkflowPhases())
        .isNotNull()
        .hasSize(orchestrationWorkflow1.getWorkflowPhases().size() + 1);

    WorkflowPhase workflowPhase2 =
        orchestrationWorkflow2.getWorkflowPhases().get(orchestrationWorkflow2.getWorkflowPhases().size() - 1);
    assertThat(workflowPhase2).isNotNull().hasFieldOrPropertyWithValue("name", "phase1");
    assertThat(workflowPhase2.getPhaseSteps()).isNotNull().hasSize(6);
  }

  @Test
  public void shouldUpdateWorkflowPhase() {
    OrchestrationWorkflow orchestrationWorkflow1 = createOrchestrationWorkflow();

    WorkflowPhase workflowPhase = aWorkflowPhase().withName("phase1").build();
    workflowService.createWorkflowPhase(
        orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 = aWorkflowPhase().withName("phase2").build();
    workflowService.createWorkflowPhase(
        orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid(), workflowPhase2);

    OrchestrationWorkflow orchestrationWorkflow2 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid());
    assertThat(orchestrationWorkflow2).isNotNull();
    assertThat(orchestrationWorkflow2.getWorkflowPhases()).isNotNull().hasSize(2);

    workflowPhase2 = orchestrationWorkflow2.getWorkflowPhases().get(1);
    workflowPhase2.setName("phase2-changed");

    workflowService.updateWorkflowPhase(
        orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid(), workflowPhase2);

    OrchestrationWorkflow orchestrationWorkflow3 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid());
    assertThat(orchestrationWorkflow3).isNotNull();
    assertThat(orchestrationWorkflow3.getWorkflowPhases()).isNotNull().hasSize(2);
    assertThat(orchestrationWorkflow3.getWorkflowPhases().get(1)).hasFieldOrPropertyWithValue("name", "phase2-changed");
  }

  @Test
  public void shouldUpdateNode() {
    OrchestrationWorkflow orchestrationWorkflow =
        anOrchestrationWorkflow()
            .withAppId(APP_ID)
            .withWorkflowOrchestrationType(WorkflowOrchestrationType.CANARY)
            .withPreDeploymentSteps(
                aPhaseStep(PhaseStepType.PRE_DEPLOYMENT)
                    .addStep(
                        aNode().withType("HTTP").withName("http").addProperty("URL", "http://www.google.com").build())
                    .build())
            .withPostDeploymentSteps(aPhaseStep(PhaseStepType.POST_DEPLOYMENT).build())
            .build();

    OrchestrationWorkflow orchestrationWorkflow2 = workflowService.createOrchestrationWorkflow(orchestrationWorkflow);
    assertThat(orchestrationWorkflow2)
        .isNotNull()
        .hasFieldOrProperty("uuid")
        .hasFieldOrProperty("graph")
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps");

    assertThat(orchestrationWorkflow2.getGraph().getSubworkflows())
        .isNotNull()
        .containsKeys(orchestrationWorkflow2.getPreDeploymentSteps().getUuid())
        .containsKeys(orchestrationWorkflow2.getPostDeploymentSteps().getUuid());

    Graph graph = orchestrationWorkflow2.getGraph().getSubworkflows().get(
        orchestrationWorkflow2.getPreDeploymentSteps().getUuid());
    assertThat(graph).isNotNull();
    assertThat(graph.getNodes()).isNotNull().hasSize(1);
    Node node = graph.getNodes().get(0);
    assertThat(node).isNotNull().hasFieldOrProperty("id").hasFieldOrPropertyWithValue("type", "HTTP");
    assertThat(node.getProperties()).isNotNull().containsKey("URL").containsValue("http://www.google.com");
    node.getProperties().put("URL", "http://www.yahoo.com");

    workflowService.updateGraphNode(orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid(),
        orchestrationWorkflow2.getPreDeploymentSteps().getUuid(), node);

    orchestrationWorkflow2 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid());
    assertThat(orchestrationWorkflow2)
        .isNotNull()
        .hasFieldOrProperty("uuid")
        .hasFieldOrProperty("graph")
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps");

    assertThat(orchestrationWorkflow2.getGraph().getSubworkflows())
        .isNotNull()
        .containsKeys(orchestrationWorkflow2.getPreDeploymentSteps().getUuid())
        .containsKeys(orchestrationWorkflow2.getPostDeploymentSteps().getUuid());

    graph = orchestrationWorkflow2.getGraph().getSubworkflows().get(
        orchestrationWorkflow2.getPreDeploymentSteps().getUuid());
    assertThat(graph).isNotNull();
    assertThat(graph.getNodes()).isNotNull().hasSize(1);
    node = graph.getNodes().get(0);
    assertThat(node).isNotNull().hasFieldOrProperty("id").hasFieldOrPropertyWithValue("type", "HTTP");
    assertThat(node.getProperties()).isNotNull().containsKey("URL").containsValue("http://www.yahoo.com");
  }

  @Test
  public void shouldHaveGraph() {
    OrchestrationWorkflow orchestrationWorkflow1 = createOrchestrationWorkflow();

    WorkflowPhase workflowPhase = aWorkflowPhase().withName("phase1").build();
    workflowService.createWorkflowPhase(
        orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 = aWorkflowPhase().withName("phase2").build();
    workflowService.createWorkflowPhase(
        orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid(), workflowPhase2);

    OrchestrationWorkflow orchestrationWorkflow2 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid());
    assertThat(orchestrationWorkflow2).isNotNull();
    assertThat(orchestrationWorkflow2.getWorkflowPhases()).isNotNull().hasSize(2);

    workflowPhase2 = orchestrationWorkflow2.getWorkflowPhases().get(1);
    workflowPhase2.setName("phase2-changed");
    workflowPhase2.addPhaseStep(aPhaseStep(PhaseStepType.DEPLOY_SERVICE).build());

    workflowService.updateWorkflowPhase(
        orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid(), workflowPhase2);

    OrchestrationWorkflow orchestrationWorkflow3 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid());
    assertThat(orchestrationWorkflow3).isNotNull();
    assertThat(orchestrationWorkflow3.getWorkflowPhases()).isNotNull().hasSize(2);
    assertThat(orchestrationWorkflow3.getWorkflowPhases().get(1)).hasFieldOrPropertyWithValue("name", "phase2-changed");

    Graph graph = orchestrationWorkflow3.getGraph();
    assertThat(graph).isNotNull();
    assertThat(graph.getNodes()).isNotNull().hasSize(4).doesNotContainNull();
    assertThat(graph.getLinks()).isNotNull().hasSize(3).doesNotContainNull();
    assertThat(graph.getNodes().get(0).getId()).isEqualTo(orchestrationWorkflow3.getPreDeploymentSteps().getUuid());
    assertThat(graph.getNodes().get(1).getId()).isEqualTo(orchestrationWorkflow3.getWorkflowPhaseIds().get(0));
    assertThat(graph.getNodes().get(2).getId()).isEqualTo(orchestrationWorkflow3.getWorkflowPhaseIds().get(1));
    assertThat(graph.getNodes().get(3).getId()).isEqualTo(orchestrationWorkflow3.getPostDeploymentSteps().getUuid());
    logger.info("Graph Nodes: {}", graph.getNodes());
    assertThat(graph.getSubworkflows())
        .isNotNull()
        .containsKeys(orchestrationWorkflow3.getPreDeploymentSteps().getUuid(),
            orchestrationWorkflow3.getWorkflowPhaseIds().get(0), orchestrationWorkflow3.getWorkflowPhaseIds().get(1),
            orchestrationWorkflow3.getPostDeploymentSteps().getUuid());
  }

  @Test
  public void shouldUpdateNotificationRules() {
    OrchestrationWorkflow orchestrationWorkflow1 = createOrchestrationWorkflow();

    List<NotificationRule> notificationRules = newArrayList(aNotificationRule().build());
    List<NotificationRule> updated = workflowService.updateNotificationRules(
        orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid(), notificationRules);

    OrchestrationWorkflow orchestrationWorkflow2 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid());
    assertThat(orchestrationWorkflow2).isNotNull().hasFieldOrPropertyWithValue("notificationRules", notificationRules);
  }

  @Test
  public void shouldUpdateFailureStrategies() {
    OrchestrationWorkflow orchestrationWorkflow1 = createOrchestrationWorkflow();

    List<FailureStrategy> failureStrategies = newArrayList(aFailureStrategy().build());
    List<FailureStrategy> updated = workflowService.updateFailureStrategies(
        orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid(), failureStrategies);

    OrchestrationWorkflow orchestrationWorkflow2 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid());
    assertThat(orchestrationWorkflow2).isNotNull().hasFieldOrPropertyWithValue("failureStrategies", failureStrategies);
  }

  @Test
  public void shouldUpdateUserVariables() {
    OrchestrationWorkflow orchestrationWorkflow1 = createOrchestrationWorkflow();

    List<Variable> userVariables = newArrayList(aVariable().build());
    List<Variable> updated = workflowService.updateUserVariables(
        orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid(), userVariables);

    OrchestrationWorkflow orchestrationWorkflow2 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow1.getAppId(), orchestrationWorkflow1.getUuid());
    assertThat(orchestrationWorkflow2).isNotNull().hasFieldOrPropertyWithValue("userVariables", userVariables);
  }

  @Test
  public void shouldCreateComplexWorkflow() {
    OrchestrationWorkflow orchestrationWorkflow =
        anOrchestrationWorkflow()
            .withAppId(APP_ID)
            .withWorkflowOrchestrationType(WorkflowOrchestrationType.CANARY)
            .withPreDeploymentSteps(aPhaseStep(PhaseStepType.PRE_DEPLOYMENT).build())
            .addWorkflowPhases(aWorkflowPhase()
                                   .withName("Phase1")
                                   .withComputeProviderId("computeProviderId1")
                                   .withServiceId("serviceId1")
                                   .withDeploymentType(SSH)
                                   .build())
            .withPostDeploymentSteps(aPhaseStep(PhaseStepType.POST_DEPLOYMENT).build())
            .build();

    OrchestrationWorkflow orchestrationWorkflow2 = workflowService.createOrchestrationWorkflow(orchestrationWorkflow);
    assertThat(orchestrationWorkflow2)
        .isNotNull()
        .hasFieldOrProperty("uuid")
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps")
        .hasFieldOrProperty("graph");

    OrchestrationWorkflow orchestrationWorkflow3 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid());
    assertThat(orchestrationWorkflow3).isNotNull();
    assertThat(orchestrationWorkflow3.getWorkflowPhases()).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = orchestrationWorkflow3.getWorkflowPhases().get(0);
    PhaseStep deployPhaseStep = workflowPhase.getPhaseSteps()
                                    .stream()
                                    .filter(ps -> ps.getPhaseStepType() == PhaseStepType.DEPLOY_SERVICE)
                                    .collect(Collectors.toList())
                                    .get(0);

    deployPhaseStep.getSteps().add(
        aNode().withType("HTTP").withName("http").addProperty("url", "www.google.com").build());

    workflowService.updateWorkflowPhase(
        orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid(), workflowPhase);

    OrchestrationWorkflow orchestrationWorkflow4 =
        workflowService.readOrchestrationWorkflow(orchestrationWorkflow2.getAppId(), orchestrationWorkflow2.getUuid());

    logger.info("Graph Json : \n {}", JsonUtils.asJson(orchestrationWorkflow4.getGraph()));
  }
}
