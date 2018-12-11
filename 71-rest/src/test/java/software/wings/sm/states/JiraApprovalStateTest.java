package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.JiraExecutionData.JiraApprovalActionType.CREATE_WEBHOOK;
import static software.wings.api.JiraExecutionData.JiraApprovalActionType.WAIT_JIRA_APPROVAL;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_NEEDED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_STATE_CHANGE_NOTIFICATION;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.BUILD_JOB_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_GROUP_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.protocol.ResponseData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.JiraExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.JiraConfig;
import software.wings.beans.Notification;
import software.wings.beans.NotificationRule;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.beans.WorkflowType;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.WorkflowStandardParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JiraApprovalStateTest extends WingsBaseTest {
  private static final Activity ACTIVITY_WITH_ID = Activity.builder().build();

  static {
    ACTIVITY_WITH_ID.setUuid(ACTIVITY_ID);
  }

  @Mock private ExecutionContextImpl context;
  @Mock private AlertService alertService;
  @Mock private NotificationService notificationService;
  @Mock private ActivityService activityService;
  @Mock private NotificationSetupService notificationSetupService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private NotificationMessageResolver notificationMessageResolver;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private JiraHelperService jiraHelperService;
  @Mock private SecretManager secretManager;
  @Mock private DelegateService delegateService;

  @InjectMocks private JiraApprovalState approvalState = new JiraApprovalState("JiraApprovalState");
  @Captor private ArgumentCaptor<List<NotificationRule>> notificationRuleArgumentCaptor;

  private static final WorkflowStandardParams WORKFLOW_STANDARD_PARAMS =
      aWorkflowStandardParams().withArtifactIds(asList(ARTIFACT_ID)).build();

  @Before
  public void setUp() throws Exception {
    when(context.getApp())
        .thenReturn(anApplication().withAccountId(ACCOUNT_ID).withUuid(APP_ID).withAppId(APP_ID).build());
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.getEnv())
        .thenReturn(anEnvironment()
                        .withAppId(APP_ID)
                        .withEnvironmentType(EnvironmentType.ALL)
                        .withUuid(ENV_ID)
                        .withName(ENV_NAME)
                        .build());
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(WORKFLOW_STANDARD_PARAMS);
    when(context.getWorkflowExecutionId()).thenReturn(PIPELINE_WORKFLOW_EXECUTION_ID);
    when(context.getWorkflowExecutionName()).thenReturn(BUILD_JOB_NAME);
    when(context.getStateExecutionInstance())
        .thenReturn(aStateExecutionInstance().withExecutionType(WorkflowType.PIPELINE).build());
    JiraExecutionData executionData = JiraExecutionData.builder().build();
    executionData.setStartTs(1L);
    when(context.getStateExecutionData()).thenReturn(executionData);
    when(activityService.save(any(Activity.class))).thenReturn(ACTIVITY_WITH_ID);
    when(wingsPersistence.get(SettingAttribute.class, "test"))
        .thenReturn(aSettingAttribute().withValue(createJiraConfig("test")).build());

    when(workflowExecutionService.getWorkflowExecution(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID))
        .thenReturn(WorkflowExecutionBuilder.aWorkflowExecution()
                        .withAppId(APP_ID)
                        .withTriggeredBy(EmbeddedUser.builder().name(USER_NAME).uuid(USER_NAME).build())
                        .withCreatedAt(70L)
                        .build());

    when(notificationSetupService.listDefaultNotificationGroup(any()))
        .thenReturn(asList(aNotificationGroup()
                               .withName(USER_NAME)
                               .withUuid(NOTIFICATION_GROUP_ID)
                               .withAccountId(ACCOUNT_ID)
                               .build()));

    Map<String, String> placeholders = new HashMap<>();
    when(notificationMessageResolver.getPlaceholderValues(
             any(), any(), any(Long.class), any(Long.class), any(), any(), any(), any(), any()))
        .thenReturn(placeholders);
    approvalState.setJiraConnectorId("test");
  }

  private JiraConfig createJiraConfig(String jiraConnectorId) {
    JiraConfig jiraConfig = new JiraConfig();
    jiraConfig.setAccountId(ACCOUNT_ID);
    jiraConfig.setBaseUrl("http://localhost:8080/");
    jiraConfig.setUsername("pooja.singhal");
    jiraConfig.setPassword("tulara@335".toCharArray());

    return jiraConfig;
  }

  @Test
  public void shouldExecute() {
    PageResponse pageResponse = new PageResponse();
    pageResponse.setResponse(asList(User.Builder.anUser().build()));
    approvalState.setCreateJira(true);
    approvalState.setApprovalField("status");
    approvalState.setApprovalValue("APPROVED");
    ExecutionResponse executionResponse = approvalState.execute(context);
    assertThat(executionResponse.isAsync()).isTrue();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SUCCESS);
  }

  @Test
  public void shouldHandleAsyncResponseCreateWebhook() {
    PageResponse pageResponse = new PageResponse();
    pageResponse.setResponse(asList(User.Builder.anUser().build()));
    ResponseData jiraExecutionData = getResponseDataForWebhookCreation();
    Map<String, ResponseData> responseDataMap = new HashMap<>();
    responseDataMap.put(ACTIVITY_ID, jiraExecutionData);
    ExecutionResponse executionResponse = approvalState.handleAsyncResponse(context, responseDataMap);

    verify(alertService)
        .openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), any(ApprovalNeededAlert.class));

    verifyNotificationArguments(APPROVAL_NEEDED_NOTIFICATION);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(PAUSED);
    assertThat(executionResponse.isAsync()).isTrue();
  }

  @Test
  public void shouldHandleAsyncResponseJiraApproved() {
    PageResponse pageResponse = new PageResponse();
    pageResponse.setResponse(asList(User.Builder.anUser().build()));
    ResponseData jiraExecutionData = getResponseDataJiraApproval();
    Map<String, ResponseData> responseDataMap = new HashMap<>();
    responseDataMap.put(generateUuid(), jiraExecutionData);
    ExecutionResponse executionResponse = approvalState.handleAsyncResponse(context, responseDataMap);

    verify(alertService)
        .closeAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), any(ApprovalNeededAlert.class));

    verifyNotificationArguments(APPROVAL_STATE_CHANGE_NOTIFICATION);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(executionResponse.isAsync()).isTrue();
  }

  private ResponseData getResponseDataForWebhookCreation() {
    String webhookUrl = "testUrl";

    return JiraExecutionData.builder()
        .executionStatus(ExecutionStatus.SUCCESS)
        .webhookUrl(webhookUrl)
        .approvalId(generateUuid())
        .activityId(ACTIVITY_ID)
        .jiraApprovalActionType(CREATE_WEBHOOK)
        .build();
  }

  private ResponseData getResponseDataJiraApproval() {
    JiraExecutionData executionData = (JiraExecutionData) getResponseDataForWebhookCreation();
    executionData.setStatus(ExecutionStatus.SUCCESS);
    executionData.setApprovedOn(System.currentTimeMillis());
    executionData.setApprovedBy(new EmbeddedUser(null, "pooja.singhal", "pooja.singhal@harness.io"));
    executionData.setJiraApprovalActionType(WAIT_JIRA_APPROVAL);
    return executionData;
  }

  private void verifyNotificationArguments(NotificationMessageType notificationMessageType) {
    verify(notificationService).sendNotificationAsync(any(Notification.class), singletonList(any()));

    ArgumentCaptor<Notification> notificationArgumentCaptor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationService)
        .sendNotificationAsync(notificationArgumentCaptor.capture(), notificationRuleArgumentCaptor.capture());

    Notification notification = notificationArgumentCaptor.getAllValues().get(0);
    assertThat(notification.getNotificationTemplateId()).isEqualTo(notificationMessageType.name());
    assertThat(notification.getAccountId()).isEqualTo(ACCOUNT_ID);

    NotificationRule notificationRule = notificationRuleArgumentCaptor.getValue().get(0);
    assertThat(notificationRule.getNotificationGroups().get(0).getName()).isEqualTo(USER_NAME);
    assertThat(notificationRule.getNotificationGroups().get(0).getUuid()).isEqualTo(NOTIFICATION_GROUP_ID);
    assertThat(notificationRule.getNotificationGroups().get(0).getAccountId()).isEqualTo(ACCOUNT_ID);
  }
}
