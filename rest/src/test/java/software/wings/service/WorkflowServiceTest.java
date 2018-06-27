package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.CustomOrchestrationWorkflow.CustomOrchestrationWorkflowBuilder.aCustomOrchestrationWorkflow;
import static software.wings.beans.EntityType.APPDYNAMICS_CONFIGID;
import static software.wings.beans.EntityType.ELK_CONFIGID;
import static software.wings.beans.EntityType.ELK_INDICES;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphLink.Builder.aLink;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.MultiServiceOrchestrationWorkflow.MultiServiceOrchestrationWorkflowBuilder.aMultiServiceOrchestrationWorkflow;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.INFRASTRUCTURE_NODE;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.SELECT_NODE;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.TemplateExpression.Builder.aTemplateExpression;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.common.Constants.ARTIFACT_S3_BUCKET_EXPRESSION;
import static software.wings.common.Constants.ARTIFACT__S3_KEY_EXPRESSION;
import static software.wings.common.Constants.DEPLOY_CONTAINERS;
import static software.wings.common.Constants.PHASE_NAME_PREFIX;
import static software.wings.common.Constants.PHASE_STEP_VALIDATION_MESSAGE;
import static software.wings.common.Constants.PHASE_VALIDATION_MESSAGE;
import static software.wings.common.Constants.STEP_VALIDATION_MESSAGE;
import static software.wings.common.Constants.UPGRADE_CONTAINERS;
import static software.wings.common.Constants.WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE;
import static software.wings.common.Constants.WORKFLOW_VALIDATION_MESSAGE;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.sm.StateType.ARTIFACT_COLLECTION;
import static software.wings.sm.StateType.ECS_SERVICE_DEPLOY;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.sm.StateType.FORK;
import static software.wings.sm.StateType.HTTP;
import static software.wings.sm.StateType.JENKINS;
import static software.wings.sm.StateType.REPEAT;
import static software.wings.utils.ArtifactType.DOCKER;
import static software.wings.utils.ArtifactType.WAR;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID_CHANGED;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID_CHANGED;
import static software.wings.utils.WingsTestConstants.JENKINS_URL;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_GROUP_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.ROLE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_COMMAND_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID_CHANGED;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TARGET_APP_ID;
import static software.wings.utils.WingsTestConstants.TARGET_SERVICE_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.BuildWorkflow;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureCriteria;
import software.wings.beans.FailureStrategy;
import software.wings.beans.FailureType;
import software.wings.beans.Graph;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.MultiServiceOrchestrationWorkflow;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.RepairActionCode;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.stats.CloneMetadata;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.rules.Listeners;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.WorkflowServiceHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
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

import java.util.List;
import java.util.Map;
import javax.validation.ConstraintViolationException;

/**
 * The Class WorkflowServiceTest.
 *
 * @author Rishi
 */
@Listeners(NotifyEventListener.class)
public class WorkflowServiceTest extends WingsBaseTest {
  private static String envId = generateUuid();
  private static String workflowId = generateUuid();

