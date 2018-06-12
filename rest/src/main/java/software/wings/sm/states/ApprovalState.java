package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static software.wings.api.ApprovalStateExecutionData.Builder.anApprovalStateExecutionData;
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

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.Application;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.Map;

/**
 * A Pause state to pause state machine execution.
 *
 * @author Rishi
 */
public class ApprovalState extends State {
  @Attributes(required = true, title = "Group Name") private String groupName;

  @Inject private AlertService alertService;
  @Inject private NotificationService notificationService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private NotificationMessageResolver notificationMessageResolver;

  private boolean disable;

  public boolean isDisable() {
    return disable;
  }

  public void setDisable(boolean disable) {
    this.disable = disable;
  }

  public ApprovalState(String name) {
    super(name, StateType.APPROVAL.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String approvalId = generateUuid();
    ApprovalStateExecutionData executionData = anApprovalStateExecutionData().withApprovalId(approvalId).build();

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

    String errorMsg;
    if (currentTimeMillis >= (timeout + startTimeMillis)) {
      errorMsg = "Pipeline was not approved within " + Misc.getDurationString(getTimeoutMillis());
      Map<String, String> placeholderValues = getPlaceholderValues(context, errorMsg);
      sendApprovalNotification(app.getAccountId(), APPROVAL_EXPIRED_NOTIFICATION, placeholderValues);
    } else {
      errorMsg = "Pipeline was aborted";
      User user = UserThreadLocal.get();
      String userName = (user != null && user.getName() != null) ? user.getName() : "System";
      Map<String, String> placeholderValues = getPlaceholderValues(context, userName, ABORTED);
      sendApprovalNotification(app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues);
    }

    context.getStateExecutionData().setErrorMsg(errorMsg);
  }

  @SchemaIgnore
  @Override
  public Integer getTimeoutMillis() {
    if (super.getTimeoutMillis() == null) {
      return DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS;
    }
    return super.getTimeoutMillis();
  }

  /**
   * Gets group name.
   *
   * @return the group name
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * Sets group name.
   *
   * @param groupName the group name
   */
  public void setGroupName(String groupName) {
    this.groupName = groupName;
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
}
