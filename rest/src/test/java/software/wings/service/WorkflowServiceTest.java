package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.CustomOrchestrationWorkflow.CustomOrchestrationWorkflowBuilder.aCustomOrchestrationWorkflow;
import static software.wings.beans.FailureStrategy.FailureStrategyBuilder.aFailureStrategy;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.MultiServiceOrchestrationWorkflow.MultiServiceOrchestrationWorkflowBuilder.aMultiServiceOrchestrationWorkflow;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Pipeline.Builder.aPipeline;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_GROUP_ID;
import static software.wings.utils.WingsTestConstants.ROLE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
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
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Node;
import software.wings.beans.MultiServiceOrchestrationWorkflow;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.RepairActionCode;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.rules.Listeners;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.State;
import software.wings.sm.StateMachine;
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
import java.util.Arrays;
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
  private static String envId = UUIDGenerator.getUuid();
  private static String workflowId = UUIDGenerator.getUuid();

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private AppService appService;
  @Mock private AccountService accountService;
  @Mock private NotificationSetupService notificationSetupService;
  @Mock private Application application;
  @Mock private Account account;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private PipelineService pipelineService;

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

  @Inject private EntityVersionService entityVersionService;

  @InjectMocks @Inject private WorkflowService workflowService;
  @Mock private FieldEnd fieldEnd;

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    when(pluginManager.getExtensions(StateTypeDescriptor.class)).thenReturn(newArrayList());

    when(query.field(anyString())).thenReturn(fieldEnd);
    when(fieldEnd.equal(anyObject())).thenReturn(query);
    when(appService.get(APP_ID)).thenReturn(application);
    when(accountService.get(anyString())).thenReturn(account);
    when(workflowExecutionService.workflowExecutionsRunning(WorkflowType.ORCHESTRATION, APP_ID, WORKFLOW_ID))
        .thenReturn(false);
  }

  /**
   * Should save and read.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldSaveAndRead() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId(APP_ID);
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
  }

  /**
   * Should read simple workflow.
   */
  @Test
  public void shouldReadSimpleWorkflowFromFile() {
    Workflow workflow = workflowService.readLatestSimpleWorkflow(APP_ID, envId);
    assertThat(workflow)
        .isNotNull()
        .extracting("appId", "envId", "workflowType")
        .containsExactly(APP_ID, envId, WorkflowType.SIMPLE);
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

    Graph graph2 =
        JsonUtils.clone(((CustomOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getGraph(), Graph.class);
    graph2.getNodes().add(aNode().withId("n5").withName("http").withX(350).withType(StateType.HTTP.name()).build());
    graph2.getLinks().add(aLink().withId("l3").withFrom("n3").withTo("n5").withType("success").build());

    Workflow updatedWorkflow = workflowService.updateWorkflow(workflow);
    assertThat(updatedWorkflow)
        .isNotNull()
        .isEqualToComparingOnlyGivenFields(workflow, "uuid", "name", "description", "orchestrationWorkflow")
        .hasFieldOrPropertyWithValue("defaultVersion", 2);

    PageResponse<StateMachine> res = wingsPersistence.query(StateMachine.class,
        aPageRequest()
            .addFilter("appId", Operator.EQ, APP_ID)
            .addFilter("originId", Operator.EQ, workflow.getUuid())
            .build());

    assertThat(res).isNotNull().hasSize(2);
    assertThat(res.get(0))
        .isNotNull()
        .hasFieldOrPropertyWithValue("orchestrationWorkflow", updatedWorkflow.getOrchestrationWorkflow())
        .hasFieldOrPropertyWithValue("originId", workflow.getUuid())
        .hasFieldOrPropertyWithValue("originVersion", 2);
    assertThat(res.get(1))
        .isNotNull()
        .hasFieldOrPropertyWithValue("orchestrationWorkflow", workflow.getOrchestrationWorkflow())
        .hasFieldOrPropertyWithValue("originId", workflow.getUuid())
        .hasFieldOrPropertyWithValue("originVersion", 1);
  }

  /**
   * Should delete workflow.
   */
  @Test
  public void shouldDeleteWorkflow() {
    Workflow workflow = createWorkflow();
    String uuid = workflow.getUuid();
    when(pipelineService.listPipelines(any(PageRequest.class))).thenReturn(aPageResponse().build());
    workflowService.deleteWorkflow(APP_ID, uuid);
    workflow = workflowService.readWorkflow(APP_ID, uuid, null);
    assertThat(workflow).isNull();
  }

  @Test
  public void shouldThrowExceptionOnReferencedWorkflowDelete() {
    Workflow workflow = createWorkflow();
    String uuid = workflow.getUuid();
    Pipeline pipeline =
        aPipeline()
            .withName("PIPELINE_NAME")
            .withPipelineStages(asList(new PipelineStage(
                asList(new PipelineStageElement("STAGE", "ENV_STATE", ImmutableMap.of("workflowId", workflowId))))))
            .build();
    when(pipelineService.listPipelines(any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(pipeline)).build());
    assertThatThrownBy(() -> workflowService.deleteWorkflow(APP_ID, uuid))
        .isInstanceOf(WingsException.class)
        .hasMessage(ErrorCode.INVALID_REQUEST.name());
  }

  /**
   * Should delete workflow.
   */
  @Test(expected = WingsException.class)
  public void deleteWorkflowExecutionInProgress() {
    Workflow workflow = createWorkflow();
    String uuid = workflow.getUuid();
    when(workflowExecutionService.workflowExecutionsRunning(WorkflowType.ORCHESTRATION, APP_ID, uuid)).thenReturn(true);
    when(pipelineService.listPipelines(any(PageRequest.class))).thenReturn(aPageResponse().build());
    workflowService.deleteWorkflow(APP_ID, uuid);
    workflow = workflowService.readWorkflow(APP_ID, uuid, null);
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
                            .withAppId(APP_ID)
                            .withName("workflow1")
                            .withDescription("Sample Workflow")
                            .withWorkflowType(WorkflowType.ORCHESTRATION)
                            .withOrchestrationWorkflow(orchestrationWorkflow)
                            .build();

    workflow = workflowService.createWorkflow(workflow);
    assertThat(workflow).isNotNull().hasFieldOrProperty("uuid").hasFieldOrPropertyWithValue("defaultVersion", 1);

    PageResponse<StateMachine> res = wingsPersistence.query(StateMachine.class,
        aPageRequest()
            .addFilter("appId", Operator.EQ, APP_ID)
            .addFilter("originId", Operator.EQ, workflow.getUuid())
            .build());

    assertThat(res).isNotNull().hasSize(1);
    assertThat(res.get(0))
        .isNotNull()
        .hasFieldOrPropertyWithValue("orchestrationWorkflow", orchestrationWorkflow)
        .hasFieldOrPropertyWithValue("originId", workflow.getUuid())
        .hasFieldOrPropertyWithValue("originVersion", 1);
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
    assertThat(stencils).isNotNull().hasSize(4).containsKeys(StateTypeScope.ORCHESTRATION_STENCILS,
        StateTypeScope.PIPELINE_STENCILS, StateTypeScope.NONE, StateTypeScope.COMMON);
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

    PageResponse<StateMachine> res = wingsPersistence.query(StateMachine.class,
        aPageRequest()
            .addFilter("appId", Operator.EQ, APP_ID)
            .addFilter("originId", Operator.EQ, workflow.getUuid())
            .build());

    assertThat(res).isNotNull().hasSize(1);
    assertThat(res.get(0)).isNotNull().hasFieldOrPropertyWithValue("orchestrationWorkflow", orchestrationWorkflow);

    logger.info(JsonUtils.asJson(workflow2));
  }

  @Test
  public void shouldCreateBasicDeploymentWorkflow() {
    Workflow workflow = createBasicWorkflow();

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());

    Role role = aRole()
                    .withRoleType(RoleType.ACCOUNT_ADMIN)
                    .withUuid(ROLE_ID)
                    .withAccountId(application.getAccountId())
                    .build();
    List<NotificationGroup> notificationGroups = Arrays.asList(aNotificationGroup()
                                                                   .withUuid(NOTIFICATION_GROUP_ID)
                                                                   .withAccountId(application.getAccountId())
                                                                   .withRole(role)
                                                                   .build());
    when(notificationSetupService.listNotificationGroups(
             application.getAccountId(), RoleType.ACCOUNT_ADMIN.getDisplayName()))
        .thenReturn(notificationGroups);

    Workflow workflow2 = workflowService.createWorkflow(workflow);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid").hasFieldOrPropertyWithValue("appId", APP_ID);

    BasicOrchestrationWorkflow orchestrationWorkflow =
        (BasicOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow)
        .isNotNull()
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps")
        .hasFieldOrProperty("graph")
        .hasFieldOrProperty("notificationRules")
        .hasFieldOrProperty("failureStrategies");
    assertThat(orchestrationWorkflow.getGraph()).isNotNull();
    assertThat(orchestrationWorkflow.getGraph().getNodes())
        .extracting("id")
        .contains(orchestrationWorkflow.getPostDeploymentSteps().getUuid(),
            orchestrationWorkflow.getPostDeploymentSteps().getUuid());

    assertThat(orchestrationWorkflow.getGraph().getSubworkflows())
        .containsKeys(orchestrationWorkflow.getPostDeploymentSteps().getUuid(),
            orchestrationWorkflow.getPostDeploymentSteps().getUuid());

    assertThat(orchestrationWorkflow.getNotificationRules()).isNotNull();
    assertThat(orchestrationWorkflow.getNotificationRules().get(0)).isNotNull();
    assertThat(orchestrationWorkflow.getNotificationRules().get(0).getConditions()).isNotNull();
    assertThat(orchestrationWorkflow.getNotificationRules().get(0).getExecutionScope())
        .isEqualTo(ExecutionScope.WORKFLOW);

    assertThat(orchestrationWorkflow.getFailureStrategies()).isNotNull();
    assertThat(orchestrationWorkflow.getFailureStrategies().get(0)).isNotNull();
    assertThat(orchestrationWorkflow.getFailureStrategies().get(0).getRepairActionCode())
        .isEqualTo(RepairActionCode.ROLLBACK_WORKFLOW);

    PageResponse<StateMachine> res = wingsPersistence.query(StateMachine.class,
        aPageRequest()
            .addFilter("appId", Operator.EQ, APP_ID)
            .addFilter("originId", Operator.EQ, workflow.getUuid())
            .build());

    assertThat(res).isNotNull().hasSize(1);
    assertThat(res.get(0)).isNotNull().hasFieldOrPropertyWithValue("orchestrationWorkflow", orchestrationWorkflow);

    logger.info(JsonUtils.asJson(workflow2));
  }

  private Workflow createBasicWorkflow() {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withServiceId(SERVICE_ID)
        .withInfraMappingId(INFRA_MAPPING_ID)
        .withOrchestrationWorkflow(
            aBasicOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  @Test
  public void shouldCreateMultiServiceWorkflow() {
    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withOrchestrationWorkflow(
                aMultiServiceOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid").hasFieldOrPropertyWithValue("appId", APP_ID);

    MultiServiceOrchestrationWorkflow orchestrationWorkflow =
        (MultiServiceOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
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

    PageResponse<StateMachine> res = wingsPersistence.query(StateMachine.class,
        aPageRequest()
            .addFilter("appId", Operator.EQ, APP_ID)
            .addFilter("originId", Operator.EQ, workflow.getUuid())
            .build());

    assertThat(res).isNotNull().hasSize(1);
    assertThat(res.get(0)).isNotNull().hasFieldOrPropertyWithValue("orchestrationWorkflow", orchestrationWorkflow);

    logger.info(JsonUtils.asJson(workflow2));
  }

  @Test
  public void shouldValidateWorkflow() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .addWorkflowPhase(
                        aWorkflowPhase()
                            .withServiceId(SERVICE_ID)
                            .withInfraMappingId(INFRA_MAPPING_ID)
                            .addPhaseStep(aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, Constants.DEPLOY_CONTAINERS)
                                              .addStep(aNode()
                                                           .withId(getUuid())
                                                           .withType(ECS_SERVICE_DEPLOY.name())
                                                           .withName(Constants.UPGRADE_CONTAINERS)
                                                           .build())
                                              .build())
                            .build())
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
        .hasFieldOrProperty("graph")
        .hasFieldOrPropertyWithValue(
            "validationMessage", String.format(Constants.WORKFLOW_VALIDATION_MESSAGE, "[Phase 1]"));
    assertThat(orchestrationWorkflow.getWorkflowPhases().get(0))
        .isNotNull()
        .hasFieldOrPropertyWithValue("valid", false)
        .hasFieldOrPropertyWithValue("validationMessage",
            String.format(Constants.PHASE_VALIDATION_MESSAGE, asList(Constants.DEPLOY_CONTAINERS)));
    assertThat(orchestrationWorkflow.getWorkflowPhases().get(0).getPhaseSteps().get(0))
        .isNotNull()
        .hasFieldOrPropertyWithValue("valid", false)
        .hasFieldOrPropertyWithValue("validationMessage",
            String.format(Constants.PHASE_STEP_VALIDATION_MESSAGE, asList(Constants.UPGRADE_CONTAINERS)));
    assertThat(orchestrationWorkflow.getWorkflowPhases()
                   .get(0)
                   .getPhaseSteps()
                   .get(0)
                   .getSteps()
                   .stream()
                   .filter(n -> n.getName().equals(Constants.UPGRADE_CONTAINERS))
                   .findFirst()
                   .get())
        .isNotNull()
        .hasFieldOrPropertyWithValue("valid", false)
        .hasFieldOrPropertyWithValue("validationMessage",
            String.format(Constants.STEP_VALIDATION_MESSAGE, asList("commandName, instanceCount")));
    assertThat(orchestrationWorkflow.getWorkflowPhases()
                   .get(0)
                   .getPhaseSteps()
                   .get(0)
                   .getSteps()
                   .get(0)
                   .getInValidFieldMessages())
        .isNotNull()
        .hasSize(2)
        .containsKeys("commandName", "instanceCount");
  }

  @Test
  public void shouldUpdateBasic() {
    Workflow workflow1 = createCanaryWorkflow();
    String name2 = "Name2";

    Workflow workflow2 = aWorkflow().withAppId(APP_ID).withUuid(workflow1.getUuid()).withName(name2).build();

    workflowService.updateWorkflow(workflow2, null);

    Workflow orchestrationWorkflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull().hasFieldOrPropertyWithValue("name", name2);
  }

  @Test
  public void shouldUpdatePreDeployment() {
    Workflow workflow1 = createCanaryWorkflow();

    PhaseStep phaseStep = aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).withStepsInParallel(true).build();
    PhaseStep updated = workflowService.updatePreDeployment(workflow1.getAppId(), workflow1.getUuid(), phaseStep);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull().hasFieldOrPropertyWithValue("uuid", workflow1.getUuid());
    assertThat(workflow2.getOrchestrationWorkflow()).isNotNull().isInstanceOf(CanaryOrchestrationWorkflow.class);
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getPreDeploymentSteps())
        .isNotNull()
        .isEqualTo(phaseStep);
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getGraph()).isNotNull();
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getGraph().getNodes())
        .isNotNull()
        .extracting("id")
        .contains(phaseStep.getUuid());
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getGraph().getSubworkflows())
        .isNotNull()
        .containsKeys(phaseStep.getUuid());
  }

  public Workflow createCanaryWorkflow() {
    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid");
    assertThat(workflow2.getOrchestrationWorkflow())
        .isNotNull()
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps")
        .hasFieldOrProperty("graph");
    return workflow2;
  }

  public Workflow createMultiServiceWorkflow() {
    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aMultiServiceOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid");
    assertThat(workflow2.getOrchestrationWorkflow())
        .isNotNull()
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps")
        .hasFieldOrProperty("graph");
    return workflow2;
  }

  @Test
  public void shouldUpdatePostDeployment() {
    Workflow workflow1 = createCanaryWorkflow();

    PhaseStep phaseStep = aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).withStepsInParallel(true).build();
    PhaseStep updated = workflowService.updatePostDeployment(workflow1.getAppId(), workflow1.getUuid(), phaseStep);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull().hasFieldOrPropertyWithValue("uuid", workflow1.getUuid());
    assertThat(workflow2.getOrchestrationWorkflow()).isNotNull().isInstanceOf(CanaryOrchestrationWorkflow.class);
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getPostDeploymentSteps())
        .isNotNull()
        .isEqualTo(phaseStep);
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getGraph()).isNotNull();
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getGraph().getNodes())
        .isNotNull()
        .extracting("id")
        .contains(phaseStep.getUuid());
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getGraph().getSubworkflows())
        .isNotNull()
        .containsKeys(phaseStep.getUuid());
  }

  @Test
  public void shouldCreateWorkflowPhase() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();
    WorkflowPhase workflowPhase =
        aWorkflowPhase().withServiceId(SERVICE_ID).withInfraMappingId(INFRA_MAPPING_ID).build();

    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);
    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    assertThat(workflow2).isNotNull().hasFieldOrPropertyWithValue("uuid", workflow1.getUuid());
    assertThat(workflow2.getOrchestrationWorkflow()).isNotNull().isInstanceOf(CanaryOrchestrationWorkflow.class);
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getGraph()).isNotNull();
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getGraph().getNodes())
        .isNotNull()
        .extracting("id")
        .contains(workflowPhase.getUuid());
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getGraph().getSubworkflows())
        .isNotNull()
        .containsKeys(workflowPhase.getUuid());

    List<WorkflowPhase> workflowPhases =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    assertThat(workflowPhases)
        .isNotNull()
        .hasSize(((CanaryOrchestrationWorkflow) workflow1.getOrchestrationWorkflow()).getWorkflowPhases().size() + 1);

    WorkflowPhase workflowPhase2 = workflowPhases.get(workflowPhases.size() - 1);
    assertThat(workflowPhase2).isNotNull().hasFieldOrPropertyWithValue("name", Constants.PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase2.getPhaseSteps()).isNotNull().hasSize(6);
  }

  @Test
  public void shouldUpdateWorkflowPhase() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withName("phase1").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withName("phase2").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("phase2-changed");

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase2);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    List<WorkflowPhase> workflowPhases3 =
        ((CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhase3 = workflowPhases3.get(workflowPhases3.size() - 1);
    assertThat(workflowPhase3).isEqualToComparingOnlyGivenFields(workflowPhase2, "uuid", "name");
  }

  @Test
  public void shouldCreateMultiServiceWorkflowPhase() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createMultiServiceWorkflow();
    WorkflowPhase workflowPhase =
        aWorkflowPhase().withServiceId(SERVICE_ID).withInfraMappingId(INFRA_MAPPING_ID).build();

    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);
    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    assertThat(workflow2).isNotNull().hasFieldOrPropertyWithValue("uuid", workflow1.getUuid());
    assertThat(workflow2.getOrchestrationWorkflow()).isNotNull().isInstanceOf(CanaryOrchestrationWorkflow.class);
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getGraph()).isNotNull();
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getGraph().getNodes())
        .isNotNull()
        .extracting("id")
        .contains(workflowPhase.getUuid());
    assertThat(((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getGraph().getSubworkflows())
        .isNotNull()
        .containsKeys(workflowPhase.getUuid());

    List<WorkflowPhase> workflowPhases =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    assertThat(workflowPhases)
        .isNotNull()
        .hasSize(((CanaryOrchestrationWorkflow) workflow1.getOrchestrationWorkflow()).getWorkflowPhases().size() + 1);

    WorkflowPhase workflowPhase2 = workflowPhases.get(workflowPhases.size() - 1);
    assertThat(workflowPhase2).isNotNull().hasFieldOrPropertyWithValue("name", Constants.PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase2.getPhaseSteps()).isNotNull().hasSize(6);
  }

  @Test
  public void shouldUpdateMultiServiceWorkflowPhase() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createMultiServiceWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withName("phase1").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withName("phase2").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("phase2-changed");

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase2);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    List<WorkflowPhase> workflowPhases3 =
        ((CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhase3 = workflowPhases3.get(workflowPhases3.size() - 1);
    assertThat(workflowPhase3).isEqualToComparingOnlyGivenFields(workflowPhase2, "uuid", "name");
  }

  @Test
  public void shouldDeleteWorkflowPhase() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withName("phase1")
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withName("phase2")
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .build())
                    .build())
            .build();

    Workflow workflow1 = workflowService.createWorkflow(workflow);
    assertThat(workflow1).isNotNull().hasFieldOrProperty("uuid");

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhases2 = orchestrationWorkflow.getWorkflowPhases();
    WorkflowPhase workflowPhase = workflowPhases2.get(workflowPhases2.size() - 2);

    assertThat(orchestrationWorkflow.getGraph().getSubworkflows()).isNotNull().containsKeys(workflowPhase.getUuid());
    workflowPhase.getPhaseSteps().forEach(phaseStep -> {
      assertThat(orchestrationWorkflow.getGraph().getSubworkflows()).containsKeys(phaseStep.getUuid());
    });

    workflowService.deleteWorkflowPhase(APP_ID, workflow1.getUuid(), workflowPhase.getUuid());

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow3).isNotNull();

    CanaryOrchestrationWorkflow orchestrationWorkflow3 =
        (CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow3.getGraph().getSubworkflows())
        .isNotNull()
        .doesNotContainKeys(workflowPhase.getUuid());
    workflowPhase.getPhaseSteps().forEach(phaseStep -> {
      assertThat(orchestrationWorkflow3.getGraph().getSubworkflows()).doesNotContainKeys(phaseStep.getUuid());
    });
  }

  @Test
  public void shouldUpdateWorkflowPhaseRollback() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withName("phase1").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withName("phase2").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow).isNotNull();

    assertThat(orchestrationWorkflow.getRollbackWorkflowPhaseIdMap())
        .isNotNull()
        .containsKeys(workflowPhase2.getUuid());

    WorkflowPhase rollbackPhase = orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(workflowPhase2.getUuid());
    assertThat(rollbackPhase).isNotNull();

    int size = rollbackPhase.getPhaseSteps().size();
    rollbackPhase.getPhaseSteps().remove(0);

    workflowService.updateWorkflowPhaseRollback(APP_ID, workflow2.getUuid(), workflowPhase2.getUuid(), rollbackPhase);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    CanaryOrchestrationWorkflow orchestrationWorkflow3 =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();

    assertThat(orchestrationWorkflow3.getRollbackWorkflowPhaseIdMap())
        .isNotNull()
        .containsKeys(orchestrationWorkflow3.getWorkflowPhases().get(0).getUuid());
    WorkflowPhase rollbackPhase2 = orchestrationWorkflow3.getRollbackWorkflowPhaseIdMap().get(workflowPhase2.getUuid());
    assertThat(rollbackPhase2).isNotNull().hasFieldOrPropertyWithValue("uuid", rollbackPhase.getUuid());
    assertThat(rollbackPhase2.getPhaseSteps()).hasSize(size - 1);
  }

  @Test
  public void shouldUpdateNode() {
    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT)
                                                .addStep(aNode()
                                                             .withType("HTTP")
                                                             .withName("http")
                                                             .addProperty("url", "http://www.google.com")
                                                             .build())
                                                .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow);
    assertThat(workflow2).isNotNull();
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow).isNotNull();
    assertThat(orchestrationWorkflow.getGraph()).isNotNull();

    Graph graph =
        orchestrationWorkflow.getGraph().getSubworkflows().get(orchestrationWorkflow.getPreDeploymentSteps().getUuid());
    assertThat(graph).isNotNull();
    assertThat(graph.getNodes()).isNotNull().hasSize(1);
    Node node = graph.getNodes().get(0);
    assertThat(node).isNotNull().hasFieldOrProperty("id").hasFieldOrPropertyWithValue("type", "HTTP");
    assertThat(node.getProperties()).isNotNull().containsKey("url").containsValue("http://www.google.com");
    node.getProperties().put("url", "http://www.yahoo.com");

    workflowService.updateGraphNode(
        workflow2.getAppId(), workflow2.getUuid(), orchestrationWorkflow.getPreDeploymentSteps().getUuid(), node);

    Workflow workflow3 = workflowService.readWorkflow(workflow2.getAppId(), workflow2.getUuid());
    assertThat(workflow3).isNotNull().hasFieldOrPropertyWithValue("uuid", workflow2.getUuid());
    CanaryOrchestrationWorkflow orchestrationWorkflow3 =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow3)
        .hasFieldOrProperty("graph")
        .hasFieldOrProperty("preDeploymentSteps")
        .hasFieldOrProperty("postDeploymentSteps");

    assertThat(orchestrationWorkflow3.getGraph().getSubworkflows())
        .isNotNull()
        .containsKeys(orchestrationWorkflow.getPreDeploymentSteps().getUuid())
        .containsKeys(orchestrationWorkflow.getPostDeploymentSteps().getUuid());

    graph = orchestrationWorkflow3.getGraph().getSubworkflows().get(
        orchestrationWorkflow.getPreDeploymentSteps().getUuid());
    assertThat(graph).isNotNull();
    assertThat(graph.getNodes()).isNotNull().hasSize(1);
    node = graph.getNodes().get(0);
    assertThat(node).isNotNull().hasFieldOrProperty("id").hasFieldOrPropertyWithValue("type", "HTTP");
    assertThat(node.getProperties()).isNotNull().containsKey("url").containsValue("http://www.yahoo.com");
  }

  @Test
  public void shouldHaveGraph() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withName("phase1").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withName("phase2").withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();
    CanaryOrchestrationWorkflow orchestrationWorkflow2 =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();

    assertThat(orchestrationWorkflow2.getWorkflowPhases()).isNotNull().hasSize(2);

    workflowPhase2 = orchestrationWorkflow2.getWorkflowPhases().get(1);

    workflowService.updateWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow3).isNotNull();
    CanaryOrchestrationWorkflow orchestrationWorkflow3 =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();

    assertThat(orchestrationWorkflow3.getWorkflowPhases()).isNotNull().hasSize(2);

    Graph graph = orchestrationWorkflow3.getGraph();
    assertThat(graph).isNotNull();
    assertThat(graph.getNodes()).isNotNull().hasSize(6).doesNotContainNull();
    assertThat(graph.getLinks()).isNotNull().hasSize(3).doesNotContainNull();
    assertThat(graph.getNodes().get(0).getId()).isEqualTo(orchestrationWorkflow3.getPreDeploymentSteps().getUuid());
    assertThat(graph.getNodes().get(1).getId()).isEqualTo(orchestrationWorkflow3.getWorkflowPhaseIds().get(0));
    assertThat(graph.getNodes().get(3).getId()).isEqualTo(orchestrationWorkflow3.getWorkflowPhaseIds().get(1));
    assertThat(graph.getNodes().get(5).getId()).isEqualTo(orchestrationWorkflow3.getPostDeploymentSteps().getUuid());
    logger.info("Graph Nodes: {}", graph.getNodes());
    assertThat(graph.getSubworkflows())
        .isNotNull()
        .containsKeys(orchestrationWorkflow3.getPreDeploymentSteps().getUuid(),
            orchestrationWorkflow3.getWorkflowPhaseIds().get(0), orchestrationWorkflow3.getWorkflowPhaseIds().get(1),
            orchestrationWorkflow3.getPostDeploymentSteps().getUuid());
  }

  @Test
  public void shouldUpdateNotificationRules() {
    Workflow workflow1 = createCanaryWorkflow();

    List<NotificationRule> notificationRules = newArrayList(aNotificationRule().build());
    List<NotificationRule> updated =
        workflowService.updateNotificationRules(workflow1.getAppId(), workflow1.getUuid(), notificationRules);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow).isNotNull().hasFieldOrPropertyWithValue("notificationRules", notificationRules);
  }

  @Test
  public void shouldUpdateFailureStrategies() {
    Workflow workflow1 = createCanaryWorkflow();

    List<FailureStrategy> failureStrategies = newArrayList(aFailureStrategy().build());
    List<FailureStrategy> updated =
        workflowService.updateFailureStrategies(workflow1.getAppId(), workflow1.getUuid(), failureStrategies);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow).isNotNull().hasFieldOrPropertyWithValue("failureStrategies", failureStrategies);
  }

  @Test
  public void shouldUpdateUserVariables() {
    Workflow workflow1 = createCanaryWorkflow();
    List<Variable> userVariables = newArrayList(aVariable().withName("name1").withValue("value1").build());
    List<Variable> updated =
        workflowService.updateUserVariables(workflow1.getAppId(), workflow1.getUuid(), userVariables);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow).isNotNull().hasFieldOrPropertyWithValue("userVariables", userVariables);
  }

  @Test
  public void shouldCreateComplexWorkflow() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    Workflow workflow1 =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withName("Phase1")
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow1);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid");

    Workflow workflow3 = workflowService.readWorkflow(workflow2.getAppId(), workflow2.getUuid());
    assertThat(workflow3).isNotNull();
    CanaryOrchestrationWorkflow orchestrationWorkflow3 =
        (CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow3.getWorkflowPhases()).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = orchestrationWorkflow3.getWorkflowPhases().get(0);
    PhaseStep deployPhaseStep = workflowPhase.getPhaseSteps()
                                    .stream()
                                    .filter(ps -> ps.getPhaseStepType() == PhaseStepType.DEPLOY_SERVICE)
                                    .collect(Collectors.toList())
                                    .get(0);

    deployPhaseStep.getSteps().add(
        aNode().withType("HTTP").withName("http").addProperty("url", "www.google.com").build());

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase);

    Workflow workflow4 = workflowService.readWorkflow(workflow2.getAppId(), workflow2.getUuid());
    workflowService.deleteWorkflowPhase(workflow4.getAppId(), workflow4.getUuid(), workflowPhase.getUuid());

    Workflow workflow5 = workflowService.readWorkflow(workflow2.getAppId(), workflow2.getUuid());
    assertThat(workflow5).isNotNull();
  }

  @Test
  public void shouldTemplatizeWorkflow() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(aService().withUuid(SERVICE_ID).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    Workflow workflow1 =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withName("Phase1")
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow1);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid");

    Workflow templatizedWorkflow =
        workflowService.templatizeWorkflow(workflow2.getAppId(), workflow2.getUuid(), workflow2);
    assertThat(templatizedWorkflow).isNotNull();
    assertThat(templatizedWorkflow.isTemplatized()).isTrue();

    CanaryOrchestrationWorkflow orchestrationWorkflow3 =
        (CanaryOrchestrationWorkflow) templatizedWorkflow.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow3.getWorkflowPhases()).isNotNull().hasSize(1);
  }
}
