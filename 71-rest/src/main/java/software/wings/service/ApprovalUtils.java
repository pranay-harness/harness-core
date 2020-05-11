package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.waiter.WaitNotifyEngine;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.beans.Application;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.states.ApprovalState;

@OwnedBy(CDC)
@UtilityClass
@Slf4j
public class ApprovalUtils {
  private static void approveWorkflow(WorkflowExecutionService workflowExecutionService,
      StateExecutionService stateExecutionService, WaitNotifyEngine waitNotifyEngine, Action action, String approvalId,
      String appId, String workflowExecutionId, ExecutionStatus approvalStatus, String currentStatus,
      String stateExecutionInstanceId) {
    ApprovalStateExecutionData executionData;
    if (stateExecutionInstanceId == null) {
      ApprovalDetails approvalDetails = new ApprovalDetails();
      approvalDetails.setAction(action);
      approvalDetails.setApprovalId(approvalId);
      executionData = workflowExecutionService.fetchApprovalStateExecutionDataFromWorkflowExecution(
          appId, workflowExecutionId, null, approvalDetails);
    } else {
      StateExecutionInstance stateExecutionInstance =
          stateExecutionService.getStateExecutionData(appId, stateExecutionInstanceId);
      if (stateExecutionInstance == null) {
        throw new WingsException(INVALID_ARGUMENT, USER)
            .addParam("args", "No stateExecutionInstance found for id " + stateExecutionInstanceId);
      }
      executionData = (ApprovalStateExecutionData) stateExecutionInstance.getStateExecutionMap().get(
          stateExecutionInstance.getDisplayName());
    }
    String message;
    if (action == Action.APPROVE) {
      message = "Approval provided on ticket: ";
    } else {
      message = "Rejection provided on ticket: ";
    }

    executionData.setStatus(approvalStatus);
    executionData.setApprovedOn(System.currentTimeMillis());
    executionData.setCurrentStatus(currentStatus);
    executionData.setErrorMsg(message);

    logger.info("Sending notify for approvalId: {}, workflowExecutionId: {} ", approvalId, workflowExecutionId);
    waitNotifyEngine.doneWith(approvalId, executionData);
  }

  private static void continuePauseWorkflow(StateExecutionService stateExecutionService, String approvalId,
      String appId, String workflowExecutionId, String currentStatus, String stateExecutionInstanceId) {
    if (stateExecutionInstanceId == null) {
      return;
    }
    StateExecutionInstance stateExecutionInstance =
        stateExecutionService.getStateExecutionData(appId, stateExecutionInstanceId);
    if (stateExecutionInstance == null) {
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args", "No stateExecutionInstace found for id " + stateExecutionInstanceId);
    }

    ApprovalStateExecutionData executionData =
        (ApprovalStateExecutionData) stateExecutionInstance.getStateExecutionMap().get(
            stateExecutionInstance.getDisplayName());

    if (executionData.getCurrentStatus() != null && executionData.getCurrentStatus().equalsIgnoreCase(currentStatus)) {
      return;
    }
    executionData.setApprovedOn(System.currentTimeMillis());
    executionData.setCurrentStatus(currentStatus);

    logger.info("Saving executionData for approvalId: {}, workflowExecutionId: {} ", approvalId, workflowExecutionId);
    stateExecutionService.updateStateExecutionData(appId, stateExecutionInstanceId, executionData);
  }

  public static void checkApproval(WorkflowExecutionService workflowExecutionService,
      StateExecutionService stateExecutionService, WaitNotifyEngine waitNotifyEngine, String appId, String approvalId,
      String issueId, String workflowExecutionId, String stateExecutionInstanceId, String currentStatus,
      String errorMsg, ExecutionStatus issueStatus) {
    try {
      if (issueStatus == ExecutionStatus.SUCCESS || issueStatus == ExecutionStatus.REJECTED) {
        ApprovalDetails.Action action =
            issueStatus == ExecutionStatus.SUCCESS ? ApprovalDetails.Action.APPROVE : ApprovalDetails.Action.REJECT;

        approveWorkflow(workflowExecutionService, stateExecutionService, waitNotifyEngine, action, approvalId, appId,
            workflowExecutionId, issueStatus, currentStatus, stateExecutionInstanceId);
      } else if (issueStatus == ExecutionStatus.PAUSED) {
        logger.info("Still waiting for approval or rejected for issueId {}. Issue Status {} and Current Status {}",
            issueId, issueStatus, currentStatus);
        continuePauseWorkflow(
            stateExecutionService, approvalId, appId, workflowExecutionId, currentStatus, stateExecutionInstanceId);
      } else if (issueStatus == ExecutionStatus.FAILED) {
        logger.info("Jira delegate task failed with error: " + errorMsg);
      }
    } catch (WingsException exception) {
      exception.addContext(Application.class, appId);
      exception.addContext(WorkflowExecution.class, workflowExecutionId);
      exception.addContext(ApprovalState.class, approvalId);
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.warn("Error while getting execution data, approvalId: {}, workflowExecutionId: {} , issueId: {}",
          approvalId, workflowExecutionId, issueId, exception);
    }
  }
}
