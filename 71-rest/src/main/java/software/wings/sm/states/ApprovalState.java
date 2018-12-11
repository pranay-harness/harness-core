package software.wings.sm.states;

import static io.harness.beans.ExecutionStatus.ABORTED;
import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static software.wings.beans.alert.AlertType.ApprovalNeeded;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_NEEDED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.APPROVAL_STATE_CHANGE_NOTIFICATION;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import io.harness.delegate.task.protocol.ResponseData;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.Application;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
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
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApprovalState extends AbstractApprovalState {
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
    populateApprovalAlert(approvalNeededAlert, context);
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
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ApprovalStateExecutionData approvalNotifyResponse =
        (ApprovalStateExecutionData) response.values().iterator().next();

    ApprovalStateExecutionData executionData = (ApprovalStateExecutionData) context.getStateExecutionData();
    executionData.setApprovedBy(approvalNotifyResponse.getApprovedBy());
    executionData.setComments(approvalNotifyResponse.getComments());
    executionData.setApprovedOn(System.currentTimeMillis());

    // Close the alert
    Application app = ((ExecutionContextImpl) context).getApp();
    ApprovalNeededAlert approvalNeededAlert = ApprovalNeededAlert.builder()
                                                  .executionId(context.getWorkflowExecutionId())
                                                  .approvalId(approvalNotifyResponse.getApprovalId())
                                                  .build();
    populateApprovalAlert(approvalNeededAlert, context);
    alertService.closeAlert(app.getAccountId(), app.getUuid(), ApprovalNeeded, approvalNeededAlert);

    Map<String, String> placeholderValues = getPlaceholderValues(
        context, approvalNotifyResponse.getApprovedBy().getName(), approvalNotifyResponse.getStatus());
    sendApprovalNotification(app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues);
    sendEmailToUserGroupMembers(userGroups, app.getAccountId(), APPROVAL_STATE_CHANGE_NOTIFICATION, placeholderValues);

    return anExecutionResponse()
        .withStateExecutionData(executionData)
        .withExecutionStatus(approvalNotifyResponse.getStatus())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    NotificationMessageType notificationMessageType = handleAbortEventAbstract(context);
    Application app = ((ExecutionContextImpl) context).getApp();
    User user = UserThreadLocal.get();
    String userName = (user != null && user.getName() != null) ? user.getName() : "System";
    Map<String, String> placeholderValues = getPlaceholderValues(context, userName, ABORTED);
    sendEmailToUserGroupMembers(userGroups, app.getAccountId(), notificationMessageType, placeholderValues);
  }
}
