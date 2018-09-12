package software.wings.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.ENTITY_CREATE_NOTIFICATION;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.BUILD_JOB_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.PIPELINE_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.WorkflowElement;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.WorkflowType;
import software.wings.beans.alert.AlertType;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.WorkflowStandardParams;

import java.util.Map;

/**
 * Created by anubhaw on 7/25/16.
 */
public class NotificationMessageResolverTest extends WingsBaseTest {
  @Inject NotificationMessageResolver notificationMessageResolver;

  @Mock private ExecutionContextImpl context;

  /**
   * Should get decorated notification message.
   */
  @Test
  public void shouldGetDecoratedNotificationMessage() {
    String decoratedNotificationMessage = NotificationMessageResolver.getDecoratedNotificationMessage(
        notificationMessageResolver.getWebTemplate(ENTITY_CREATE_NOTIFICATION.name()),
        ImmutableMap.of("ENTITY_TYPE", "SERVICE", "ENTITY_NAME", "Account", "DATE", "July 26, 2016"));
    assertThat(decoratedNotificationMessage).isNotEmpty();
  }

  /**
   * Should fail on in complete map.
   */
  @Test
  public void shouldFailOnInCompleteMap() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(()
                        -> NotificationMessageResolver.getDecoratedNotificationMessage(
                            notificationMessageResolver.getWebTemplate(ENTITY_CREATE_NOTIFICATION.name()),
                            ImmutableMap.of("ENTITY_TYPE", "SERVICE")))
        .withMessage("INVALID_ARGUMENT");
  }

  @Test
  public void testGetPlaceholderValues() {
    String ApprovalUrl =
        String.format("PORTAL_URL/#/account/%s/app/%s/pipeline-execution/%s/workflow-execution/undefined/details",
            ACCOUNT_ID, APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID);

    String ManualInterventionUrl = String.format("PORTAL_URL/#/account/%s/app/%s/env/%s/executions/%s/details",
        ACCOUNT_ID, APP_ID, ENV_ID, PIPELINE_WORKFLOW_EXECUTION_ID);
    Application app = anApplication().withAccountId(ACCOUNT_ID).withUuid(APP_ID).build();

    // Pipeline placeholder values
    when(context.getApp()).thenReturn(app);
    when(context.getWorkflowExecutionId()).thenReturn(PIPELINE_WORKFLOW_EXECUTION_ID);
    when(context.getEnv()).thenReturn(null);
    when(context.getWorkflowExecutionName()).thenReturn(BUILD_JOB_NAME);
    when(context.getWorkflowType()).thenReturn(WorkflowType.PIPELINE);

    Map<String, String> placeholderValues;
    placeholderValues = notificationMessageResolver.getPlaceholderValues(
        context, "", 500L, 100L, "1000", "", "", ExecutionStatus.ABORTED, AlertType.ApprovalNeeded);
    assertThat(placeholderValues.get("VERB")).isEqualTo("aborted");
    assertThat(placeholderValues.get("ENV")).isEqualTo("");
    assertThat(placeholderValues.get("WORKFLOW_NAME")).isEqualTo(BUILD_JOB_NAME);
    assertThat(placeholderValues.get("WORKFLOW_URL")).isEqualTo(ApprovalUrl);

    when(context.getEnv())
        .thenReturn(anEnvironment().withAppId(app.getUuid()).withName(ENV_NAME).withUuid(ENV_ID).build());
    placeholderValues = notificationMessageResolver.getPlaceholderValues(
        context, "", 500L, 100L, "1000", "", "", ExecutionStatus.PAUSED, AlertType.ManualInterventionNeeded);
    assertThat(placeholderValues.get("VERB")).isEqualTo("paused");
    assertThat(placeholderValues.get("ENV")).isEqualTo(ENV_NAME);
    assertThat(placeholderValues.get("WORKFLOW_NAME")).isEqualTo(BUILD_JOB_NAME);
    assertThat(placeholderValues.get("WORKFLOW_URL")).isEqualTo(ManualInterventionUrl);

    // Manual Intervention URL (No Environment)
    ManualInterventionUrl = String.format("PORTAL_URL/#/account/%s/app/%s/env/empty/executions/%s/details", ACCOUNT_ID,
        APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID);
    when(context.getEnv()).thenReturn(null);
    placeholderValues = notificationMessageResolver.getPlaceholderValues(
        context, "", 500L, 100L, "1000", "", "", ExecutionStatus.PAUSED, AlertType.ManualInterventionNeeded);
    assertThat(placeholderValues.get("WORKFLOW_URL")).isEqualTo(ManualInterventionUrl);

    // Direct workflow placeholder values
    Environment env = anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).build();
    when(context.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);
    when(context.getEnv()).thenReturn(env);
    when(context.getWorkflowExecutionName()).thenReturn(BUILD_JOB_NAME);
    when(context.getWorkflowType()).thenReturn(WorkflowType.ORCHESTRATION);
    ApprovalUrl = String.format("PORTAL_URL/#/account/%s/app/%s/env/%s/executions/%s/details", ACCOUNT_ID, APP_ID,
        ENV_ID, WORKFLOW_EXECUTION_ID);

    placeholderValues = notificationMessageResolver.getPlaceholderValues(
        context, "", 500L, 100L, "1000", "", "", ExecutionStatus.ABORTED, AlertType.ApprovalNeeded);
    assertThat(placeholderValues.get("VERB")).isEqualTo("aborted");
    assertThat(placeholderValues.get("WORKFLOW_NAME")).isEqualTo(BUILD_JOB_NAME);
    assertThat(placeholderValues.get("WORKFLOW_URL")).isEqualTo(ApprovalUrl);

    // Direct workflow placeholder values (No Environment)
    when(context.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);
    when(context.getWorkflowExecutionName()).thenReturn(BUILD_JOB_NAME);
    when(context.getWorkflowType()).thenReturn(WorkflowType.ORCHESTRATION);
    when(context.getEnv()).thenReturn(null);
    ApprovalUrl = String.format(
        "PORTAL_URL/#/account/%s/app/%s/env/empty/executions/%s/details", ACCOUNT_ID, APP_ID, WORKFLOW_EXECUTION_ID);

    placeholderValues = notificationMessageResolver.getPlaceholderValues(
        context, "", 500L, 100L, "1000", "", "", ExecutionStatus.ABORTED, AlertType.ApprovalNeeded);
    assertThat(placeholderValues.get("VERB")).isEqualTo("aborted");
    assertThat(placeholderValues.get("WORKFLOW_NAME")).isEqualTo(BUILD_JOB_NAME);
    assertThat(placeholderValues.get("WORKFLOW_URL")).isEqualTo(ApprovalUrl);

    // Workflow in pipeline placeholder values
    when(context.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);
    WorkflowStandardParams workflowStandardParams =
        aWorkflowStandardParams()
            .withAppId(APP_ID)
            .withEnvId(ENV_ID)
            .withWorkflowElement(WorkflowElement.builder().pipelineDeploymentUuid(PIPELINE_EXECUTION_ID).build())
            .build();
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
    when(context.getEnv()).thenReturn(env);
    when(context.getWorkflowExecutionName()).thenReturn(BUILD_JOB_NAME);
    when(context.getWorkflowType()).thenReturn(WorkflowType.ORCHESTRATION);
    ApprovalUrl = String.format("PORTAL_URL/#/account/%s/app/%s/pipeline-execution/%s/workflow-execution/%s/details",
        ACCOUNT_ID, APP_ID, PIPELINE_EXECUTION_ID, WORKFLOW_EXECUTION_ID);

    placeholderValues = notificationMessageResolver.getPlaceholderValues(
        context, "", 500L, 100L, "1000", "", "", ExecutionStatus.ABORTED, AlertType.ApprovalNeeded);
    assertThat(placeholderValues.get("VERB")).isEqualTo("aborted");
    assertThat(placeholderValues.get("WORKFLOW_NAME")).isEqualTo(BUILD_JOB_NAME);
    assertThat(placeholderValues.get("WORKFLOW_URL")).isEqualTo(ApprovalUrl);
  }
}
