package software.wings.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.BuildWorkflow.BuildOrchestrationWorkflowBuilder.aBuildOrchestrationWorkflow;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_NOTIFICATION;
import static software.wings.sm.PipelineSummary.Builder.aPipelineSummary;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.PIPELINE_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mongodb.morphia.query.FieldEnd;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.BuildWorkflow;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureNotification;
import software.wings.beans.InformationNotification;
import software.wings.beans.Notification;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.Service;
import software.wings.beans.WorkflowExecution;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.HQuery;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.WorkflowNotificationHelper;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.states.PhaseSubWorkflow;

/**
 * Created by anubhaw on 4/14/17.
 */
public class WorkflowNotificationHelperTest extends WingsBaseTest {
  private static final String BASE_URL = "https://env.harness.io/";
  private static final String EXPECTED_WORKFLOW_URL =
      "https://env.harness.io/#/account/ACCOUNT_ID/app/APP_ID/env/ENV_ID/executions/WORKFLOW_EXECUTION_ID/details";
  private static final String EXPECTED_PIPELINE_URL =
      "https://env.harness.io/#/account/ACCOUNT_ID/app/APP_ID/pipeline-execution/PIPELINE_EXECUTION_ID/workflow-execution/WORKFLOW_EXECUTION_ID/details";

  @Mock private NotificationService notificationService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private MainConfiguration configuration;
  @Mock private WingsPersistence wingsPersistence;
  @Inject @InjectMocks private WorkflowNotificationHelper workflowNotificationHelper;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private ExecutionContextImpl executionContext;
  @Mock private HQuery<StateExecutionInstance> stateExecutionInstanceQuery;
  @Mock private FieldEnd stateExecutionInstanceEnd;
  @Mock private HQuery<WorkflowExecution> workflowExecutionQuery;
  @Mock private FieldEnd workflowExecutionEnd;

