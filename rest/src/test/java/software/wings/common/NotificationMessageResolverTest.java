package software.wings.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.ENTITY_CREATE_NOTIFICATION;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.BUILD_JOB_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.alert.AlertType;
import software.wings.exception.WingsException;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;

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
    final String ApprovalUrl =
        String.format("PORTAL_URL/#/account/%s/app/%s/pipeline-execution/%s/workflow-execution/undefined/details",
            ACCOUNT_ID, APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID);

    final String ManualInterventionUrl = String.format("PORTAL_URL/#/account/%s/app/%s/env/%s/executions/%s/details",
        ACCOUNT_ID, APP_ID, ENV_ID, PIPELINE_WORKFLOW_EXECUTION_ID);
    Application app = anApplication().withAccountId(ACCOUNT_ID).withUuid(APP_ID).build();

    when(context.getApp()).thenReturn(app);
    when(context.getWorkflowExecutionId()).thenReturn(PIPELINE_WORKFLOW_EXECUTION_ID);
    when(context.getEnv()).thenReturn(null);
    when(context.getWorkflowExecutionName()).thenReturn(BUILD_JOB_NAME);

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
  }
}
