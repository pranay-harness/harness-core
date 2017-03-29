package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.CustomOrchestrationWorkflow.CustomOrchestrationWorkflowBuilder.aCustomOrchestrationWorkflow;
import static software.wings.beans.EntityVersion.Builder.anEntityVersion;
import static software.wings.beans.FailureStrategy.FailureStrategyBuilder.aFailureStrategy;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowExecutionFilter.WorkflowExecutionFilterBuilder.aWorkflowExecutionFilter;
import static software.wings.beans.WorkflowFailureStrategy.WorkflowFailureStrategyBuilder.aWorkflowFailureStrategy;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.WingsBaseTest;
import software.wings.beans.Base;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureType;
import software.wings.beans.Graph;
import software.wings.beans.PhaseStep;
import software.wings.beans.RepairActionCode;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowFailureStrategy;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.State;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionSimulator;
import software.wings.sm.StateMachineTest.StateSync;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.sm.Transition;
import software.wings.sm.TransitionType;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilPostProcessor;
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
  private static String envId = UUIDGenerator.getUuid();
  private static String workflowId = UUIDGenerator.getUuid();

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Mock private WingsPersistence wingsPersistence;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceInstanceService serviceInstanceService;
  @Mock private StateMachineExecutionSimulator stateMachineExecutionSimulator;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  private StencilPostProcessor stencilPostProcessor = mock(StencilPostProcessor.class, new Answer<List<Stencil>>() {
    @Override
    public List<Stencil> answer(InvocationOnMock invocationOnMock) throws Throwable {
      logger.info("invocationOnMock.getArguments()[0] " + invocationOnMock.getArguments()[0]);
      return (List<Stencil>) invocationOnMock.getArguments()[0];
    }
  });

  @Mock private PluginManager pluginManager;
  @Mock private UpdateOperations<Workflow> updateOperations;
  @Mock Query<Workflow> query;

  @Mock private EntityVersionService entityVersionService;

  @InjectMocks @Inject private WorkflowService workflowService;
  @Mock private FieldEnd fieldEnd;

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    setupSaveAndGet(Workflow.class);
    setupSaveAndGet(StateMachine.class);
    setupSaveAndGet(WorkflowFailureStrategy.class);

    when(wingsPersistence.createUpdateOperations(Workflow.class)).thenReturn(updateOperations);
    when(pluginManager.getExtensions(StateTypeDescriptor.class)).thenReturn(newArrayList());

    when(wingsPersistence.createQuery(eq(Workflow.class))).thenReturn(query);
    when(query.field(anyString())).thenReturn(fieldEnd);
    when(fieldEnd.equal(anyObject())).thenReturn(query);
  }

  private <T extends Base> void setupSaveAndGet(Class<T> cls) {
    when(wingsPersistence.saveAndGet(eq(cls), any(cls))).then(new Answer<T>() {
      @Override
      public T answer(InvocationOnMock invocationOnMock) throws Throwable {
        T t = (T) invocationOnMock.getArguments()[1];
        t.setUuid(UUIDGenerator.getUuid());
        return t;
      }
    });
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
    verify(wingsPersistence).saveAndGet(StateMachine.class, sm);
  }

  /**
   * Should read simple workflow.
   */
  @Test
  public void shouldReadSimpleWorkflowFromFile() {
    Workflow workflow = workflowService.readLatestSimpleWorkflow(appId, envId);
    assertThat(workflow)
        .isNotNull()
        .extracting("appId", "envId", "workflowType")
        .containsExactly(appId, envId, WorkflowType.SIMPLE);
    assertThat(workflow.getOrchestrationWorkflow()).isNotNull().isInstanceOf(CustomOrchestrationWorkflow.class);
    CustomOrchestrationWorkflow orchestrationWorkflow =
        (CustomOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow.getGraph()).isNotNull();
    assertThat(orchestrationWorkflow.getGraph().getNodes()).isNotNull().hasSize(2);
    assertThat(orchestrationWorkflow.getGraph().getLinks()).isNotNull().hasSize(1);
  }

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

  /**
   * Should create workflow.
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
    Graph graph1 = ((CustomOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getGraph();

    workflow.setName("workflow2");
    workflow.setDescription(null);

    Graph graph2 = ((CustomOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getGraph();
    graph2.getNodes().add(aNode().withId("n5").withName("http").withX(350).withType(StateType.HTTP.name()).build());
    graph2.getLinks().add(aLink().withId("l3").withFrom("n3").withTo("n5").withType("success").build());

    when(entityVersionService.lastEntityVersion(
             eq(workflow.getAppId()), eq(EntityType.WORKFLOW), eq(workflow.getUuid())))
        .thenReturn(anEntityVersion().withVersion(2).build());

    Workflow updatedWorkflow = workflowService.updateWorkflow(workflow);
    assertThat(updatedWorkflow).isNotNull().hasFieldOrPropertyWithValue("defaultVersion", 2);
    assertThat(updatedWorkflow.getUuid()).isNotNull();
    assertThat(updatedWorkflow.getUuid()).isEqualTo(workflow.getUuid());
    assertThat(updatedWorkflow.getName()).isEqualTo("workflow2");
    assertThat(updatedWorkflow.getDescription()).isNull();

    ArgumentCaptor<StateMachine> stateMachineArgumentCaptor = ArgumentCaptor.forClass(StateMachine.class);
    verify(wingsPersistence, times(2)).saveAndGet(eq(StateMachine.class), stateMachineArgumentCaptor.capture());
    assertThat(stateMachineArgumentCaptor.getAllValues())
        .isNotNull()
        .hasSize(2)
        .extracting("orchestrationWorkflow")
        .isNotNull()
        .extracting("graph")
        .isNotNull()
        .containsExactly(graph1, graph2);

    verify(updateOperations).set(eq("name"), eq(workflow.getName()));
    verify(updateOperations).unset(eq("description"));
    verify(updateOperations).set(eq("defaultVersion"), eq(2));
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

    CustomOrchestrationWorkflow orchestrationWorkflow = aCustomOrchestrationWorkflow().withGraph(graph).build();
    Workflow workflow = aWorkflow()
                            .withAppId(appId)
                            .withName("workflow1")
                            .withDescription("Sample Workflow")
                            .withOrchestrationWorkflow(orchestrationWorkflow)
                            .build();

    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull().hasFieldOrProperty("uuid").hasFieldOrPropertyWithValue("defaultVersion", 1);

    ArgumentCaptor<StateMachine> stateMachineArgumentCaptor = ArgumentCaptor.forClass(StateMachine.class);
    verify(wingsPersistence).saveAndGet(eq(StateMachine.class), stateMachineArgumentCaptor.capture());
    assertThat(stateMachineArgumentCaptor.getValue())
        .isNotNull()
        .extracting("orchestrationWorkflow")
        .isNotNull()
        .extracting("graph")
        .isNotNull()
        .containsExactly(graph);

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
    PageRequest pageRequest = Builder.aPageRequest().build();
    PageResponse res = aPageResponse().build();
    when(wingsPersistence.query(eq(WorkflowFailureStrategy.class), any(PageRequest.class))).thenReturn(res);
    workflowService.listWorkflowFailureStrategies(pageRequest);

    verify(wingsPersistence).query(eq(WorkflowFailureStrategy.class), eq(pageRequest));
  }

  @Test
  public void shouldListWorkflowFailureStrategiesByAppId() {
    PageResponse res = aPageResponse().build();
    when(wingsPersistence.query(eq(WorkflowFailureStrategy.class), any(PageRequest.class))).thenReturn(res);
    List<WorkflowFailureStrategy> res2 = workflowService.listWorkflowFailureStrategies(appId);
    assertThat(res2).isNotNull().isEqualTo(res);

    ArgumentCaptor<PageRequest> argumentCaptor = ArgumentCaptor.forClass(PageRequest.class);
    verify(wingsPersistence).query(eq(WorkflowFailureStrategy.class), argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).isNotNull().extracting("filters").hasSize(1);
    assertThat(argumentCaptor.getValue().getFilters().get(0))
        .isNotNull()
        .hasFieldOrPropertyWithValue("fieldName", "appId")
        .hasFieldOrPropertyWithValue("op", EQ)
        .hasFieldOrPropertyWithValue("fieldValues", new Object[] {appId});
  }

  @Test
  public void shouldDeleteWorkflowFailureStrategy() {
    String appId = UUIDGenerator.getUuid();
    String uuid = UUIDGenerator.getUuid();
    workflowService.deleteWorkflowFailureStrategy(appId, uuid);
    verify(wingsPersistence).delete(eq(WorkflowFailureStrategy.class), eq(appId), eq(uuid));
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

    WorkflowFailureStrategy created = workflowService.createWorkflowFailureStrategy(workflowFailureStrategy);
    assertThat(created).isNotNull().hasFieldOrProperty("uuid").isEqualToComparingFieldByField(workflowFailureStrategy);
    return created;
  }

  @Test
  public void shouldCreateCanaryWorkflow() {
    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid").hasFieldOrPropertyWithValue("appId", APP_ID);

    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow)
        .isNotNull()
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps")
        .hasFieldOrProperty("graph");
    assertThat(orchestrationWorkflow.getGraph()).isNotNull();
    assertThat(orchestrationWorkflow.getGraph().getNodes())
        .extracting("id")
        .contains(orchestrationWorkflow.getPostDeploymentSteps().getUuid(),
            orchestrationWorkflow.getPostDeploymentSteps().getUuid());

    assertThat(orchestrationWorkflow.getGraph().getSubworkflows())
        .containsKeys(orchestrationWorkflow.getPostDeploymentSteps().getUuid(),
            orchestrationWorkflow.getPostDeploymentSteps().getUuid());

    verify(entityVersionService)
        .newEntityVersion(eq(workflow2.getAppId()), eq(EntityType.WORKFLOW), eq(workflow2.getUuid()),
            eq(workflow2.getName()), eq(ChangeType.CREATED), eq(workflow2.getNotes()));

    ArgumentCaptor<StateMachine> stateMachineArgumentCaptor = ArgumentCaptor.forClass(StateMachine.class);
    verify(wingsPersistence).saveAndGet(eq(StateMachine.class), stateMachineArgumentCaptor.capture());
    StateMachine stateMachine = stateMachineArgumentCaptor.getValue();
    assertThat(stateMachine).isNotNull();
    assertThat(stateMachine.getOrchestrationWorkflow()).isNotNull().isEqualTo(workflow2.getOrchestrationWorkflow());

    logger.info(JsonUtils.asJson(workflow2));
  }

  @Test
  public void shouldUpdateBasic() {
    Workflow workflow =
        aWorkflow()
            .withUuid(WORKFLOW_ID)
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.updateWorkflow(workflow, null);
    verify(updateOperations).set(eq("name"), eq(WORKFLOW_NAME));
    verify(updateOperations).unset(eq("description"));
    verify(entityVersionService, never())
        .newEntityVersion(eq(workflow2.getAppId()), eq(EntityType.WORKFLOW), eq(workflow2.getUuid()),
            eq(workflow2.getName()), eq(ChangeType.CREATED), eq(workflow2.getNotes()));
    verify(entityVersionService, never())
        .lastEntityVersion(eq(workflow2.getAppId()), eq(EntityType.WORKFLOW), eq(workflow2.getUuid()));
  }

  @Test
  public void shouldUpdatePreDeployment() {
    Workflow workflow = aWorkflow()
                            .withUuid(workflowId)
                            .withName(WORKFLOW_NAME)
                            .withAppId(APP_ID)
                            .withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build())
                            .build();
    when(wingsPersistence.get(eq(Workflow.class), eq(appId), eq(workflowId))).thenReturn(workflow);
    when(entityVersionService.lastEntityVersion(
             eq(workflow.getAppId()), eq(EntityType.WORKFLOW), eq(workflow.getUuid())))
        .thenReturn(anEntityVersion().withVersion(2).build());

    PhaseStep phaseStep = aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).withStepsInParallel(true).build();
    PhaseStep updated = workflowService.updatePreDeployment(appId, workflowId, phaseStep);
    verify(updateOperations).set(eq("name"), eq(WORKFLOW_NAME));
    verify(updateOperations).unset(eq("description"));
    verify(entityVersionService, never())
        .newEntityVersion(eq(workflow.getAppId()), eq(EntityType.WORKFLOW), eq(workflow.getUuid()),
            eq(workflow.getName()), eq(ChangeType.CREATED), eq(workflow.getNotes()));
    verify(entityVersionService)
        .lastEntityVersion(eq(workflow.getAppId()), eq(EntityType.WORKFLOW), eq(workflow.getUuid()));
  }

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