  @Before
  public void setUp() throws Exception {
    when(executionContext.getApp())
        .thenReturn(anApplication().withAccountId(ACCOUNT_ID).withUuid(APP_ID).withName(APP_NAME).build());
    when(executionContext.getArtifacts())
        .thenReturn(ImmutableList.of(anArtifact()
                                         .withArtifactSourceName("artifact-1")
                                         .withMetadata(ImmutableMap.of(BUILD_NO, "build-1"))
                                         .withServiceIds(ImmutableList.of("service-1"))
                                         .build(),
            anArtifact()
                .withArtifactSourceName("artifact-2")
                .withMetadata(ImmutableMap.of(BUILD_NO, "build-2"))
                .withServiceIds(ImmutableList.of("service-2"))
                .build(),
            anArtifact()
                .withArtifactSourceName("artifact-3")
                .withMetadata(ImmutableMap.of(BUILD_NO, "build-3"))
                .withServiceIds(ImmutableList.of("service-3"))
                .build()));
    when(executionContext.getEnv())
        .thenReturn(anEnvironment().withUuid(ENV_ID).withName(ENV_NAME).withAppId(APP_ID).build());
    when(executionContext.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);
    when(executionContext.getWorkflowExecutionName()).thenReturn(WORKFLOW_NAME);
    when(executionContext.getAppId()).thenReturn(APP_ID);
    when(executionContext.getWorkflowId()).thenReturn(WORKFLOW_ID);
    when(workflowExecutionService.getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID, true, emptySet()))
        .thenReturn(aWorkflowExecution()
                        .withServiceIds(asList("service-1", "service-2"))
                        .withTriggeredBy(EmbeddedUser.builder().name(USER_NAME).build())
                        .build());
    PortalConfig portalConfig = new PortalConfig();
    portalConfig.setUrl(BASE_URL);
    when(configuration.getPortal()).thenReturn(portalConfig);
    when(serviceResourceService.get(APP_ID, "service-1"))
        .thenReturn(Service.builder().uuid("service-1").name("Service One").build());
    when(serviceResourceService.get(APP_ID, "service-2"))
        .thenReturn(Service.builder().uuid("service-2").name("Service Two").build());
    when(wingsPersistence.createQuery(StateExecutionInstance.class)).thenReturn(stateExecutionInstanceQuery);
    when(stateExecutionInstanceQuery.filter(any(), any())).thenReturn(stateExecutionInstanceQuery);
    when(stateExecutionInstanceQuery.get())
        .thenReturn(aStateExecutionInstance().withStartTs(1234L).withEndTs(2345L).build());
    when(wingsPersistence.createQuery(WorkflowExecution.class)).thenReturn(workflowExecutionQuery);
    when(workflowExecutionQuery.filter(any(), any())).thenReturn(workflowExecutionQuery);
  }

  @Test
  public void shouldSendWorkflowStatusChangeNotification() {
    NotificationRule notificationRule = aNotificationRule()
                                            .withExecutionScope(ExecutionScope.WORKFLOW)
                                            .withConditions(asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS))
                                            .build();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withNotificationRules(singletonList(notificationRule)).build();

    when(executionContext.getStateMachine().getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);
    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.SUCCESS);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(singletonList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(InformationNotification.class);
    assertThat(notification.getNotificationTemplateId()).isEqualTo(WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders =
        ImmutableMap.<String, String>builder()
            .put("WORKFLOW_NAME", WORKFLOW_NAME)
            .put("WORKFLOW_URL", EXPECTED_WORKFLOW_URL)
            .put("VERB", "completed")
            .put("PHASE_NAME", "")
            .put("ARTIFACTS", "Service One: artifact-1 (build# build-1), Service Two: artifact-2 (build# build-2)")
            .put("USER_NAME", USER_NAME)
            .put("PIPELINE", "")
            .put("APP_NAME", APP_NAME)
            .put("ENV_NAME", ENV_NAME)
            .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  @Test
  public void shouldSendWorkflowStatusChangeNotificationPipeline() {
    when(workflowExecutionService.getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID, true, emptySet()))
        .thenReturn(
            aWorkflowExecution()
                .withServiceIds(asList("service-1", "service-2"))
                .withTriggeredBy(EmbeddedUser.builder().name(USER_NAME).build())
                .withPipelineExecutionId(PIPELINE_EXECUTION_ID)
                .withPipelineSummary(
                    aPipelineSummary().withPipelineId(PIPELINE_EXECUTION_ID).withPipelineName("Pipeline Name").build())
                .build());
    NotificationRule notificationRule = aNotificationRule()
                                            .withExecutionScope(ExecutionScope.WORKFLOW)
                                            .withConditions(asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS))
                                            .build();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withNotificationRules(asList(notificationRule)).build();

    when(executionContext.getStateMachine().getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);
    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.FAILED);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(singletonList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId()).isEqualTo(WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders =
        ImmutableMap.<String, String>builder()
            .put("WORKFLOW_NAME", WORKFLOW_NAME)
            .put("WORKFLOW_URL", EXPECTED_WORKFLOW_URL)
            .put("VERB", "failed")
            .put("PHASE_NAME", "")
            .put("ARTIFACTS", "Service One: artifact-1 (build# build-1), Service Two: artifact-2 (build# build-2)")
            .put("USER_NAME", USER_NAME)
            .put("PIPELINE", " as part of <<<" + EXPECTED_PIPELINE_URL + "|-|Pipeline Name>>> pipeline")
            .put("APP_NAME", APP_NAME)
            .put("ENV_NAME", ENV_NAME)
            .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  @Test
  public void shouldSendWorkflowPhaseStatusChangeNotification() {
    NotificationRule notificationRule = aNotificationRule()
                                            .withExecutionScope(ExecutionScope.WORKFLOW_PHASE)
                                            .withConditions(singletonList(ExecutionStatus.FAILED))
                                            .build();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withNotificationRules(singletonList(notificationRule)).build();

    when(executionContext.getStateMachine().getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);

    PhaseSubWorkflow phaseSubWorkflow = Mockito.mock(PhaseSubWorkflow.class);
    when(phaseSubWorkflow.getName()).thenReturn("Phase1");
    when(phaseSubWorkflow.getServiceId()).thenReturn("service-2");
    workflowNotificationHelper.sendWorkflowPhaseStatusChangeNotification(
        executionContext, ExecutionStatus.FAILED, phaseSubWorkflow);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(singletonList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId()).isEqualTo(WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders = ImmutableMap.<String, String>builder()
                                                    .put("WORKFLOW_NAME", WORKFLOW_NAME)
                                                    .put("WORKFLOW_URL", EXPECTED_WORKFLOW_URL)
                                                    .put("VERB", "failed")
                                                    .put("PHASE_NAME", "Phase1 of ")
                                                    .put("ARTIFACTS", "Service Two: artifact-2 (build# build-2)")
                                                    .put("USER_NAME", USER_NAME)
                                                    .put("PIPELINE", "")
                                                    .put("ENV_NAME", ENV_NAME)
                                                    .put("APP_NAME", APP_NAME)
                                                    .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  @Test
  public void shouldSendWorkflowStatusChangeNotificationNoArtifacts() {
    when(executionContext.getArtifacts()).thenReturn(null);
    NotificationRule notificationRule = aNotificationRule()
                                            .withExecutionScope(ExecutionScope.WORKFLOW)
                                            .withConditions(asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS))
                                            .build();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withNotificationRules(asList(notificationRule)).build();

    when(executionContext.getStateMachine().getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);

    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.FAILED);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(singletonList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId()).isEqualTo(WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders =
        ImmutableMap.<String, String>builder()
            .put("WORKFLOW_NAME", WORKFLOW_NAME)
            .put("WORKFLOW_URL", EXPECTED_WORKFLOW_URL)
            .put("VERB", "failed")
            .put("PHASE_NAME", "")
            .put("ARTIFACTS", "Service One: no artifact, Service Two: no artifact")
            .put("USER_NAME", USER_NAME)
            .put("PIPELINE", "")
            .put("ENV_NAME", ENV_NAME)
            .put("APP_NAME", APP_NAME)
            .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  @Test
  public void shouldSendWorkflowStatusChangeNotificationSomeArtifacts() {
    when(executionContext.getArtifacts())
        .thenReturn(ImmutableList.of(anArtifact()
                                         .withArtifactSourceName("artifact-1")
                                         .withMetadata(ImmutableMap.of(BUILD_NO, "build-1"))
                                         .withServiceIds(ImmutableList.of("service-1"))
                                         .build()));
    NotificationRule notificationRule = aNotificationRule()
                                            .withExecutionScope(ExecutionScope.WORKFLOW)
                                            .withConditions(asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS))
                                            .build();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withNotificationRules(singletonList(notificationRule)).build();

    when(executionContext.getStateMachine().getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);

    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.FAILED);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(singletonList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId()).isEqualTo(WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders =
        ImmutableMap.<String, String>builder()
            .put("WORKFLOW_NAME", WORKFLOW_NAME)
            .put("WORKFLOW_URL", EXPECTED_WORKFLOW_URL)
            .put("VERB", "failed")
            .put("PHASE_NAME", "")
            .put("ARTIFACTS", "Service One: artifact-1 (build# build-1), Service Two: no artifact")
            .put("USER_NAME", USER_NAME)
            .put("PIPELINE", "")
            .put("ENV_NAME", ENV_NAME)
            .put("APP_NAME", APP_NAME)
            .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  @Test
  public void shouldSendWorkflowStatusChangeNotificationNoServices() {
    when(executionContext.getArtifacts()).thenReturn(null);
    when(workflowExecutionService.getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID, true, emptySet()))
        .thenReturn(aWorkflowExecution().withTriggeredBy(EmbeddedUser.builder().name(USER_NAME).build()).build());
    NotificationRule notificationRule = aNotificationRule()
                                            .withExecutionScope(ExecutionScope.WORKFLOW)
                                            .withConditions(asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS))
                                            .build();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withNotificationRules(asList(notificationRule)).build();

    when(executionContext.getStateMachine().getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);

    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.FAILED);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(asList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId())
        .isEqualTo(NotificationMessageType.WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders = ImmutableMap.<String, String>builder()
                                                    .put("WORKFLOW_NAME", WORKFLOW_NAME)
                                                    .put("WORKFLOW_URL", EXPECTED_WORKFLOW_URL)
                                                    .put("VERB", "failed")
                                                    .put("PHASE_NAME", "")
                                                    .put("ARTIFACTS", "no services")
                                                    .put("USER_NAME", USER_NAME)
                                                    .put("PIPELINE", "")
                                                    .put("ENV_NAME", ENV_NAME)
                                                    .put("APP_NAME", APP_NAME)
                                                    .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  @Test
  public void shouldSendWorkflowStatusChangeNotificationBuildWorkflow() {
    when(executionContext.getEnv()).thenReturn(null);
    when(executionContext.getArtifacts()).thenReturn(null);
    when(workflowExecutionService.getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID, true, emptySet()))
        .thenReturn(aWorkflowExecution().withTriggeredBy(EmbeddedUser.builder().name(USER_NAME).build()).build());
    NotificationRule notificationRule = aNotificationRule()
                                            .withExecutionScope(ExecutionScope.WORKFLOW)
                                            .withConditions(asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS))
                                            .build();
    BuildWorkflow buildWorkflow =
        aBuildOrchestrationWorkflow().withNotificationRules(singletonList(notificationRule)).build();

    when(executionContext.getStateMachine().getOrchestrationWorkflow()).thenReturn(buildWorkflow);
    when(executionContext.getOrchestrationWorkflowType()).thenReturn(OrchestrationWorkflowType.BUILD);

    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.FAILED);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(singletonList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId())
        .isEqualTo(NotificationMessageType.WORKFLOW_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders =
        ImmutableMap.<String, String>builder()
            .put("WORKFLOW_NAME", WORKFLOW_NAME)
            .put("WORKFLOW_URL",
                "https://env.harness.io/#/account/ACCOUNT_ID/app/APP_ID/env/build/executions/WORKFLOW_EXECUTION_ID/details")
            .put("VERB", "failed")
            .put("PHASE_NAME", "")
            .put("ARTIFACTS", "no services")
            .put("USER_NAME", USER_NAME)
            .put("PIPELINE", "")
            .put("ENV_NAME", "no environment")
            .put("APP_NAME", APP_NAME)
            .build();
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }
}
