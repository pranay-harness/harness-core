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
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureNotification;
import software.wings.beans.Notification;
import software.wings.beans.NotificationRule;
import software.wings.beans.WorkflowExecution;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.service.impl.WorkflowNotificationHelper;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.PhaseSubWorkflow;

import javax.inject.Inject;

/**
 * Created by anubhaw on 4/14/17.
 */
public class WorkflowNotificationHelperTest extends WingsBaseTest {
  @Mock private NotificationService notificationService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Inject @InjectMocks private WorkflowNotificationHelper workflowNotificationHelper;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private ExecutionContextImpl executionContext;

  @Before
  public void setUp() throws Exception {
    when(executionContext.getApp()).thenReturn(anApplication().withUuid(APP_ID).build());
    when(executionContext.getEnv())
        .thenReturn(anEnvironment().withUuid(ENV_ID).withName(ENV_NAME).withAppId(APP_ID).build());
    when(executionContext.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);
    when(executionContext.getWorkflowExecutionName()).thenReturn(WORKFLOW_NAME);
  }

  @Test
  public void shouldSendWorkflowStatusChangeNotification() {
    NotificationRule notificationRule = aNotificationRule()
                                            .withExecutionScope(ExecutionScope.WORKFLOW)
                                            .withConditions(asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS))
                                            .build();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withNotificationRules(asList(notificationRule)).build();

    when(((ExecutionContextImpl) executionContext).getStateMachine().getOrchestrationWorkflow())
        .thenReturn(canaryOrchestrationWorkflow);
    when(workflowExecutionService.getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution()
                        .withStartTs(System.currentTimeMillis())
                        .build());

    workflowNotificationHelper.sendWorkflowStatusChangeNotification(executionContext, ExecutionStatus.FAILED);

    verify(workflowExecutionService).getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID);
    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(asList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId())
        .isEqualTo(NotificationMessageType.WORKFLOW_FAILED_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders = ImmutableMap.of("WORKFLOW_NAME", WORKFLOW_NAME, "ENV_NAME", ENV_NAME);
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

    when(((ExecutionContextImpl) executionContext).getStateMachine().getOrchestrationWorkflow())
        .thenReturn(canaryOrchestrationWorkflow);
    when(workflowExecutionService.getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution()
                        .withStartTs(System.currentTimeMillis())
                        .build());

    PhaseSubWorkflow phaseSubWorkflow = Mockito.mock(PhaseSubWorkflow.class);
    when(phaseSubWorkflow.getName()).thenReturn("Phase1");
    workflowNotificationHelper.sendWorkflowPhaseStatusChangeNotification(
        executionContext, ExecutionStatus.FAILED, phaseSubWorkflow);

    verify(workflowExecutionService).getExecutionDetails(APP_ID, WORKFLOW_EXECUTION_ID);
    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), eq(asList(notificationRule)));
    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification).isInstanceOf(FailureNotification.class);
    assertThat(notification.getNotificationTemplateId())
        .isEqualTo(NotificationMessageType.WORKFLOW_PHASE_FAILED_NOTIFICATION.name());
    ImmutableMap<String, String> placeholders =
        ImmutableMap.of("WORKFLOW_NAME", WORKFLOW_NAME, "PHASE_NAME", "Phase1", "ENV_NAME", ENV_NAME);
    assertThat(notification.getNotificationTemplateVariables()).containsAllEntriesOf(placeholders);
  }
}
