package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.alert.AlertType.ApprovalNeeded;
import static software.wings.common.Constants.DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_EXPIRED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_NEEDED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_STATE_CHANGE_NOTIFICATION;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.ExecutionStatus.ABORTED;
import static software.wings.sm.ExecutionStatus.EXPIRED;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.SKIPPED;

import com.google.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.Application;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ApprovalState extends State {
  @Getter @Setter private String groupName;
  @Getter @Setter private List<String> userGroups = new ArrayList<>();
  @Getter @Setter private boolean disable;

  @Inject private AlertService alertService;
  @Inject private NotificationService notificationService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private NotificationMessageResolver notificationMessageResolver;
  @Inject private NotificationDispatcherService notificationDispatcherService;
  @Inject private EmailNotificationService emailNotificationService;
  @Inject private UserGroupService userGroupService;
  @Inject private UserService userService;

  public ApprovalState(String name) {
    super(name, StateType.APPROVAL.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String approvalId = generateUuid();
    ApprovalStateExecutionData executionData =
        ApprovalStateExecutionData.builder().approvalId(approvalId).userGroups(userGroups).build();

    if (disable) {
      return anExecutionResponse()
          .withExecutionStatus(SKIPPED)
          .withErrorMessage("Approval step is disabled. Approval is skipped.")
          .withStateExecutionData(executionData)
          .build();
    }

    // Open an alert
    Application app = ((ExecutionContextImpl) context).getApp();
    ApprovalNeededAlert approvalNeededAlert = ApprovalNeededAlert.builder()
                                                  .executionId(context.getWorkflowExecutionId())
                                                  .approvalId(approvalId)
                                                  .name(context.getWorkflowExecutionName())
                                                  .build();
    alertService.openAlert(app.getAccountId(), app.getUuid(), ApprovalNeeded, approvalNeededAlert);

    Map<String, String> placeholderValues = getPlaceholderValues(context, "", PAUSED);
    sendApprovalNotification(app.getAccountId(), APPROVAL_NEEDED_NOTIFICATION, placeholderValues);
    sendEmailToUserGroupMembers(userGroups, app.getAccountId(), APPROVAL_NEEDED_NOTIFICATION, placeholderValues);

    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetailsWithoutGraph(app.getAppId(), context.getWorkflowExecutionId());
    if (workflowExecution != null) {
      if (workflowExecution.getPipelineSummary() != null) {
        executionData.setWorkflowId(workflowExecution.getPipelineSummary().getPipelineId());
      } else {
        executionData.setWorkflowId(workflowExecution.getWorkflowId());
      }
    }
    executionData.setAppId(app.getAppId());

    return anExecutionResponse()
        .withAsync(true)
        .withExecutionStatus(PAUSED)
        .withCorrelationIds(asList(approvalId))
        .withStateExecutionData(executionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ApprovalStateExecutionData approvalNotifyResponse =
        (ApprovalStateExecutionData) response.values().iterator().next();

    ApprovalStateExecutionData executionData = (ApprovalStateExecutionData) context.getStateExecutionData();
    executionData.setApprovedBy(approvalNotifyResponse.getApprovedBy());
    executionData.setComments(approvalNotifyResponse.getComments());
    executionData.setApprovedOn(System.currentTimeMillis());

    // Close the alert
    Application app = ((ExecutionContextImpl) context).getApp();
    alertService.closeAlert(app.getAccountId(), app.getUuid(), ApprovalNeeded,
        ApprovalNeededAlert.builder()
            .executionId(context.getWorkflowExecutionId())
            .approvalId(approvalNotifyResponse.getApprovalId())
            .build());

    Map<String, String> placeholderValues = getPlaceholderValues(
        context, approvalNotifyResponse.getApprovedBy().getName(), approvalNotifyResponse.getStatus());
    sendApprovalNotification(app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues);
    sendEmailToUserGroupMembers(userGroups, app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues);

    return anExecutionResponse()
        .withStateExecutionData(executionData)
        .withExecutionStatus(approvalNotifyResponse.getStatus())
        .build();
  }

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {
    if (context == null || context.getStateExecutionData() == null) {
      return;
    }

    Application app = ((ExecutionContextImpl) context).getApp();
    Integer timeout = getTimeoutMillis();
    Long startTimeMillis = context.getStateExecutionData().getStartTs();
    Long currentTimeMillis = System.currentTimeMillis();

    String errorMsg = "";
    String approvalType = "";
    if (((ExecutionContextImpl) context).getStateExecutionInstance() != null
        && ((ExecutionContextImpl) context).getStateExecutionInstance().getExecutionType() != null) {
      approvalType = notificationMessageResolver.getApprovalType(
          ((ExecutionContextImpl) context).getStateExecutionInstance().getExecutionType());
    }

    if (currentTimeMillis >= (timeout + startTimeMillis)) {
      if (approvalType != null && approvalType.equalsIgnoreCase("PIPELINE")) {
        errorMsg = "Pipeline was not approved within " + Misc.getDurationString(getTimeoutMillis());
      } else if (approvalType != null && approvalType.equalsIgnoreCase("ORCHESTRATION")) {
        errorMsg = "Workflow was not approved within " + Misc.getDurationString(getTimeoutMillis());
      } else {
        errorMsg = "Approval not approved within " + Misc.getDurationString(getTimeoutMillis());
      }
      Map<String, String> placeholderValues = getPlaceholderValues(context, errorMsg);
      sendApprovalNotification(app.getAccountId(), APPROVAL_EXPIRED_NOTIFICATION, placeholderValues);
      sendEmailToUserGroupMembers(userGroups, app.getAccountId(), APPROVAL_EXPIRED_NOTIFICATION, placeholderValues);
    } else {
      if (approvalType != null && approvalType.equalsIgnoreCase("PIPELINE")) {
        errorMsg = "Pipeline was aborted";
      } else if (approvalType != null && approvalType.equalsIgnoreCase("ORCHESTRATION")) {
        errorMsg = "Workflow was aborted";
      } else {
        errorMsg = "Workflow or Pipeline was aborted";
      }

      User user = UserThreadLocal.get();
      String userName = (user != null && user.getName() != null) ? user.getName() : "System";
      Map<String, String> placeholderValues = getPlaceholderValues(context, userName, ABORTED);
      sendApprovalNotification(app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues);
      sendEmailToUserGroupMembers(
          userGroups, app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues);
    }

    context.getStateExecutionData().setErrorMsg(errorMsg);
  }

  @Override
  public Integer getTimeoutMillis() {
    if (super.getTimeoutMillis() == null) {
      return DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS;
    }
    return super.getTimeoutMillis();
  }

  private void sendApprovalNotification(
      String accountId, NotificationMessageType notificationMessageType, Map<String, String> placeHolderValues) {
    List<NotificationGroup> notificationGroups = notificationSetupService.listDefaultNotificationGroup(accountId);
    NotificationRule notificationRule = aNotificationRule().withNotificationGroups(notificationGroups).build();

    notificationService.sendNotificationAsync(anInformationNotification()
                                                  .withAppId(GLOBAL_APP_ID)
                                                  .withAccountId(accountId)
                                                  .withNotificationTemplateId(notificationMessageType.name())
                                                  .withNotificationTemplateVariables(placeHolderValues)
                                                  .build(),
        singletonList(notificationRule));
  }

  private static String getStatusMessage(ExecutionStatus status) {
    switch (status) {
      case SUCCESS:
        return "approved";
      case ABORTED:
        return "aborted";
      case REJECTED:
        return "rejected";
      case EXPIRED:
        return "expired";
      case PAUSED:
        return "paused";
      default:
        unhandled(status);
        return "failed";
    }
  }

  protected Map<String, String> getPlaceholderValues(
      ExecutionContext context, String userName, ExecutionStatus status) {
    WorkflowExecution workflowExecution = workflowExecutionService.getWorkflowExecution(
        ((ExecutionContextImpl) context).getApp().getUuid(), context.getWorkflowExecutionId());

    String statusMsg = getStatusMessage(status);
    long startTs = (status == PAUSED) ? workflowExecution.getCreatedAt() : context.getStateExecutionData().getStartTs();
    if (status == PAUSED) {
      userName = workflowExecution.getTriggeredBy().getName();
    }

    return notificationMessageResolver.getPlaceholderValues(
        context, userName, startTs, System.currentTimeMillis(), "", statusMsg, "", status, ApprovalNeeded);
  }

  private Map<String, String> getPlaceholderValues(ExecutionContext context, String timeout) {
    return notificationMessageResolver.getPlaceholderValues(
        context, "", 0, 0, timeout, "", "", EXPIRED, ApprovalNeeded);
  }

  private void sendEmailToUserGroupMembers(List<String> userGroups, String accountId,
      NotificationMessageType notificationMessageType, Map<String, String> placeHolderValues) {
    List<String> userEmailAddress = getUserGroupMemberEmailAddresses(accountId, userGroups);
    if (isEmpty(userEmailAddress)) {
      return;
    }

    List<String> excludeEmailAddress = getNotificationGroupMemberEmailAddresses(accountId);
    userEmailAddress.removeAll(excludeEmailAddress);
    if (isEmpty(userEmailAddress)) {
      return;
    }

    EmailData emailData =
        notificationDispatcherService.obtainEmailData(notificationMessageType.toString(), placeHolderValues);
    if (isEmpty(emailData.getBody()) || isEmpty(emailData.getSubject())) {
      return;
    }

    emailData.setSystem(true);
    emailData.setCc(Collections.emptyList());
    emailData.setTo(userEmailAddress);
    emailNotificationService.sendAsync(emailData);
  }

  private List<String> getNotificationGroupMemberEmailAddresses(String accountId) {
    List<NotificationGroup> notificationGroups = notificationSetupService.listDefaultNotificationGroup(accountId);
    return notificationSetupService.getUserEmailAddressFromNotificationGroups(accountId, notificationGroups);
  }

  private List<String> getUserGroupMemberEmailAddresses(String accountId, List<String> userGroups) {
    if (isEmpty(userGroups)) {
      return asList();
    }

    List<String> userGroupMembers = userGroupService.fetchUserGroupsMemberIds(accountId, userGroups);
    if (isEmpty(userGroupMembers)) {
      return asList();
    }

    return userService.fetchUserEmailAddressesFromUserIds(userGroupMembers);
  }
}
