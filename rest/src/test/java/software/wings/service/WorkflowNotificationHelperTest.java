package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
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
import software.wings.WingsBaseTest;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureNotification;
import software.wings.beans.Notification;
import software.wings.beans.NotificationRule;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.service.impl.WorkflowNotificationHelper;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.PhaseSubWorkflow;

/**
 * Created by anubhaw on 4/14/17.
 */
public class WorkflowNotificationHelperTest extends WingsBaseTest {
  @Mock private NotificationService notificationService;
  @Mock private WorkflowService workflowService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Inject @InjectMocks private WorkflowNotificationHelper workflowNotificationHelper;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private ExecutionContextImpl executionContext;

  @Before
  public void setUp() throws Exception {
    when(executionContext.getApp()).thenReturn(anApplication().withUuid(APP_ID).build());
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
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID))
        .thenReturn(aWorkflow()
                        .withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build())
                        .withServices(ImmutableList.of(
                            aService().withUuid("service-1").build(), aService().withUuid("service-2").build()))
                        .build());
    when(workflowExecutionService.getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(aWorkflowExecution().withTriggeredBy(EmbeddedUser.builder().name(USER_NAME).build()).build());
  }

  @Test
  public void shouldSendWorkflowStatusChangeNotification() {
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
        .isEqualTo(NotificationMessageType.WORKFLOW_FAILED_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders = ImmutableMap.of("WORKFLOW_NAME", WORKFLOW_NAME, "ARTIFACTS",
        "artifact-1 (build# build-1), artifact-2 (build# build-2)", "USER_NAME", USER_NAME, "ENV_NAME", ENV_NAME);
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }

  @Test
  public void shouldSendWorkflowPhaseStatusChangeNotification() {
    NotificationRule notificationRule = aNotificationRule()
                                            .withExecutionScope(ExecutionScope.WORKFLOW_PHASE)
                                            .withConditions(asList(ExecutionStatus.FAILED))
                                            .build();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withNotificationRules(asList(notificationRule)).build();

    when(executionContext.getStateMachine().getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);

    PhaseSubWorkflow phaseSubWorkflow = Mockito.mock(PhaseSubWorkflow.class);
    when(phaseSubWorkflow.getName()).thenReturn("Phase1");
    when(phaseSubWorkflow.getServiceId()).thenReturn("service-2");
    workflowNotificationHelper.sendWorkflowPhaseStatusChangeNotification(
        executionContext, ExecutionStatus.FAILED, phaseSubWorkflow);

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(asList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId())
        .isEqualTo(NotificationMessageType.WORKFLOW_PHASE_FAILED_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders = ImmutableMap.of("WORKFLOW_NAME", WORKFLOW_NAME, "PHASE_NAME", "Phase1",
        "ARTIFACTS", "artifact-2 (build# build-2)", "USER_NAME", USER_NAME, "ENV_NAME", ENV_NAME);
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
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(asList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId())
        .isEqualTo(NotificationMessageType.WORKFLOW_FAILED_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders = ImmutableMap.of(
        "WORKFLOW_NAME", WORKFLOW_NAME, "ARTIFACTS", "no artifacts", "USER_NAME", USER_NAME, "ENV_NAME", ENV_NAME);
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }
}