  private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceTest.class);

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
  @Mock private EntityUpdateService entityUpdateService;
  @Mock private YamlDirectoryService yamlDirectoryService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactStream artifactStream;
  @Mock private TriggerService triggerService;
  @Mock private EnvironmentService environmentService;
  @InjectMocks @Inject private WorkflowServiceHelper workflowServiceHelper;

  @Mock @Named("JobScheduler") private QuartzScheduler jobScheduler;

  private StencilPostProcessor stencilPostProcessor =
      mock(StencilPostProcessor.class, (Answer<List<Stencil>>) invocationOnMock -> {
        logger.info("invocationOnMock.getArguments()[0] " + invocationOnMock.getArguments()[0]);
        return (List<Stencil>) invocationOnMock.getArguments()[0];
      });

  @Mock private PluginManager pluginManager;
  @Mock private UpdateOperations<Workflow> updateOperations;

  @Inject private EntityVersionService entityVersionService;

  @InjectMocks @Inject private WorkflowService workflowService;
  @Mock private FieldEnd fieldEnd;

  private Service service = Service.builder().name(SERVICE_NAME).uuid(SERVICE_ID).artifactType(WAR).build();
  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    when(pluginManager.getExtensions(StateTypeDescriptor.class)).thenReturn(newArrayList());

    when(appService.get(APP_ID)).thenReturn(application);
    when(accountService.get(anyString())).thenReturn(account);
    when(workflowExecutionService.workflowExecutionsRunning(WorkflowType.ORCHESTRATION, APP_ID, WORKFLOW_ID))
        .thenReturn(false);
    when(appService.get(TARGET_APP_ID))
        .thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());

    when(environmentService.get(APP_ID, ENV_ID, false))
        .thenReturn(Environment.Builder.anEnvironment().withUuid(ENV_ID).withName(ENV_NAME).withAppId(APP_ID).build());

    when(serviceResourceService.get(APP_ID, SERVICE_ID, false)).thenReturn(service);
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);
    when(serviceResourceService.fetchServicesByUuids(APP_ID, asList(SERVICE_ID))).thenReturn(asList(service));
  }

  /**
   * Should save and read.
   *
   */
  @Test
  public void shouldSaveAndRead() {
    StateMachine sm = new StateMachine();
    sm.setAppId(APP_ID);
    State stateA = new StateSync("stateA" + StaticMap.getUnique());
    sm.addState(stateA);
    StateSync stateB = new StateSync("stateB" + StaticMap.getUnique());
    sm.addState(stateB);
    StateSync stateC = new StateSync("stateC" + StaticMap.getUnique());
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
   * Should create workflow.
   */
  @Test
  public void shouldCreateWorkflow() {
    createWorkflow();
  }

  /**
   * Clone workflow within the same application
   */
  @Test
  public void shouldCloneWorkflow() {
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
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow1);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid");

    Workflow clonedWorkflow =
        workflowService.cloneWorkflow(APP_ID, workflow2.getUuid(), CloneMetadata.builder().workflow(workflow2).build());
    assertThat(clonedWorkflow).isNotNull();
    assertThat(clonedWorkflow.getUuid()).isNotEqualTo(workflow2.getUuid());
    assertThat(clonedWorkflow.getAppId()).isEqualTo(workflow2.getAppId());
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    CanaryOrchestrationWorkflow clonedOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) clonedWorkflow.getOrchestrationWorkflow();
    assertThat(clonedOrchestrationWorkflow).isNotNull();
    assertThat(clonedOrchestrationWorkflow.getOrchestrationWorkflowType())
        .isEqualTo(orchestrationWorkflow.getOrchestrationWorkflowType());

    assertThat(clonedOrchestrationWorkflow.getWorkflowPhases()).isNotNull().hasSize(1);
  }

  /**
   * Clone workflow within the same application
   */
  @Test
  public void shouldCloneWorkflowAcrossApps() {
    when(serviceResourceService.get(TARGET_APP_ID, TARGET_SERVICE_ID))
        .thenReturn(Service.builder().uuid(TARGET_SERVICE_ID).artifactType(WAR).build());
    when(serviceResourceService.get(TARGET_APP_ID, TARGET_SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(TARGET_SERVICE_ID).artifactType(WAR).build());

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
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow1);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid");

    CloneMetadata cloneMetadata = CloneMetadata.builder()
                                      .workflow(workflow2)
                                      .serviceMapping(ImmutableMap.of(SERVICE_ID, TARGET_SERVICE_ID))
                                      .targetAppId(TARGET_APP_ID)
                                      .build();
    Workflow clonedWorkflow = workflowService.cloneWorkflow(APP_ID, workflow2.getUuid(), cloneMetadata);
    assertThat(clonedWorkflow).isNotNull();
    assertThat(clonedWorkflow.getUuid()).isNotEqualTo(workflow2.getUuid());
    assertThat(clonedWorkflow.getAppId()).isEqualTo(TARGET_APP_ID);
    assertThat(clonedWorkflow.getEnvId()).isNullOrEmpty();
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    CanaryOrchestrationWorkflow clonedOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) clonedWorkflow.getOrchestrationWorkflow();
    assertThat(clonedOrchestrationWorkflow).isNotNull();
    assertThat(clonedOrchestrationWorkflow.getOrchestrationWorkflowType())
        .isEqualTo(orchestrationWorkflow.getOrchestrationWorkflowType());
    assertThat(clonedOrchestrationWorkflow.isValid()).isFalse();
    assertThat(clonedOrchestrationWorkflow.getValidationMessage()).startsWith("Environment");
    List<WorkflowPhase> workflowPhases = clonedOrchestrationWorkflow.getWorkflowPhases();
    assertThat(workflowPhases).extracting(workflowPhase -> workflowPhase.getServiceId()).contains(TARGET_SERVICE_ID);
    assertThat(workflowPhases)
        .isNotNull()
        .hasSize(1)
        .extracting(workflowPhase -> workflowPhase.getInfraMappingId())
        .containsNull();
    assertThat(workflowPhases)
        .isNotNull()
        .hasSize(1)
        .extracting(workflowPhase -> workflowPhase.getInfraMappingName())
        .containsNull();
    assertThat(workflowPhases)
        .isNotNull()
        .hasSize(1)
        .extracting(workflowPhase -> workflowPhase.getComputeProviderId())
        .containsNull();
  }

  /**
   * Clone workflow within the same application
   */
  @Test(expected = WingsException.class)
  public void shouldCloneWorkflowAcrossAppsDifferentArtifactType() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(DOCKER).build());
    when(serviceResourceService.get(TARGET_APP_ID, TARGET_SERVICE_ID))
        .thenReturn(Service.builder().uuid(TARGET_SERVICE_ID).artifactType(WAR).build());
    when(serviceResourceService.get(TARGET_APP_ID, TARGET_SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(TARGET_SERVICE_ID).artifactType(WAR).build());

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
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow1);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid");

    CloneMetadata cloneMetadata = CloneMetadata.builder()
                                      .workflow(workflow2)
                                      .serviceMapping(ImmutableMap.of(SERVICE_ID, TARGET_SERVICE_ID))
                                      .targetAppId(TARGET_APP_ID)
                                      .build();
    workflowService.cloneWorkflow(APP_ID, workflow2.getUuid(), cloneMetadata);
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
    graph2.addNode(aGraphNode().withId("n5").withName("http").withType(StateType.HTTP.name()).build());
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
  public void shouldPruneDescendingObjects() {
    workflowService.pruneDescendingEntities(APP_ID, WORKFLOW_ID);
    InOrder inOrder = inOrder(triggerService);
    inOrder.verify(triggerService).pruneByWorkflow(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldThrowExceptionOnReferencedWorkflowDelete() {
    Workflow workflow = createWorkflow();
    String uuid = workflow.getUuid();
    Pipeline pipeline =
        Pipeline.builder()
            .name("PIPELINE_NAME")
            .pipelineStages(
                asList(PipelineStage.builder()
                           .pipelineStageElements(asList(PipelineStageElement.builder()
                                                             .name("STAGE")
                                                             .type(ENV_STATE.name())
                                                             .properties(ImmutableMap.of("workflowId", workflowId))
                                                             .build()))
                           .build()))
            .build();
    when(pipelineService.listPipelines(any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(pipeline)).build());
    assertThatThrownBy(() -> workflowService.deleteWorkflow(APP_ID, uuid))
        .isInstanceOf(WingsException.class)
        .hasMessage(INVALID_REQUEST.name());
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
                      .addNodes(aGraphNode()
                                    .withId("n1")
                                    .withName("stop")
                                    .withType(StateType.ENV_STATE.name())
                                    .withOrigin(true)
                                    .build(),
                          aGraphNode()
                              .withId("n2")
                              .withName("wait")
                              .withType(StateType.WAIT.name())
                              .addProperty("duration", 1l)
                              .build(),
                          aGraphNode().withId("n3").withName("start").withType(StateType.ENV_STATE.name()).build())
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

  @Test
  public void stencils() throws IllegalArgumentException {
    Map<StateTypeScope, List<Stencil>> stencils = workflowService.stencils(APP_ID, null, null);
    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(4).containsKeys(StateTypeScope.ORCHESTRATION_STENCILS,
        StateTypeScope.PIPELINE_STENCILS, StateTypeScope.NONE, StateTypeScope.COMMON);
    assertThat(stencils.get(StateTypeScope.ORCHESTRATION_STENCILS))
        .extracting(Stencil::getType)
        .contains("REPEAT", "FORK");
  }

  @Test
  public void stencilsForPipeline() throws IllegalArgumentException {
    Map<StateTypeScope, List<Stencil>> stencils =
        workflowService.stencils(APP_ID, null, null, StateTypeScope.PIPELINE_STENCILS);
    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(1).containsKeys(StateTypeScope.PIPELINE_STENCILS);
    assertThat(stencils.get(StateTypeScope.PIPELINE_STENCILS))
        .extracting(Stencil::getType)
        .contains("APPROVAL", "ENV_STATE")
        .doesNotContain("REPEAT", "FORK");
  }

  @Test
  public void stencilsForOrchestration() throws IllegalArgumentException {
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
  public void stencilsForOrchestrationFilterWorkflow() throws IllegalArgumentException {
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
    Map<StateTypeScope, List<Stencil>> stencils =
        workflowService.stencils(APP_ID, workflow2.getUuid(), null, StateTypeScope.ORCHESTRATION_STENCILS);

    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(1).containsKeys(StateTypeScope.ORCHESTRATION_STENCILS);
    assertThat(stencils.get(StateTypeScope.ORCHESTRATION_STENCILS))
        .extracting(Stencil::getType)
        .doesNotContain("BUILD", "ENV_STATE")
        .contains("REPEAT", "FORK", "HTTP");
  }

  @Test
  public void stencilsForBuildWorkflow() throws IllegalArgumentException {
    Workflow workflow = aWorkflow()
                            .withName(WORKFLOW_NAME)
                            .withAppId(APP_ID)
                            .withOrchestrationWorkflow(aBuildOrchestrationWorkflow().build())
                            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid").hasFieldOrPropertyWithValue("appId", APP_ID);
    Map<StateTypeScope, List<Stencil>> stencils =
        workflowService.stencils(APP_ID, workflow2.getUuid(), null, StateTypeScope.ORCHESTRATION_STENCILS);

    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(1).containsKeys(StateTypeScope.ORCHESTRATION_STENCILS);
    assertThat(stencils.get(StateTypeScope.ORCHESTRATION_STENCILS))
        .extracting(Stencil::getType)
        .doesNotContain("BUILD", "ENV_STATE")
        .contains(REPEAT.name(), FORK.name(), HTTP.name(), ARTIFACT_COLLECTION.name());
  }

  @Test
  public void stencilsForOrchestrationFilterWorkflowPhase() throws IllegalArgumentException {
    Workflow workflow =
        aWorkflow()
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
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withInfraMappingType(InfrastructureMappingType.AWS_SSH.name())
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    ServiceCommand serviceCommand = aServiceCommand()
                                        .withServiceId(SERVICE_ID)
                                        .withUuid(SERVICE_COMMAND_ID)
                                        .withAppId(APP_ID)
                                        .withName("START")
                                        .withCommand(aCommand()
                                                         .withName("START")
                                                         .addCommandUnits(anExecCommandUnit()
                                                                              .withCommandPath("/home/xxx/tomcat")
                                                                              .withCommandString("bin/startup.sh")
                                                                              .build())
                                                         .build())
                                        .build();

    when(serviceResourceService.get(APP_ID, SERVICE_ID, true))
        .thenReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(ImmutableList.of(serviceCommand)).build());

    when(serviceResourceService.get(APP_ID, SERVICE_ID))
        .thenReturn(Service.builder().uuid(SERVICE_ID).serviceCommands(ImmutableList.of(serviceCommand)).build());

    Workflow workflow2 = workflowService.createWorkflow(workflow);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid").hasFieldOrPropertyWithValue("appId", APP_ID);

    BasicOrchestrationWorkflow basicOrchestrationWorkflow =
        (BasicOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    Map<StateTypeScope, List<Stencil>> stencils = workflowService.stencils(APP_ID, workflow2.getUuid(),
        basicOrchestrationWorkflow.getWorkflowPhases().get(0).getUuid(), StateTypeScope.ORCHESTRATION_STENCILS);

    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(1).containsKeys(StateTypeScope.ORCHESTRATION_STENCILS);
    assertThat(stencils.get(StateTypeScope.ORCHESTRATION_STENCILS))
        .extracting(Stencil::getType)
        .doesNotContain("BUILD", "ENV_STATE")
        .contains("REPEAT", "FORK", "HTTP");
  }

  @Test
  public void shouldCreateCanaryWorkflow() {
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

    assertThat(workflow2.getKeywords())
        .isNotNull()
        .contains(workflow.getName().toLowerCase())
        .contains(WorkflowType.ORCHESTRATION.name().toLowerCase());

    workflow2 = workflowService.readWorkflow(workflow2.getAppId(), workflow2.getUuid());
    assertThat(workflow2.getKeywords())
        .isNotNull()
        .contains(workflow.getName().toLowerCase())
        .contains(WorkflowType.ORCHESTRATION.name().toLowerCase())
        .contains(OrchestrationWorkflowType.CANARY.name().toLowerCase());

    logger.info(JsonUtils.asJson(workflow2));
  }

  @Test
  public void shouldCreateBasicDeploymentWorkflow() {
    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withEnvId(ENV_ID)
            .withAppId(APP_ID)
            .withServiceId(SERVICE_ID)
            .withInfraMappingId(INFRA_MAPPING_ID)
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    Role role = aRole()
                    .withRoleType(RoleType.ACCOUNT_ADMIN)
                    .withUuid(ROLE_ID)
                    .withAccountId(application.getAccountId())
                    .build();
    List<NotificationGroup> notificationGroups = asList(aNotificationGroup()
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

    workflow2 = workflowService.readWorkflow(workflow2.getAppId(), workflow2.getUuid());
    assertThat(workflow2.getKeywords())
        .isNotNull()
        .isNotNull()
        .contains(workflow.getName().toLowerCase())
        .contains(WorkflowType.ORCHESTRATION.name().toLowerCase())
        .contains(OrchestrationWorkflowType.BASIC.name().toLowerCase())
        .contains(ENV_NAME.toLowerCase())
        .contains(SERVICE_NAME.toLowerCase());

    logger.info(JsonUtils.asJson(workflow2));
  }

  private Workflow createBasicWorkflow() {
    Workflow workflow =
        aWorkflow()
            .withEnvId(ENV_ID)
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

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    Role role = aRole()
                    .withRoleType(RoleType.ACCOUNT_ADMIN)
                    .withUuid(ROLE_ID)
                    .withAccountId(application.getAccountId())
                    .build();
    List<NotificationGroup> notificationGroups = asList(aNotificationGroup()
                                                            .withUuid(NOTIFICATION_GROUP_ID)
                                                            .withAccountId(application.getAccountId())
                                                            .withRole(role)
                                                            .build());
    when(notificationSetupService.listNotificationGroups(
             application.getAccountId(), RoleType.ACCOUNT_ADMIN.getDisplayName()))
        .thenReturn(notificationGroups);

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
  public void shouldCreateMultiServiceWorkflow() {
    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
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

    assertThat(workflow2.getKeywords())
        .isNotNull()
        .isNotNull()
        .contains(workflow.getName().toLowerCase())
        .contains(WorkflowType.ORCHESTRATION.name().toLowerCase());

    logger.info(JsonUtils.asJson(workflow2));
  }

  @Test
  public void shouldValidateWorkflow() {
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
            .withEnvId(ENV_ID)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withServiceId(SERVICE_ID)
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .addPhaseStep(aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
                                                            .addStep(aGraphNode()
                                                                         .withId(generateUuid())
                                                                         .withType(ECS_SERVICE_DEPLOY.name())
                                                                         .withName(UPGRADE_CONTAINERS)
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
        .hasFieldOrPropertyWithValue("validationMessage", format(WORKFLOW_VALIDATION_MESSAGE, "[Phase 1]"));
    assertThat(orchestrationWorkflow.getWorkflowPhases().get(0))
        .isNotNull()
        .hasFieldOrPropertyWithValue("valid", false)
        .hasFieldOrPropertyWithValue("validationMessage", format(PHASE_VALIDATION_MESSAGE, asList(DEPLOY_CONTAINERS)));
    assertThat(orchestrationWorkflow.getWorkflowPhases().get(0).getPhaseSteps().get(0))
        .isNotNull()
        .hasFieldOrPropertyWithValue("valid", false)
        .hasFieldOrPropertyWithValue(
            "validationMessage", format(PHASE_STEP_VALIDATION_MESSAGE, asList(UPGRADE_CONTAINERS)));
    assertThat(orchestrationWorkflow.getWorkflowPhases()
                   .get(0)
                   .getPhaseSteps()
                   .get(0)
                   .getSteps()
                   .stream()
                   .filter(n -> n.getName().equals(UPGRADE_CONTAINERS))
                   .findFirst()
                   .get())
        .isNotNull()
        .hasFieldOrPropertyWithValue("valid", false)
        .hasFieldOrPropertyWithValue("validationMessage", format(STEP_VALIDATION_MESSAGE, asList("instanceCount")));
    assertThat(orchestrationWorkflow.getWorkflowPhases()
                   .get(0)
                   .getPhaseSteps()
                   .get(0)
                   .getSteps()
                   .get(0)
                   .getInValidFieldMessages())
        .isNotNull()
        .hasSize(1)
        .containsKeys("instanceCount");
  }

  @Test
  public void shouldUpdateCanary() {
    Workflow workflow1 = createCanaryWorkflow();
    String name2 = "Name2";

    Workflow workflow2 = aWorkflow().withAppId(APP_ID).withUuid(workflow1.getUuid()).withName(name2).build();

    workflowService.updateWorkflow(workflow2, null);

    Workflow orchestrationWorkflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(orchestrationWorkflow3).isNotNull().hasFieldOrPropertyWithValue("name", name2);
    assertThat(orchestrationWorkflow3.getKeywords())
        .isNotNull()
        .isNotNull()
        .contains(orchestrationWorkflow3.getName().toLowerCase())
        .contains(WorkflowType.ORCHESTRATION.name().toLowerCase())
        .contains(OrchestrationWorkflowType.CANARY.name().toLowerCase())
        .contains(ENV_NAME.toLowerCase());
  }

  @Test
  public void shouldUpdateBasicDeploymentEnvironment() {
    Workflow workflow1 = createBasicWorkflow();
    String name2 = "Name2";

    Workflow workflow2 =
        aWorkflow().withAppId(APP_ID).withEnvId(ENV_ID_CHANGED).withUuid(workflow1.getUuid()).withName(name2).build();

    workflowService.updateWorkflow(workflow2, null);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow3).isNotNull().hasFieldOrPropertyWithValue("envId", ENV_ID_CHANGED);
    OrchestrationWorkflow orchestrationWorkflow = workflow3.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow.isValid()).isFalse();
    assertThat(orchestrationWorkflow)
        .hasFieldOrPropertyWithValue(
            "validationMessage", format(WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE, "[Phase 1]"));

    List<WorkflowPhase> workflowPhases =
        ((BasicOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    assertThat(workflowPhases).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase.getInfraMappingId()).isNull();
    assertThat(workflowPhase.getComputeProviderId()).isNull();
    assertThat(workflowPhase.getInfraMappingName()).isNull();
  }

  @Test
  public void shouldUpdateBasicDeploymentEnvironmentServiceInfraMapping() {
    Workflow workflow1 = createBasicWorkflow();
    String name2 = "Name2";

    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(DOCKER).build());
    when(serviceResourceService.get(APP_ID, SERVICE_ID_CHANGED, false))
        .thenReturn(Service.builder().artifactType(DOCKER).uuid(SERVICE_ID_CHANGED).build());

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID_CHANGED))
        .thenReturn(anAwsInfrastructureMapping()
                        .withName("NAME")
                        .withServiceId(SERVICE_ID_CHANGED)
                        .withUuid(INFRA_MAPPING_ID_CHANGED)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow2 = aWorkflow()
                             .withAppId(APP_ID)
                             .withEnvId(ENV_ID_CHANGED)
                             .withInfraMappingId(INFRA_MAPPING_ID_CHANGED)
                             .withServiceId(SERVICE_ID_CHANGED)
                             .withUuid(workflow1.getUuid())
                             .withName(name2)
                             .build();

    workflowService.updateWorkflow(workflow2, null);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow3).isNotNull().hasFieldOrPropertyWithValue("envId", "ENV_ID_CHANGED");
    OrchestrationWorkflow orchestrationWorkflow = workflow3.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow.isValid()).isTrue();

    List<WorkflowPhase> workflowPhases =
        ((BasicOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    assertThat(workflowPhases).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase).hasFieldOrPropertyWithValue("infraMappingId", INFRA_MAPPING_ID_CHANGED);
    assertThat(workflowPhase).hasFieldOrPropertyWithValue("serviceId", SERVICE_ID_CHANGED);
    assertThat(workflowPhase.getComputeProviderId()).isNotNull();
    assertThat(workflowPhase.getInfraMappingName()).isNotNull();
  }

  @Test(expected = WingsException.class)
  public void shouldUpdateBasicEnvironmentServiceInfraMappingIncompatible() {
    Workflow workflow1 = createBasicWorkflow();
    String name2 = "Name2";

    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).artifactType(DOCKER).build());
    when(serviceResourceService.get(APP_ID, SERVICE_ID_CHANGED, false))
        .thenReturn(Service.builder().artifactType(DOCKER).uuid(SERVICE_ID_CHANGED).build());

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID_CHANGED))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
                        .withUuid(INFRA_MAPPING_ID_CHANGED)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow2 = aWorkflow()
                             .withAppId(APP_ID)
                             .withEnvId(ENV_ID_CHANGED)
                             .withInfraMappingId(INFRA_MAPPING_ID_CHANGED)
                             .withServiceId(SERVICE_ID_CHANGED)
                             .withUuid(workflow1.getUuid())
                             .withName(name2)
                             .build();

    workflowService.updateWorkflow(workflow2, null);
  }

  @Test
  public void shouldUpdateBasicDeploymentInCompatibleService() {
    Workflow workflow1 = createBasicWorkflow();
    String name2 = "Name2";

    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).artifactType(DOCKER).build());
    when(serviceResourceService.get(APP_ID, SERVICE_ID_CHANGED, false))
        .thenReturn(Service.builder().name(SERVICE_NAME).artifactType(WAR).uuid(SERVICE_ID_CHANGED).build());

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID_CHANGED))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID_CHANGED)
                        .withUuid(INFRA_MAPPING_ID_CHANGED)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow2 = aWorkflow()
                             .withAppId(APP_ID)
                             .withEnvId(ENV_ID_CHANGED)
                             .withInfraMappingId(INFRA_MAPPING_ID_CHANGED)
                             .withServiceId(SERVICE_ID_CHANGED)
                             .withUuid(workflow1.getUuid())
                             .withName(name2)
                             .build();

    try {
      workflowService.updateWorkflow(workflow2, null);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isNotNull();
      assertThat(e.getParams().get("message"))
          .isEqualTo("Service [SERVICE_NAME] is not compatible with the service [SERVICE_NAME]");
    }
  }

  @Test
  public void shouldUpdateMulitServiceDeploymentEnvironment() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    Workflow workflow1 =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withEnvId(ENV_ID)
            .withAppId(APP_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aMultiServiceOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    workflowService.createWorkflow(workflow1);

    String name2 = "Name2";

    Workflow workflow2 =
        aWorkflow().withAppId(APP_ID).withEnvId(ENV_ID_CHANGED).withUuid(workflow1.getUuid()).withName(name2).build();

    workflowService.updateWorkflow(workflow2, null);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow3).isNotNull().hasFieldOrPropertyWithValue("envId", ENV_ID_CHANGED);
    OrchestrationWorkflow orchestrationWorkflow = workflow3.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow.isValid()).isFalse();
    assertThat(orchestrationWorkflow)
        .hasFieldOrPropertyWithValue(
            "validationMessage", format(WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE, "[Phase 1]"));

    List<WorkflowPhase> workflowPhases =
        ((CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    assertThat(workflowPhases).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase.getInfraMappingId()).isNull();
    assertThat(workflowPhase.getComputeProviderId()).isNull();
    assertThat(workflowPhase.getInfraMappingName()).isNull();
  }

  @Test
  public void shouldUpdateMultiServiceDeploymentEnvironmentServiceInfraMapping() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    when(serviceResourceService.get(APP_ID, SERVICE_ID_CHANGED, false))
        .thenReturn(Service.builder().name(SERVICE_NAME).artifactType(WAR).uuid(SERVICE_ID_CHANGED).build());

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID_CHANGED))
        .thenReturn(anAwsInfrastructureMapping()
                        .withName("NAME")
                        .withServiceId(SERVICE_ID_CHANGED)
                        .withUuid(INFRA_MAPPING_ID_CHANGED)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createMultiServiceWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("phase2-changed");
    workflowPhase2.setServiceId(SERVICE_ID_CHANGED);
    workflowPhase2.setInfraMappingId(INFRA_MAPPING_ID_CHANGED);

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase2);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    List<WorkflowPhase> workflowPhases3 =
        ((CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhase3 = workflowPhases3.get(workflowPhases3.size() - 1);
    assertThat(workflowPhase3).isEqualToComparingOnlyGivenFields(workflowPhase2, "uuid", "name");

    assertThat(workflowPhase3).hasFieldOrPropertyWithValue("infraMappingId", INFRA_MAPPING_ID_CHANGED);
    assertThat(workflowPhase3).hasFieldOrPropertyWithValue("serviceId", SERVICE_ID_CHANGED);
    assertThat(workflowPhase3.getComputeProviderId()).isNotNull();
    assertThat(workflowPhase3.getInfraMappingName()).isNotNull();
  }

  @Test
  public void shouldUpdateMultiServiceDeploymentInCompatibleService() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    when(serviceResourceService.get(APP_ID, SERVICE_ID_CHANGED, false))
        .thenReturn(Service.builder().name(SERVICE_NAME).artifactType(WAR).uuid(SERVICE_ID_CHANGED).build());

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID_CHANGED))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID_CHANGED)
                        .withUuid(INFRA_MAPPING_ID_CHANGED)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createMultiServiceWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("phase2-changed");
    workflowPhase2.setServiceId(SERVICE_ID_CHANGED);
    workflowPhase2.setInfraMappingId(INFRA_MAPPING_ID_CHANGED);

    try {
      workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase2);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isNotNull();
      assertThat(e.getParams().get("message"))
          .isEqualTo("Workflow is not compatible with service [" + SERVICE_NAME + "]");
    }
  }

  @Test
  public void shouldUpdateCanaryDeploymentEnvironmentNoPhases() {
    Workflow workflow1 = createCanaryWorkflow();
    String name2 = "Name2";

    Workflow workflow2 =
        aWorkflow().withAppId(APP_ID).withEnvId(ENV_ID_CHANGED).withUuid(workflow1.getUuid()).withName(name2).build();

    workflowService.updateWorkflow(workflow2, null);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow3).isNotNull().hasFieldOrPropertyWithValue("envId", ENV_ID_CHANGED);
    OrchestrationWorkflow orchestrationWorkflow = workflow3.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow.isValid()).isTrue();
  }

  @Test
  public void shouldUpdateCanaryDeploymentEnvironment() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    Workflow workflow1 =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withEnvId(ENV_ID)
            .withAppId(APP_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    workflowService.createWorkflow(workflow1);

    String name2 = "Name2";

    Workflow workflow2 =
        aWorkflow().withAppId(APP_ID).withEnvId(ENV_ID_CHANGED).withUuid(workflow1.getUuid()).withName(name2).build();

    workflowService.updateWorkflow(workflow2, null);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow3).isNotNull().hasFieldOrPropertyWithValue("envId", ENV_ID_CHANGED);
    OrchestrationWorkflow orchestrationWorkflow = workflow3.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow.isValid()).isFalse();
    assertThat(orchestrationWorkflow)
        .hasFieldOrPropertyWithValue(
            "validationMessage", format(WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE, "[Phase 1]"));

    List<WorkflowPhase> workflowPhases =
        ((CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    assertThat(workflowPhases).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase.getInfraMappingId()).isNull();
    assertThat(workflowPhase.getComputeProviderId()).isNull();
    assertThat(workflowPhase.getInfraMappingName()).isNull();
  }

  @Test
  public void shouldUpdateCanaryDeploymentEnvironmentServiceInfraMapping() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withName("NAME")
                        .withServiceId(SERVICE_ID)
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    when(serviceResourceService.get(APP_ID, SERVICE_ID_CHANGED, false))
        .thenReturn(Service.builder().name(SERVICE_NAME).artifactType(WAR).uuid(SERVICE_ID_CHANGED).build());

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID_CHANGED))
        .thenReturn(anAwsInfrastructureMapping()
                        .withName("NAME")
                        .withServiceId(SERVICE_ID_CHANGED)
                        .withUuid(INFRA_MAPPING_ID_CHANGED)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("phase2-changed");
    workflowPhase2.setServiceId(SERVICE_ID_CHANGED);
    workflowPhase2.setInfraMappingId(INFRA_MAPPING_ID_CHANGED);

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase2);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    List<WorkflowPhase> workflowPhases3 =
        ((CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhase3 = workflowPhases3.get(workflowPhases3.size() - 1);
    assertThat(workflowPhase3).isEqualToComparingOnlyGivenFields(workflowPhase2, "uuid", "name");

    assertThat(workflowPhase3).hasFieldOrPropertyWithValue("infraMappingId", INFRA_MAPPING_ID_CHANGED);
    assertThat(workflowPhase3).hasFieldOrPropertyWithValue("serviceId", SERVICE_ID_CHANGED);
    assertThat(workflowPhase3.getComputeProviderId()).isNotNull();
    assertThat(workflowPhase3.getInfraMappingName()).isNotNull();
  }

  @Test
  public void shouldUpdateCanaryInCompatibleService() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    when(serviceResourceService.get(APP_ID, SERVICE_ID_CHANGED, false))
        .thenReturn(Service.builder().name(SERVICE_NAME).artifactType(WAR).uuid(SERVICE_ID_CHANGED).build());

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID_CHANGED))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID_CHANGED)
                        .withUuid(INFRA_MAPPING_ID_CHANGED)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("phase2-changed");
    workflowPhase2.setServiceId(SERVICE_ID_CHANGED);
    workflowPhase2.setInfraMappingId(INFRA_MAPPING_ID_CHANGED);

    try {
      workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase2);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isNotNull();
      assertThat(e.getParams().get("message"))
          .isEqualTo("Workflow is not compatible with service [" + SERVICE_NAME + "]");
    }
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
            .withEnvId(ENV_ID)
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
    workflowService.updatePostDeployment(workflow1.getAppId(), workflow1.getUuid(), phaseStep);

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
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
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
    assertThat(workflowPhase2).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase2.getPhaseSteps()).isNotNull().hasSize(6);
  }

  @Test
  public void shouldUpdateWorkflowPhase() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow1Refresh = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow1Refresh).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow1Refresh.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("abcd");

    workflowService.updateWorkflowPhase(workflow1Refresh.getAppId(), workflow1Refresh.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    List<WorkflowPhase> workflowPhases =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhases2Changed = workflowPhases.get(workflowPhases.size() - 1);
    assertThat(workflowPhases2Changed).isEqualToComparingOnlyGivenFields(workflowPhase2, "uuid", "name");

    WorkflowPhase workflowPhase3 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase3);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    workflowPhases = ((CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhases3Refreshed = workflowPhases.get(workflowPhases.size() - 1);
    assertThat(workflowPhases3Refreshed).isNotNull().hasFieldOrPropertyWithValue("name", "Phase 3");
  }

  @Test(expected = WingsException.class)
  public void shouldCreateWorkflowPhaseInvalidServiceandInframapping() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID_CHANGED)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);
  }

  @Test(expected = WingsException.class)
  public void shouldUpdateWorkflowPhaseInvalidServiceandInfra() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID_CHANGED, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID_CHANGED).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID_CHANGED))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID_CHANGED)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("phase2-changed");
    workflowPhase2.setServiceId(SERVICE_ID_CHANGED);
    workflowPhase2.setInfraMappingId(INFRA_MAPPING_ID_CHANGED);

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase2);
  }

  @Test
  public void shouldCloneWorkflowPhase() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("phase 2-clone");

    workflowService.cloneWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase2);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    List<WorkflowPhase> workflowPhases3 =
        ((CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase clonedWorkflowPhase = workflowPhases3.get(workflowPhases3.size() - 1);
    assertThat(clonedWorkflowPhase).isNotNull();
    assertThat(clonedWorkflowPhase.getUuid()).isNotEqualTo(workflowPhase2.getUuid());
    assertThat(clonedWorkflowPhase.getName()).isEqualTo("phase 2-clone");
    assertThat(clonedWorkflowPhase)
        .isEqualToComparingOnlyGivenFields(workflowPhase2, "infraMappingId", "serviceId", "computeProviderId");
    assertThat(clonedWorkflowPhase.getPhaseSteps()).isNotNull().size().isEqualTo(workflowPhase2.getPhaseSteps().size());
  }

  @Test
  public void shouldCreateMultiServiceWorkflowPhase() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
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
    assertThat(workflowPhase2).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase2.getPhaseSteps()).isNotNull().hasSize(6);
  }

  @Test
  public void shouldUpdateMultiServiceWorkflowPhase() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createMultiServiceWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
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
                    .addWorkflowPhase(
                        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build())
                    .addWorkflowPhase(
                        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build())
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
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
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
        (CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow();

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
                                                .addStep(aGraphNode()
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
    GraphNode node = graph.getNodes().get(0);
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
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
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

    for (WorkflowPhase phase : orchestrationWorkflow3.getWorkflowPhases()) {
      for (PhaseStep phaseStep : phase.getPhaseSteps()) {
        if (SELECT_NODE == phaseStep.getPhaseStepType() || INFRASTRUCTURE_NODE == phaseStep.getPhaseStepType()) {
          for (GraphNode node : phaseStep.getSteps()) {
            if (StateType.AWS_NODE_SELECT.name().equals(node.getType())
                || StateType.DC_NODE_SELECT.name().equals(node.getType())) {
              Map<String, Object> properties = node.getProperties();
              assertThat(properties.get("specificHosts")).isEqualTo(false);
              assertThat(properties.get("instanceCount")).isEqualTo(1);
              assertThat(properties.get("excludeSelectedHostsFromFuturePhases")).isEqualTo(true);
            }
          }
        }
      }
    }
  }

  @Test
  public void shouldUpdateNotificationRules() {
    Workflow workflow1 = createCanaryWorkflow();

    List<NotificationRule> notificationRules = newArrayList(aNotificationRule().build());
    List<NotificationRule> updatedNotificationRules =
        workflowService.updateNotificationRules(workflow1.getAppId(), workflow1.getUuid(), notificationRules);

    assertThat(updatedNotificationRules).isNotEmpty();
    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow).isNotNull().hasFieldOrPropertyWithValue("notificationRules", notificationRules);
  }

  @Test
  public void shouldUpdateFailureStrategies() {
    Workflow workflow1 = createCanaryWorkflow();

    List<FailureStrategy> failureStrategies =
        newArrayList(FailureStrategy.builder().failureTypes(asList(FailureType.VERIFICATION_FAILURE)).build());
    List<FailureStrategy> updated =
        workflowService.updateFailureStrategies(workflow1.getAppId(), workflow1.getUuid(), failureStrategies);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow).isNotNull().hasFieldOrPropertyWithValue("failureStrategies", failureStrategies);
  }

  @Test
  public void testValidationFailuresForUpdateFailureStrategies() {
    try {
      Workflow workflow1 = createCanaryWorkflow();

      List<FailureStrategy> failureStrategies = newArrayList(FailureStrategy.builder().build());
      List<FailureStrategy> updated =
          workflowService.updateFailureStrategies(workflow1.getAppId(), workflow1.getUuid(), failureStrategies);
      fail("No Constraint violation detected");
    } catch (ConstraintViolationException e) {
      logger.info("Expected constraintViolationException", e);
    }

    try {
      Workflow workflow1 = createCanaryWorkflow();

      List<FailureStrategy> failureStrategies =
          newArrayList(FailureStrategy.builder()
                           .failureTypes(asList(FailureType.VERIFICATION_FAILURE))
                           .failureCriteria(FailureCriteria.builder().failureThresholdPercentage(-1).build())
                           .build());
      List<FailureStrategy> updated =
          workflowService.updateFailureStrategies(workflow1.getAppId(), workflow1.getUuid(), failureStrategies);
      fail("No Constraint violation detected");
    } catch (ConstraintViolationException e) {
      logger.info("Expected constraintViolationException", e);
    }

    try {
      Workflow workflow1 = createCanaryWorkflow();

      List<FailureStrategy> failureStrategies =
          newArrayList(FailureStrategy.builder()
                           .failureTypes(asList(FailureType.VERIFICATION_FAILURE))
                           .failureCriteria(FailureCriteria.builder().failureThresholdPercentage(101).build())
                           .build());
      List<FailureStrategy> updated =
          workflowService.updateFailureStrategies(workflow1.getAppId(), workflow1.getUuid(), failureStrategies);
      fail("No Constraint violation detected");
    } catch (ConstraintViolationException e) {
      logger.info("Expected constraintViolationException", e);
    }

    try {
      Workflow workflow1 = createCanaryWorkflow();

      List<FailureStrategy> failureStrategies =
          newArrayList(FailureStrategy.builder()
                           .failureTypes(asList(FailureType.VERIFICATION_FAILURE))
                           .failureCriteria(FailureCriteria.builder().failureThresholdPercentage(100).build())
                           .build());
      List<FailureStrategy> updated =
          workflowService.updateFailureStrategies(workflow1.getAppId(), workflow1.getUuid(), failureStrategies);

      failureStrategies =
          newArrayList(FailureStrategy.builder()
                           .failureTypes(asList(FailureType.VERIFICATION_FAILURE))
                           .failureCriteria(FailureCriteria.builder().failureThresholdPercentage(100).build())
                           .build());
      updated = workflowService.updateFailureStrategies(workflow1.getAppId(), workflow1.getUuid(), failureStrategies);
    } catch (Exception e) {
      fail("Unexpected exception", e);
    }
  }

  @Test
  public void shouldUpdateUserVariables() {
    Workflow workflow1 = createCanaryWorkflow();
    List<Variable> userVariables = newArrayList(aVariable().withName("name1").withValue("value1").build());

    workflowService.updateUserVariables(workflow1.getAppId(), workflow1.getUuid(), userVariables);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();
    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow).isNotNull().hasFieldOrPropertyWithValue("userVariables", userVariables);
  }

  @Test
  public void shouldCreateComplexWorkflow() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
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
                                    .collect(toList())
                                    .get(0);

    deployPhaseStep.getSteps().add(
        aGraphNode().withType("HTTP").withName("http").addProperty("url", "www.google.com").build());

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase);

    Workflow workflow4 = workflowService.readWorkflow(workflow2.getAppId(), workflow2.getUuid());
    workflowService.deleteWorkflowPhase(workflow4.getAppId(), workflow4.getUuid(), workflowPhase.getUuid());

    Workflow workflow5 = workflowService.readWorkflow(workflow2.getAppId(), workflow2.getUuid());
    assertThat(workflow5).isNotNull();
  }

  @Test
  public void shouldTemplatizeBasicDeploymentOnCreation() {
    TemplateExpression envExpression = aTemplateExpression()
                                           .withFieldName("envId")
                                           .withExpression("${Environment}")
                                           .withMetadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
                                           .build();
    TemplateExpression infraExpression = aTemplateExpression()
                                             .withFieldName("infraMappingId")
                                             .withExpression("${ServiceInfra_SSH}")
                                             .withMetadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                                             .build();
    TemplateExpression serviceExpression = aTemplateExpression()
                                               .withFieldName("serviceId")
                                               .withExpression("${Service}")
                                               .withMetadata(ImmutableMap.of("entityType", "SERVICE"))
                                               .build();

    Workflow workflow =
        aWorkflow()
            .withEnvId(ENV_ID)
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withServiceId(SERVICE_ID)
            .withInfraMappingId(INFRA_MAPPING_ID)
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withWorkflowPhases(
                        asList(aWorkflowPhase()
                                   .withServiceId(SERVICE_ID)
                                   .withInfraMappingId(INFRA_MAPPING_ID)
                                   .withTemplateExpressions(asList(envExpression, serviceExpression, infraExpression))
                                   .build()))
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .withTemplateExpressions(asList(envExpression, serviceExpression, infraExpression))
            .build();

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    Role role = aRole()
                    .withRoleType(RoleType.ACCOUNT_ADMIN)
                    .withUuid(ROLE_ID)
                    .withAccountId(application.getAccountId())
                    .build();
    List<NotificationGroup> notificationGroups = asList(aNotificationGroup()
                                                            .withUuid(NOTIFICATION_GROUP_ID)
                                                            .withAccountId(application.getAccountId())
                                                            .withRole(role)
                                                            .build());
    when(notificationSetupService.listNotificationGroups(
             application.getAccountId(), RoleType.ACCOUNT_ADMIN.getDisplayName()))
        .thenReturn(notificationGroups);

    Workflow workflow2 = workflowService.createWorkflow(workflow);

    assertThat(workflow2.getKeywords())
        .isNotNull()
        .contains(workflow2.getName().toLowerCase())
        .contains(OrchestrationWorkflowType.BASIC.name().toLowerCase())
        .contains(ENV_NAME.toLowerCase())
        .contains(SERVICE_NAME.toLowerCase())
        .contains("template");

    assertThat(workflow2.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("envId");
    OrchestrationWorkflow orchestrationWorkflow = workflow2.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhases = ((BasicOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
    assertThat(orchestrationWorkflow.getTemplatizedServiceIds()).isNotNull().contains(SERVICE_ID);
    assertThat(orchestrationWorkflow.getTemplatizedInfraMappingIds()).isNotNull().contains(INFRA_MAPPING_ID);
    assertThat(orchestrationWorkflow).extracting("userVariables").isNotEmpty();
    assertThat(
        orchestrationWorkflow.getUserVariables().stream().anyMatch(variable -> variable.getName().equals("Service")))
        .isTrue();

    assertThat(workflowPhases).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(orchestrationWorkflow.getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(ENVIRONMENT, SERVICE, INFRASTRUCTURE_MAPPING);
  }

  @Test
  public void shouldTemplatizeBasicDeployment() {
    Workflow workflow1 = createBasicWorkflow();
    String name2 = "Name2";

    Workflow workflow2 =
        aWorkflow().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(workflow1.getUuid()).withName(name2).build();

    TemplateExpression envExpression = aTemplateExpression()
                                           .withFieldName("envId")
                                           .withExpression("${Environment}")
                                           .withMetadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
                                           .build();
    TemplateExpression infraExpression = aTemplateExpression()
                                             .withFieldName("infraMappingId")
                                             .withExpression("${ServiceInfra_SSH}")
                                             .withMetadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                                             .build();
    TemplateExpression serviceExpression = aTemplateExpression()
                                               .withFieldName("serviceId")
                                               .withExpression("${Service}")
                                               .withMetadata(ImmutableMap.of("entityType", "SERVICE"))
                                               .build();
    workflow2.setTemplateExpressions(asList(envExpression, infraExpression, serviceExpression));

    workflowService.updateWorkflow(workflow2, null);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());

    assertThat(workflow3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("envId");
    OrchestrationWorkflow orchestrationWorkflow = workflow3.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhases = ((BasicOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
    assertThat(orchestrationWorkflow.getTemplatizedServiceIds()).isNotNull().contains(SERVICE_ID);
    assertThat(orchestrationWorkflow.getTemplatizedInfraMappingIds()).isNotNull().contains(INFRA_MAPPING_ID);
    assertThat(orchestrationWorkflow).extracting("userVariables").isNotEmpty();
    assertThat(
        orchestrationWorkflow.getUserVariables().stream().anyMatch(variable -> variable.getName().equals("Service")))
        .isTrue();

    assertThat(workflowPhases).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(orchestrationWorkflow.getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(ENVIRONMENT, SERVICE, INFRASTRUCTURE_MAPPING);
  }

  @Test
  public void shouldTemplatizeMultiServiceEnvThenTemplatizeInfra() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
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
                aMultiServiceOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow1);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid");

    TemplateExpression envExpression = aTemplateExpression()
                                           .withFieldName("envId")
                                           .withExpression("${Environment}")
                                           .withMetadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
                                           .build();
    workflow2.setTemplateExpressions(asList(envExpression));

    workflowService.updateWorkflow(workflow2, null);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());

    assertThat(workflow3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("envId");
    OrchestrationWorkflow orchestrationWorkflow = workflow3.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhases =
        ((MultiServiceOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
    assertThat(orchestrationWorkflow.getTemplatizedServiceIds()).isNotNull().doesNotContain(SERVICE_ID);
    assertThat(orchestrationWorkflow.getTemplatizedInfraMappingIds()).isNotNull().contains(INFRA_MAPPING_ID);
    assertThat(orchestrationWorkflow).extracting("userVariables").isNotEmpty();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("ServiceInfra_SSH")))
        .isTrue();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("ServiceInfra_SSH2")))
        .isTrue();

    assertThat(workflowPhases).isNotNull().hasSize(2);

    WorkflowPhase workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(orchestrationWorkflow.getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(ENVIRONMENT, INFRASTRUCTURE_MAPPING);

    workflowPhase = workflowPhases.get(1);
    assertThat(workflowPhase.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
  }

  @Test
  public void shouldTemplatizeCanaryEnvThenTemplatizeInfra() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
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
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow1);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid");

    TemplateExpression envExpression = aTemplateExpression()
                                           .withFieldName("envId")
                                           .withExpression("${Environment}")
                                           .withMetadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
                                           .build();
    workflow2.setTemplateExpressions(asList(envExpression));

    workflowService.updateWorkflow(workflow2, null);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());

    assertThat(workflow3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("envId");
    OrchestrationWorkflow orchestrationWorkflow = workflow3.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhases = ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
    assertThat(orchestrationWorkflow.getTemplatizedServiceIds()).isNotNull().doesNotContain(SERVICE_ID);
    assertThat(orchestrationWorkflow.getTemplatizedInfraMappingIds()).isNotNull().contains(INFRA_MAPPING_ID);
    assertThat(orchestrationWorkflow).extracting("userVariables").isNotEmpty();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("ServiceInfra_SSH")))
        .isTrue();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("ServiceInfra_SSH2")))
        .isTrue();

    assertThat(workflowPhases).isNotNull().hasSize(2);

    WorkflowPhase workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(orchestrationWorkflow.getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(ENVIRONMENT, INFRASTRUCTURE_MAPPING);
    workflowPhase = workflowPhases.get(1);
    assertThat(workflowPhase.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
  }

  @Test
  public void shouldTemplatizeCanaryPhase() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("phase2-changed");

    TemplateExpression infraExpression = aTemplateExpression()
                                             .withFieldName("infraMappingId")
                                             .withExpression("${ServiceInfra_SSH}")
                                             .withMetadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                                             .build();
    TemplateExpression serviceExpression = aTemplateExpression()
                                               .withFieldName("serviceId")
                                               .withExpression("${Service}")
                                               .withMetadata(ImmutableMap.of("entityType", "SERVICE"))
                                               .build();
    workflowPhase2.setTemplateExpressions(asList(serviceExpression, infraExpression));

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase2);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    List<WorkflowPhase> workflowPhases3 =
        ((CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhase3 = workflowPhases3.get(workflowPhases3.size() - 1);
    assertThat(workflowPhase3).isEqualToComparingOnlyGivenFields(workflowPhase2, "uuid", "name");

    assertThat(workflowPhase3.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(workflow3.getOrchestrationWorkflow().getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(SERVICE, INFRASTRUCTURE_MAPPING);
  }

  @Test
  public void shouldUpdateTemplatizeExpressionsBasicDeployment() {
    Workflow workflow1 = createBasicWorkflow();
    String name2 = "Name2";

    Workflow workflow2 =
        aWorkflow().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(workflow1.getUuid()).withName(name2).build();

    TemplateExpression envExpression = aTemplateExpression()
                                           .withFieldName("envId")
                                           .withExpression("${Environment}")
                                           .withMetadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
                                           .build();
    TemplateExpression infraExpression = aTemplateExpression()
                                             .withFieldName("infraMappingId")
                                             .withExpression("${ServiceInfra_SSH}")
                                             .withMetadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                                             .build();
    TemplateExpression serviceExpression = aTemplateExpression()
                                               .withFieldName("serviceId")
                                               .withExpression("${Service}")
                                               .withMetadata(ImmutableMap.of("entityType", "SERVICE"))
                                               .build();
    workflow2.setTemplateExpressions(asList(envExpression, infraExpression, serviceExpression));

    workflowService.updateWorkflow(workflow2, null);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());

    assertThat(workflow3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("envId");
    OrchestrationWorkflow orchestrationWorkflow = workflow3.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhases = ((BasicOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
    assertThat(orchestrationWorkflow.getTemplatizedServiceIds()).isNotNull().contains(SERVICE_ID);
    assertThat(orchestrationWorkflow.getTemplatizedInfraMappingIds()).isNotNull().contains(INFRA_MAPPING_ID);
    assertThat(orchestrationWorkflow).extracting("userVariables").isNotEmpty();
    assertThat(
        orchestrationWorkflow.getUserVariables().stream().anyMatch(variable -> variable.getName().equals("Service")))
        .isTrue();

    assertThat(workflowPhases).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(orchestrationWorkflow.getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(ENVIRONMENT, SERVICE, INFRASTRUCTURE_MAPPING);

    // Now update template expressions with different names

    envExpression = aTemplateExpression()
                        .withFieldName("envId")
                        .withExpression("${Environment_Changed}")
                        .withMetadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
                        .build();
    infraExpression = aTemplateExpression()
                          .withFieldName("infraMappingId")
                          .withExpression("${ServiceInfra_SSH_Changed}")
                          .withMetadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                          .build();
    serviceExpression = aTemplateExpression()
                            .withFieldName("serviceId")
                            .withExpression("${Service_Changed}")
                            .withMetadata(ImmutableMap.of("entityType", "SERVICE"))
                            .build();
    workflow2.setTemplateExpressions(asList(envExpression, infraExpression, serviceExpression));

    workflowService.updateWorkflow(workflow2, null);

    Workflow templatizedWorkflow = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());

    assertThat(workflow3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("envId");
    orchestrationWorkflow = templatizedWorkflow.getOrchestrationWorkflow();
    workflowPhases = ((BasicOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
    assertThat(orchestrationWorkflow.getTemplatizedServiceIds()).isNotNull().contains(SERVICE_ID);
    assertThat(orchestrationWorkflow.getTemplatizedInfraMappingIds()).isNotNull().contains(INFRA_MAPPING_ID);
    assertThat(orchestrationWorkflow).extracting("userVariables").isNotEmpty();
    assertThat(
        orchestrationWorkflow.getUserVariables().stream().anyMatch(variable -> variable.getName().equals("Service")))
        .isFalse();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("Service_Changed")))
        .isTrue();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("ServiceInfra_SSH")))
        .isFalse();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("ServiceInfra_SSH_Changed")))
        .isTrue();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("Environment")))
        .isFalse();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("Environment_Changed")))
        .isTrue();

    assertThat(workflowPhases).isNotNull().hasSize(1);

    workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("serviceId");
    assertThat(orchestrationWorkflow.getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(ENVIRONMENT, SERVICE, INFRASTRUCTURE_MAPPING);
  }

  @Test
  public void shouldUpdateTemplatizeExpressionsCanary() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
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
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow workflow2 = workflowService.createWorkflow(workflow1);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid");

    TemplateExpression envExpression = aTemplateExpression()
                                           .withFieldName("envId")
                                           .withExpression("${Environment}")
                                           .withMetadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
                                           .build();
    workflow2.setTemplateExpressions(asList(envExpression));

    workflowService.updateWorkflow(workflow2, null);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());

    assertThat(workflow3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("envId");
    OrchestrationWorkflow orchestrationWorkflow = workflow3.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhases = ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
    assertThat(orchestrationWorkflow.getTemplatizedServiceIds()).isNotNull().doesNotContain(SERVICE_ID);
    assertThat(orchestrationWorkflow.getTemplatizedInfraMappingIds()).isNotNull().contains(INFRA_MAPPING_ID);
    assertThat(orchestrationWorkflow).extracting("userVariables").isNotEmpty();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("ServiceInfra_SSH")))
        .isTrue();

    assertThat(workflowPhases).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(orchestrationWorkflow.getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(ENVIRONMENT, INFRASTRUCTURE_MAPPING);

    envExpression = aTemplateExpression()
                        .withFieldName("envId")
                        .withExpression("${Environment_Changed}")
                        .withMetadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
                        .build();
    workflow3.setTemplateExpressions(asList(envExpression));

    workflow3 = workflowService.updateWorkflow(workflow3, null);

    assertThat(workflow3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("envId");
    orchestrationWorkflow = workflow3.getOrchestrationWorkflow();
    workflowPhases = ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
    assertThat(orchestrationWorkflow.getTemplatizedServiceIds()).isNotNull().doesNotContain(SERVICE_ID);
    assertThat(orchestrationWorkflow.getTemplatizedInfraMappingIds()).isNotNull().contains(INFRA_MAPPING_ID);
    assertThat(orchestrationWorkflow).extracting("userVariables").isNotEmpty();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("ServiceInfra_SSH")))
        .isTrue();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("Environment")))
        .isFalse();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("Environment_Changed")))
        .isTrue();

    assertThat(workflowPhases).isNotNull().hasSize(1);

    workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(orchestrationWorkflow.getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(ENVIRONMENT, INFRASTRUCTURE_MAPPING);
  }

  @Test
  public void shouldTemplatizeBasicPhase() {
    Workflow workflow = createBasicWorkflow();
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow).isNotNull();
    List<WorkflowPhase> workflowPhases = ((BasicOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
    WorkflowPhase workflowPhase = workflowPhases.get(workflowPhases.size() - 1);

    workflowPhase.setName("phase2-changed");

    workflowPhase.setTemplateExpressions(
        asList(aTemplateExpression()
                   .withFieldName("infraMappingId")
                   .withExpression("${ServiceInfra_SSH}")
                   .withMetadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                   .build(),
            aTemplateExpression()
                .withFieldName("serviceId")
                .withExpression("${Service}")
                .withMetadata(ImmutableMap.of("entityType", "SERVICE"))
                .build()));

    workflowService.updateWorkflowPhase(workflow.getAppId(), workflow.getUuid(), workflowPhase);

    Workflow workflow3 = workflowService.readWorkflow(workflow.getAppId(), workflow.getUuid());

    List<WorkflowPhase> workflowPhases2 =
        ((BasicOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhase3 = workflowPhases2.get(workflowPhases2.size() - 1);
    assertThat(workflowPhase3).isEqualToComparingOnlyGivenFields(workflowPhase, "uuid", "name");

    assertThat(workflowPhase3.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(workflow3.getOrchestrationWorkflow().getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(SERVICE, INFRASTRUCTURE_MAPPING);
  }

  @Test
  public void shouldDeTemplatizeBasicDeployment() {
    Workflow workflow1 = createBasicWorkflow();
    String name2 = "Name2";

    Workflow workflow2 =
        aWorkflow().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(workflow1.getUuid()).withName(name2).build();

    TemplateExpression envExpression = aTemplateExpression()
                                           .withFieldName("envId")
                                           .withExpression("${Environment}")
                                           .withMetadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
                                           .build();
    TemplateExpression infraExpression = aTemplateExpression()
                                             .withFieldName("infraMappingId")
                                             .withExpression("${ServiceInfra_SSH}")
                                             .withMetadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                                             .build();
    TemplateExpression serviceExpression = aTemplateExpression()
                                               .withFieldName("serviceId")
                                               .withExpression("${Service}")
                                               .withMetadata(ImmutableMap.of("entityType", "SERVICE"))
                                               .build();
    workflow2.setTemplateExpressions(asList(envExpression, infraExpression, serviceExpression));

    workflowService.updateWorkflow(workflow2, null);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());

    assertThat(workflow3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("envId");
    OrchestrationWorkflow orchestrationWorkflow = workflow3.getOrchestrationWorkflow();
    List<WorkflowPhase> workflowPhases = ((BasicOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
    assertThat(orchestrationWorkflow.getTemplatizedServiceIds()).isNotNull().contains(SERVICE_ID);
    assertThat(orchestrationWorkflow.getTemplatizedInfraMappingIds()).isNotNull().contains(INFRA_MAPPING_ID);
    assertThat(orchestrationWorkflow).extracting("userVariables").isNotEmpty();
    assertThat(
        orchestrationWorkflow.getUserVariables().stream().anyMatch(variable -> variable.getName().equals("Service")))
        .isTrue();

    assertThat(workflowPhases).isNotNull().hasSize(1);

    WorkflowPhase workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(orchestrationWorkflow.getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(ENVIRONMENT, SERVICE, INFRASTRUCTURE_MAPPING);

    // Detemplatize service Infra
    workflow2.setTemplateExpressions(asList(envExpression, infraExpression));

    workflowService.updateWorkflow(workflow2, null);

    Workflow templatizedWorkflow = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());

    assertThat(workflow3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("envId");
    orchestrationWorkflow = templatizedWorkflow.getOrchestrationWorkflow();
    workflowPhases = ((BasicOrchestrationWorkflow) orchestrationWorkflow).getWorkflowPhases();
    assertThat(orchestrationWorkflow.getTemplatizedServiceIds()).isNullOrEmpty();
    assertThat(orchestrationWorkflow.getTemplatizedInfraMappingIds()).isNotNull().contains(INFRA_MAPPING_ID);
    assertThat(orchestrationWorkflow).extracting("userVariables").isNotEmpty();
    assertThat(
        orchestrationWorkflow.getUserVariables().stream().anyMatch(variable -> variable.getName().equals("Service")))
        .isFalse();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("ServiceInfra_SSH")))
        .isTrue();
    assertThat(orchestrationWorkflow.getUserVariables().stream().anyMatch(
                   variable -> variable.getName().equals("Environment")))
        .isTrue();

    assertThat(workflowPhases).isNotNull().hasSize(1);

    workflowPhase = workflowPhases.get(0);
    assertThat(workflowPhase).isNotNull().hasFieldOrPropertyWithValue("name", PHASE_NAME_PREFIX + 1);
    assertThat(workflowPhase.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(workflowPhase.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .doesNotContain("serviceId");
    assertThat(orchestrationWorkflow.getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(ENVIRONMENT, INFRASTRUCTURE_MAPPING);
  }

  @Test(expected = WingsException.class)
  public void shouldDeTemplatizeOnlyInfraCanaryPhase() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("phase2-changed");

    TemplateExpression infraExpression = aTemplateExpression()
                                             .withFieldName("infraMappingId")
                                             .withExpression("${ServiceInfra_SSH}")
                                             .withMetadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                                             .build();
    TemplateExpression serviceExpression = aTemplateExpression()
                                               .withFieldName("serviceId")
                                               .withExpression("${Service}")
                                               .withMetadata(ImmutableMap.of("entityType", "SERVICE"))
                                               .build();
    workflowPhase2.setTemplateExpressions(asList(serviceExpression, infraExpression));

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase2);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    List<WorkflowPhase> workflowPhases3 =
        ((CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhase3 = workflowPhases3.get(workflowPhases3.size() - 1);
    assertThat(workflowPhase3).isEqualToComparingOnlyGivenFields(workflowPhase2, "uuid", "name");

    assertThat(workflowPhase3.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(workflow3.getOrchestrationWorkflow().getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(SERVICE, INFRASTRUCTURE_MAPPING);

    workflowPhase3.setTemplateExpressions(asList(serviceExpression));
    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase3);
  }

  @Test
  public void shouldDeTemplatizeOnlyServiceandInfraCanaryPhase() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("phase2-changed");

    TemplateExpression infraExpression = aTemplateExpression()
                                             .withFieldName("infraMappingId")
                                             .withExpression("${ServiceInfra_SSH}")
                                             .withMetadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                                             .build();
    TemplateExpression serviceExpression = aTemplateExpression()
                                               .withFieldName("serviceId")
                                               .withExpression("${Service}")
                                               .withMetadata(ImmutableMap.of("entityType", "SERVICE"))
                                               .build();
    workflowPhase2.setTemplateExpressions(asList(serviceExpression, infraExpression));

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase2);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    List<WorkflowPhase> workflowPhases3 =
        ((CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhase3 = workflowPhases3.get(workflowPhases3.size() - 1);
    assertThat(workflowPhase3).isEqualToComparingOnlyGivenFields(workflowPhase2, "uuid", "name");

    assertThat(workflowPhase3.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(workflow3.getOrchestrationWorkflow().getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(SERVICE, INFRASTRUCTURE_MAPPING);

    workflowPhase3.setTemplateExpressions(null);
    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase3);

    Workflow workflow4 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    List<WorkflowPhase> workflowPhases4 =
        ((CanaryOrchestrationWorkflow) workflow4.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhase4 = workflowPhases3.get(workflowPhases4.size() - 1);
    assertThat(workflowPhase4).isEqualToComparingOnlyGivenFields(workflowPhase2, "uuid", "name");

    assertThat(workflowPhase4.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase4.getTemplateExpressions()).isEmpty();
    assertThat(workflow4.getOrchestrationWorkflow().getUserVariables()).isNullOrEmpty();
  }

  @Test
  public void shouldCreateBuildDeploymentWorkflow() {
    Workflow workflow = aWorkflow()
                            .withName(WORKFLOW_NAME)
                            .withAppId(APP_ID)
                            .withServiceId(SERVICE_ID)
                            .withInfraMappingId(INFRA_MAPPING_ID)
                            .withOrchestrationWorkflow(aBuildOrchestrationWorkflow().build())
                            .build();

    Role role = aRole()
                    .withRoleType(RoleType.ACCOUNT_ADMIN)
                    .withUuid(ROLE_ID)
                    .withAccountId(application.getAccountId())
                    .build();
    List<NotificationGroup> notificationGroups = asList(aNotificationGroup()
                                                            .withUuid(NOTIFICATION_GROUP_ID)
                                                            .withAccountId(application.getAccountId())
                                                            .withRole(role)
                                                            .build());
    when(notificationSetupService.listNotificationGroups(
             application.getAccountId(), RoleType.ACCOUNT_ADMIN.getDisplayName()))
        .thenReturn(notificationGroups);

    Workflow workflow2 = workflowService.createWorkflow(workflow);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid").hasFieldOrPropertyWithValue("appId", APP_ID);

    BuildWorkflow orchestrationWorkflow = (BuildWorkflow) workflow2.getOrchestrationWorkflow();
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

    assertThat(orchestrationWorkflow.getFailureStrategies()).isEmpty();

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
  public void shouldAwsCodeDeployStateDefaults() {
    when(artifactStreamService.getArtifactStreamsForService(APP_ID, SERVICE_ID)).thenReturn(asList(artifactStream));
    when(artifactStream.getArtifactStreamType()).thenReturn(ArtifactStreamType.AMAZON_S3.name());
    Map<String, String> defaults = workflowService.getStateDefaults(APP_ID, SERVICE_ID, StateType.AWS_CODEDEPLOY_STATE);
    assertThat(defaults).isNotEmpty();
    assertThat(defaults).containsKeys("bucket", "key", "bundleType");
    assertThat(defaults).containsValues(ARTIFACT_S3_BUCKET_EXPRESSION, ARTIFACT__S3_KEY_EXPRESSION, "zip");
  }

  @Test
  public void shouldAwsCodeDeployNoStateDefaults() {
    assertThat(workflowService.getStateDefaults(APP_ID, SERVICE_ID, StateType.AWS_CODEDEPLOY_STATE)).isEmpty();
  }

  @Test
  public void shouldTestWorkflowHasSshInfraMapping() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false)).thenReturn(Service.builder().uuid(SERVICE_ID).build());
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(Service.builder().uuid(SERVICE_ID).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    assertThat(workflowService.workflowHasSshInfraMapping(workflow2.getAppId(), workflow2.getUuid())).isFalse();
  }

  @Test
  public void shouldTemplatizeAppDElkState() {
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(Service.builder().uuid(SERVICE_ID).build());
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false)).thenReturn(Service.builder().uuid(SERVICE_ID).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
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
    PhaseStep verifyPhaseStep = workflowPhase.getPhaseSteps()
                                    .stream()
                                    .filter(ps -> ps.getPhaseStepType() == PhaseStepType.VERIFY_SERVICE)
                                    .collect(toList())
                                    .get(0);

    verifyPhaseStep.getSteps().add(aGraphNode()
                                       .withType("APP_DYNAMICS")
                                       .withName("APP_DYNAMICS")
                                       .addProperty("analysisServerConfigId", "analysisServerConfigId")
                                       .addProperty("applicationId", "applicagionId")
                                       .addProperty("tierId", "tierId")
                                       .build());

    verifyPhaseStep.getSteps().add(aGraphNode()
                                       .withType("ELK")
                                       .withName("ELK")
                                       .addProperty("analysisServerConfigId", "analysisServerConfigId")
                                       .addProperty("indices", "indices")
                                       .build());

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase);

    Workflow workflow4 = workflowService.readWorkflow(workflow2.getAppId(), workflow2.getUuid());
    CanaryOrchestrationWorkflow orchestrationWorkflow4 =
        (CanaryOrchestrationWorkflow) workflow4.getOrchestrationWorkflow();

    workflowPhase = orchestrationWorkflow4.getWorkflowPhases().get(0);
    verifyPhaseStep = workflowPhase.getPhaseSteps()
                          .stream()
                          .filter(ps -> ps.getPhaseStepType() == PhaseStepType.VERIFY_SERVICE)
                          .collect(toList())
                          .get(0);

    List<TemplateExpression> appDTemplateExpressions =
        asList(aTemplateExpression()
                   .withFieldName("analysisServerConfigId")
                   .withExpression("${AppDynamics_Server}")
                   .withMetadata(ImmutableMap.of("entityType", "APPDYNAMICS_CONFIGID"))
                   .build(),
            aTemplateExpression()
                .withFieldName("applicationId")
                .withExpression("${AppDynamics_App}")
                .withMetadata(ImmutableMap.of("entityType", "APPDYNAMICS_APPID"))
                .build(),
            aTemplateExpression()
                .withFieldName("tierId")
                .withExpression("${AppDynamics_Tier}")
                .withMetadata(ImmutableMap.of("entityType", "APPDYNAMICS_TIERID"))
                .build());

    List<TemplateExpression> elkTemplateExpressions =
        asList(aTemplateExpression()
                   .withFieldName("analysisServerConfigId")
                   .withExpression("${ELK_Server}")
                   .withMetadata(ImmutableMap.of("entityType", "ELK_CONFIGID"))
                   .build(),
            aTemplateExpression()
                .withFieldName("indices")
                .withExpression("${ELK_Indices}")
                .withMetadata(ImmutableMap.of("entityType", "ELK_INDICES"))
                .build());
    verifyPhaseStep.getSteps().get(0).setTemplateExpressions(appDTemplateExpressions);
    verifyPhaseStep.getSteps().get(1).setTemplateExpressions(elkTemplateExpressions);

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase);

    Workflow workflow5 = workflowService.readWorkflow(workflow2.getAppId(), workflow2.getUuid());
    assertThat(workflow5).isNotNull();
    CanaryOrchestrationWorkflow orchestrationWorkflow5 =
        (CanaryOrchestrationWorkflow) workflow5.getOrchestrationWorkflow();
    assertThat(orchestrationWorkflow5).extracting("userVariables").isNotEmpty();
    assertThat(orchestrationWorkflow5.getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(APPDYNAMICS_CONFIGID, EntityType.APPDYNAMICS_APPID, EntityType.APPDYNAMICS_TIERID,
            ELK_CONFIGID, ELK_INDICES);
  }

  /**
   * Test custom metric yaml generation
   * @throws Exception
   */
  @Test
  public void testGetHPAYamlStringWithCustomMetric() throws Exception {
    Integer minAutoscaleInstances = 2;
    Integer maxAutoscaleInstances = 10;
    Integer targetCpuUtilizationPercentage = 60;

    String yamlHPA = workflowService.getHPAYamlStringWithCustomMetric(
        minAutoscaleInstances, maxAutoscaleInstances, targetCpuUtilizationPercentage);

    HorizontalPodAutoscaler horizontalPodAutoscaler = KubernetesHelper.loadYaml(yamlHPA);
    assertEquals("autoscaling/v2beta1", horizontalPodAutoscaler.getApiVersion());
    assertEquals("HorizontalPodAutoscaler", horizontalPodAutoscaler.getKind());
    assertNotNull(horizontalPodAutoscaler.getSpec());
    assertNotNull(horizontalPodAutoscaler.getMetadata());
    assertEquals(Integer.valueOf(2), horizontalPodAutoscaler.getSpec().getMinReplicas());
    assertEquals(Integer.valueOf(10), horizontalPodAutoscaler.getSpec().getMaxReplicas());
    assertNotNull(horizontalPodAutoscaler.getSpec().getAdditionalProperties());
    assertEquals(1, horizontalPodAutoscaler.getSpec().getAdditionalProperties().size());
    assertEquals("metrics", horizontalPodAutoscaler.getSpec().getAdditionalProperties().keySet().iterator().next());
  }

  @Test
  public void shouldGetResolvedServices() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
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
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow savedWorkflow = workflowService.createWorkflow(workflow1);
    assertThat(savedWorkflow).isNotNull().hasFieldOrProperty("uuid");
    List<Service> resolvedServices = workflowService.getResolvedServices(savedWorkflow, null);
    assertThat(resolvedServices).isNotEmpty().extracting(service1 -> service1.getName()).contains(SERVICE_NAME);
  }

  @Test
  public void shouldGetResolvedTemplatizedServices() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withServiceId(SERVICE_ID)
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());
    Workflow workflow1 = createCanaryWorkflow();

    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("phase2-changed");

    TemplateExpression infraExpression = aTemplateExpression()
                                             .withFieldName("infraMappingId")
                                             .withExpression("${ServiceInfra_SSH}")
                                             .withMetadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                                             .build();
    TemplateExpression serviceExpression = aTemplateExpression()
                                               .withFieldName("serviceId")
                                               .withExpression("${Service}")
                                               .withMetadata(ImmutableMap.of("entityType", "SERVICE"))
                                               .build();
    workflowPhase2.setTemplateExpressions(asList(serviceExpression, infraExpression));

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase2);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    List<WorkflowPhase> workflowPhases3 =
        ((CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhase3 = workflowPhases3.get(workflowPhases3.size() - 1);
    assertThat(workflowPhase3).isEqualToComparingOnlyGivenFields(workflowPhase2, "uuid", "name");

    assertThat(workflowPhase3.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(workflow3.getOrchestrationWorkflow().getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(SERVICE, INFRASTRUCTURE_MAPPING);

    when(serviceResourceService.fetchServicesByUuids(APP_ID, asList(SERVICE_ID))).thenReturn(asList(service));
    List<Service> resolvedServices = workflowService.getResolvedServices(
        workflow3, ImmutableMap.of("Service", SERVICE_ID, "ServiceInfra_SSH", INFRA_MAPPING_ID));
    assertThat(resolvedServices).isNotEmpty().extracting(service1 -> service1.getName()).contains(SERVICE_NAME);
  }

  @Test
  public void shouldGetResolvedInfraMappings() {
    AwsInfrastructureMapping awsInfrastructureMapping = anAwsInfrastructureMapping()
                                                            .withServiceId(SERVICE_ID)
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withDeploymentType(SSH.name())
                                                            .withComputeProviderType(SettingVariableTypes.AWS.name())
                                                            .build();
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(awsInfrastructureMapping);
    when(infrastructureMappingService.getInfraStructureMappingsByUuids(APP_ID, asList(INFRA_MAPPING_ID)))
        .thenReturn(asList(awsInfrastructureMapping));
    Workflow workflow1 =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .addWorkflowPhase(aWorkflowPhase()
                                          .withInfraMappingId(INFRA_MAPPING_ID)
                                          .withServiceId(SERVICE_ID)
                                          .withDeploymentType(SSH)
                                          .build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    Workflow savedWorkflow = workflowService.createWorkflow(workflow1);
    assertThat(savedWorkflow).isNotNull().hasFieldOrProperty("uuid");
    List<InfrastructureMapping> resolvedInfraMappings = workflowService.getResolvedInfraMappings(savedWorkflow, null);
    assertThat(resolvedInfraMappings)
        .isNotEmpty()
        .extracting(infrastructureMapping -> infrastructureMapping.getUuid())
        .contains(INFRA_MAPPING_ID);
  }

  @Test
  public void shouldGetResolvedTemplatizedInfraMappings() {
    AwsInfrastructureMapping awsInfrastructureMapping = anAwsInfrastructureMapping()
                                                            .withServiceId(SERVICE_ID)
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withDeploymentType(SSH.name())
                                                            .withComputeProviderType(SettingVariableTypes.AWS.name())
                                                            .build();
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(awsInfrastructureMapping);
    when(infrastructureMappingService.getInfraStructureMappingsByUuids(APP_ID, asList(INFRA_MAPPING_ID)))
        .thenReturn(asList(awsInfrastructureMapping));

    Workflow workflow1 = createCanaryWorkflow();
    WorkflowPhase workflowPhase =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase);

    WorkflowPhase workflowPhase2 =
        aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).withServiceId(SERVICE_ID).build();
    workflowService.createWorkflowPhase(workflow1.getAppId(), workflow1.getUuid(), workflowPhase2);

    Workflow workflow2 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    assertThat(workflow2).isNotNull();

    List<WorkflowPhase> workflowPhases2 =
        ((CanaryOrchestrationWorkflow) workflow2.getOrchestrationWorkflow()).getWorkflowPhases();
    workflowPhase2 = workflowPhases2.get(workflowPhases2.size() - 1);
    workflowPhase2.setName("phase2-changed");

    TemplateExpression infraExpression = aTemplateExpression()
                                             .withFieldName("infraMappingId")
                                             .withExpression("${ServiceInfra_SSH}")
                                             .withMetadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                                             .build();
    TemplateExpression serviceExpression = aTemplateExpression()
                                               .withFieldName("serviceId")
                                               .withExpression("${Service}")
                                               .withMetadata(ImmutableMap.of("entityType", "SERVICE"))
                                               .build();
    workflowPhase2.setTemplateExpressions(asList(serviceExpression, infraExpression));

    workflowService.updateWorkflowPhase(workflow2.getAppId(), workflow2.getUuid(), workflowPhase2);

    Workflow workflow3 = workflowService.readWorkflow(workflow1.getAppId(), workflow1.getUuid());
    List<WorkflowPhase> workflowPhases3 =
        ((CanaryOrchestrationWorkflow) workflow3.getOrchestrationWorkflow()).getWorkflowPhases();
    WorkflowPhase workflowPhase3 = workflowPhases3.get(workflowPhases3.size() - 1);
    assertThat(workflowPhase3).isEqualToComparingOnlyGivenFields(workflowPhase2, "uuid", "name");

    assertThat(workflowPhase3.getInfraMappingId()).isNotNull();
    assertThat(workflowPhase3.getTemplateExpressions())
        .isNotEmpty()
        .extracting(templateExpression -> templateExpression.getFieldName())
        .contains("infraMappingId");
    assertThat(workflow3.getOrchestrationWorkflow().getUserVariables())
        .extracting(variable -> variable.getEntityType())
        .containsSequence(SERVICE, INFRASTRUCTURE_MAPPING);

    when(serviceResourceService.fetchServicesByUuids(APP_ID, asList(SERVICE_ID))).thenReturn(asList(service));
    List<InfrastructureMapping> resolvedInfraMappings = workflowService.getResolvedInfraMappings(
        workflow3, ImmutableMap.of("Service", SERVICE_ID, "ServiceInfra_SSH", INFRA_MAPPING_ID));
    assertThat(resolvedInfraMappings)
        .isNotEmpty()
        .extracting(infrastructureMapping -> infrastructureMapping.getUuid())
        .contains(INFRA_MAPPING_ID);
  }

  @Test
  public void shouldGetResolvedEnvironmentId() {
    Workflow workflow =
        aWorkflow()
            .withEnvId(ENV_ID)
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withServiceId(SERVICE_ID)
            .withInfraMappingId(INFRA_MAPPING_ID)
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withWorkflowPhases(
                        asList(aWorkflowPhase().withServiceId(SERVICE_ID).withInfraMappingId(INFRA_MAPPING_ID).build()))
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())

            .build();

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    Role role = aRole()
                    .withRoleType(RoleType.ACCOUNT_ADMIN)
                    .withUuid(ROLE_ID)
                    .withAccountId(application.getAccountId())
                    .build();
    List<NotificationGroup> notificationGroups = asList(aNotificationGroup()
                                                            .withUuid(NOTIFICATION_GROUP_ID)
                                                            .withAccountId(application.getAccountId())
                                                            .withRole(role)
                                                            .build());
    when(notificationSetupService.listNotificationGroups(
             application.getAccountId(), RoleType.ACCOUNT_ADMIN.getDisplayName()))
        .thenReturn(notificationGroups);

    Workflow workflow2 = workflowService.createWorkflow(workflow);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid").hasFieldOrPropertyWithValue("appId", APP_ID);

    assertThat(workflowService.resolveEnvironmentId(workflow2, ImmutableMap.of("Environment", ENV_ID)))
        .isNotNull()
        .isEqualTo(ENV_ID);
  }

  @Test
  public void shouldGetResolvedEnvironmentIdForTemplatizedWorkflow() {
    TemplateExpression envExpression = aTemplateExpression()
                                           .withFieldName("envId")
                                           .withExpression("${Environment}")
                                           .withMetadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
                                           .build();

    Workflow workflow =
        aWorkflow()
            .withEnvId(ENV_ID)
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withServiceId(SERVICE_ID)
            .withInfraMappingId(INFRA_MAPPING_ID)
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withWorkflowPhases(
                        asList(aWorkflowPhase().withServiceId(SERVICE_ID).withInfraMappingId(INFRA_MAPPING_ID).build()))
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .withTemplateExpressions(asList(envExpression))
            .build();

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    Role role = aRole()
                    .withRoleType(RoleType.ACCOUNT_ADMIN)
                    .withUuid(ROLE_ID)
                    .withAccountId(application.getAccountId())
                    .build();
    List<NotificationGroup> notificationGroups = asList(aNotificationGroup()
                                                            .withUuid(NOTIFICATION_GROUP_ID)
                                                            .withAccountId(application.getAccountId())
                                                            .withRole(role)
                                                            .build());
    when(notificationSetupService.listNotificationGroups(
             application.getAccountId(), RoleType.ACCOUNT_ADMIN.getDisplayName()))
        .thenReturn(notificationGroups);

    Workflow workflow2 = workflowService.createWorkflow(workflow);
    assertThat(workflow2).isNotNull().hasFieldOrProperty("uuid").hasFieldOrPropertyWithValue("appId", APP_ID);

    assertThat(workflowService.resolveEnvironmentId(workflow2, ImmutableMap.of("Environment", ENV_ID_CHANGED)))
        .isNotNull()
        .isEqualTo(ENV_ID_CHANGED);
  }

  private PhaseStep createPhaseStep(String uuid) {
    return aPhaseStep(PhaseStepType.CONTAINER_DEPLOY, DEPLOY_CONTAINERS)
        .addStep(aGraphNode()
                     .withId(generateUuid())
                     .withType(JENKINS.getName())
                     .withName(UPGRADE_CONTAINERS)
                     .addProperty(JENKINS.getName(), uuid)
                     .build())
        .build();
  }

  private Workflow createWorkflowWithParam(PhaseStep phaseStep) {
    return aWorkflow()
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withOrchestrationWorkflow(
            aCanaryOrchestrationWorkflow()
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .addWorkflowPhase(aWorkflowPhase()
                                      .withInfraMappingId(INFRA_MAPPING_ID)
                                      .withServiceId(SERVICE_ID)
                                      .addPhaseStep(phaseStep)
                                      .withDeploymentType(SSH)
                                      .build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .build();
  }

  @Test
  public void testSettingsServiceDeleting() {
    String uuid = generateUuid();

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType(SettingVariableTypes.AWS.name())
                        .build());

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withAppId("APP_ID")
                                            .withAccountId("ACCOUNT_ID")
                                            .withName("SETTING_NAME")
                                            .withCategory(Category.CONNECTOR)
                                            .withUuid(uuid)
                                            .withValue(JenkinsConfig.builder()
                                                           .jenkinsUrl(JENKINS_URL)
                                                           .password(PASSWORD)
                                                           .username(USER_NAME)
                                                           .accountId("ACCOUNT_ID")
                                                           .build())
                                            .build();

    // Create a workflow with a random Jenkins Id
    PhaseStep phaseStep = createPhaseStep(generateUuid());
    Workflow workflow = createWorkflowWithParam(phaseStep);
    workflowService.createWorkflow(workflow);
    assertNull(workflowService.settingsServiceDeleting(settingAttribute));

    // Create a workflow with a specific Jenkins Id
    phaseStep = createPhaseStep(uuid);
    workflow = createWorkflowWithParam(phaseStep);
    workflowService.createWorkflow(workflow);
    assertNotNull(workflowService.settingsServiceDeleting(settingAttribute).message());
  }
}
