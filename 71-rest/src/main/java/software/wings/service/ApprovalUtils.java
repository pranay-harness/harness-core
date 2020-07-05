package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.eraro.ErrorCode.SERVICENOW_ERROR;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.exception.ServiceNowException;
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
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.states.ApprovalState;

@OwnedBy(CDC)
@UtilityClass
@Slf4j
public class ApprovalUtils {
  @Inject private WingsPersistence wingsPersistence;
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

  private static void failWorkflow(StateExecutionService stateExecutionService, WaitNotifyEngine waitNotifyEngine,
      String workflowExecutionId, String stateExecutionInstanceId, String errorMesage,
      ApprovalStateExecutionData approvalData) {
    String approvalId = approvalData.getApprovalId();
    String appId = approvalData.getAppId();
    ApprovalStateExecutionData executionData;
    if (stateExecutionInstanceId == null) {
      ApprovalDetails approvalDetails = new ApprovalDetails();
      approvalDetails.setApprovalId(approvalId);
      executionData = approvalData.getWorkflowExecutionService().fetchApprovalStateExecutionDataFromWorkflowExecution(
          appId, workflowExecutionId, null, approvalDetails);
    } else {
      StateExecutionInstance stateExecutionInstance =
          stateExecutionService.getStateExecutionData(appId, stateExecutionInstanceId);
      if (stateExecutionInstance == null) {
        throw new ServiceNowException(
            "State execution instance " + stateExecutionInstanceId + " not found for service now approval",
            SERVICENOW_ERROR, USER);
      }
      executionData = (ApprovalStateExecutionData) stateExecutionInstance.getStateExecutionMap().get(
          stateExecutionInstance.getDisplayName());
    }

    executionData.setStatus(ExecutionStatus.FAILED);
    executionData.setApprovedOn(System.currentTimeMillis());
    executionData.setCurrentStatus(approvalData.getCurrentStatus());
    executionData.setErrorMsg("Servicenow approval failed: " + errorMesage + " ticket: ");

    logger.info("Sending notify for approvalId: {}, workflowExecutionId: {} ", approvalId, workflowExecutionId);
    waitNotifyEngine.doneWith(approvalId, executionData);
  }

  private static void continuePauseWorkflow(StateExecutionService stateExecutionService, String workflowExecutionId,
      String stateExecutionInstanceId, String message, ApprovalStateExecutionData approvalData) {
    if (stateExecutionInstanceId == null) {
      return;
    }
    String approvalId = approvalData.getApprovalId();
    String appId = approvalData.getAppId();
    String currentStatus = approvalData.getCurrentStatus();
    StateExecutionInstance stateExecutionInstance =
        stateExecutionService.getStateExecutionData(appId, stateExecutionInstanceId);
    if (stateExecutionInstance == null) {
      throw new ServiceNowException(
          "State execution instance " + stateExecutionInstanceId + " not found for service now approval",
          SERVICENOW_ERROR, USER);
    }

    ApprovalStateExecutionData executionData =
        (ApprovalStateExecutionData) stateExecutionInstance.getStateExecutionMap().get(
            stateExecutionInstance.getDisplayName());
    if (approvalData.isWaitingForChangeWindow()) {
      executionData.setWaitingForChangeWindow(true);
      stateExecutionInstance.setExpiryTs(Long.MAX_VALUE);
      executionData.setTimeoutMillis(Integer.MAX_VALUE);
      executionData.setErrorMsg("Approved but waiting for Change window ( " + message + " )");
      stateExecutionService.updateStateExecutionInstance(stateExecutionInstance);
    }
    if (executionData.getCurrentStatus() != null && executionData.getCurrentStatus().equalsIgnoreCase(currentStatus)) {
      return;
    }

    executionData.setApprovedOn(System.currentTimeMillis());
    executionData.setCurrentStatus(currentStatus);

    logger.info("Saving executionData for approvalId: {}, workflowExecutionId: {} ", approvalId, workflowExecutionId);
    stateExecutionService.updateStateExecutionData(appId, stateExecutionInstanceId, executionData);
  }

  public static void checkApproval(StateExecutionService stateExecutionService, WaitNotifyEngine waitNotifyEngine,
      String workflowExecutionId, String stateExecutionInstanceId, String errorMsg, ExecutionStatus issueStatus,
      ApprovalStateExecutionData approvalStateExecutionData) {
    String approvalId = approvalStateExecutionData.getApprovalId();
    String appId = approvalStateExecutionData.getAppId();
    try {
      String currentStatus = approvalStateExecutionData.getCurrentStatus();
      if (issueStatus == ExecutionStatus.SUCCESS || issueStatus == ExecutionStatus.REJECTED) {
        ApprovalDetails.Action action =
            issueStatus == ExecutionStatus.SUCCESS ? ApprovalDetails.Action.APPROVE : ApprovalDetails.Action.REJECT;

        approveWorkflow(approvalStateExecutionData.getWorkflowExecutionService(), stateExecutionService,
            waitNotifyEngine, action, approvalId, appId, workflowExecutionId, issueStatus, currentStatus,
            stateExecutionInstanceId);
      } else if (issueStatus == ExecutionStatus.PAUSED) {
        logger.info("Still waiting for approval or rejected for issueId {}. Issue Status {} and Current Status {}",
            approvalStateExecutionData.getIssueKey(), issueStatus, currentStatus);
        continuePauseWorkflow(
            stateExecutionService, workflowExecutionId, stateExecutionInstanceId, errorMsg, approvalStateExecutionData);
      } else if (issueStatus == ExecutionStatus.FAILED) {
        logger.info("Jira delegate task failed with error: " + errorMsg);
        failWorkflow(stateExecutionService, waitNotifyEngine, workflowExecutionId, stateExecutionInstanceId, errorMsg,
            approvalStateExecutionData);
      }
    } catch (WingsException exception) {
      exception.addContext(Application.class, appId);
      exception.addContext(WorkflowExecution.class, workflowExecutionId);
      exception.addContext(ApprovalState.class, approvalId);
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.warn("Error while getting execution data, approvalId: {}, workflowExecutionId: {} , issueId: {}",
          approvalId, workflowExecutionId, approvalStateExecutionData.getIssueKey(), exception);
    }
  }
}
