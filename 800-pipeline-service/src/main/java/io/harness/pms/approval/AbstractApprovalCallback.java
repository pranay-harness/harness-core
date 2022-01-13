/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.approval;

import static java.util.Objects.isNull;

import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.ApprovalStepNGException;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.logstreaming.NGLogCallback;
import io.harness.servicenow.TicketNG;
import io.harness.steps.approval.step.ApprovalInstanceService;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.beans.CriteriaSpecDTO;
import io.harness.steps.approval.step.beans.CriteriaSpecWrapperDTO;
import io.harness.steps.approval.step.entities.ApprovalInstance;

import software.wings.beans.LogColor;
import software.wings.beans.LogHelper;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractApprovalCallback {
  @Inject protected ApprovalInstanceService approvalInstanceService;
  @Inject protected NGErrorHelper ngErrorHelper;

  protected void updateApprovalInstanceAndLog(NGLogCallback logCallback, String logMessage, LogColor red,
      CommandExecutionStatus executionStatus, ApprovalStatus approvalStatus, String approvalInstanceId) {
    log.info(logMessage);
    logCallback.saveExecutionLog(LogHelper.color(logMessage, red), LogLevel.INFO, executionStatus);
    approvalInstanceService.finalizeStatus(approvalInstanceId, approvalStatus);
  }

  protected void handleErrorNotifyResponse(
      NGLogCallback logCallback, ErrorNotifyResponseData responseData, String errorMessagePrefix) {
    String errorMessage = String.format(errorMessagePrefix + " %s", responseData.getErrorMessage());
    logCallback.saveExecutionLog(
        LogHelper.color(errorMessage, LogColor.Red), LogLevel.INFO, CommandExecutionStatus.FAILURE);
    log.error(errorMessage, responseData.getException());
  }

  protected void checkApprovalAndRejectionCriteria(TicketNG ticket, ApprovalInstance instance,
      NGLogCallback logCallback, CriteriaSpecWrapperDTO approvalCriteria, CriteriaSpecWrapperDTO rejectionCriteria) {
    if (isNull(approvalCriteria) || isNull(approvalCriteria.getCriteriaSpecDTO())) {
      throw new InvalidRequestException("Approval criteria can't be empty");
    }

    logCallback.saveExecutionLog("Evaluating approval criteria...");
    CriteriaSpecDTO approvalCriteriaSpec = approvalCriteria.getCriteriaSpecDTO();
    boolean approvalEvaluationResult = evaluateCriteria(ticket, approvalCriteriaSpec);
    if (approvalEvaluationResult) {
      updateApprovalInstanceAndLog(logCallback, "Approval criteria has been met", LogColor.Cyan,
          CommandExecutionStatus.SUCCESS, ApprovalStatus.APPROVED, instance.getId());
      return;
    }
    logCallback.saveExecutionLog("Approval criteria has not been met");

    if (isNull(rejectionCriteria) || isNull(rejectionCriteria.getCriteriaSpecDTO())
        || rejectionCriteria.getCriteriaSpecDTO().isEmpty()) {
      logCallback.saveExecutionLog("Rejection criteria is not present");
      return;
    }

    logCallback.saveExecutionLog("Evaluating rejection criteria...");
    CriteriaSpecDTO rejectionCriteriaSpec = rejectionCriteria.getCriteriaSpecDTO();
    boolean rejectionEvaluationResult = evaluateCriteria(ticket, rejectionCriteriaSpec);
    if (rejectionEvaluationResult) {
      updateApprovalInstanceAndLog(logCallback, "Rejection criteria has been met", LogColor.Red,
          CommandExecutionStatus.FAILURE, ApprovalStatus.REJECTED, instance.getId());
      return;
    }
    logCallback.saveExecutionLog("Rejection criteria has also not been met");
  }

  protected void handleFatalException(
      ApprovalInstance instance, NGLogCallback logCallback, ApprovalStepNGException ex) {
    log.error("Error while evaluating approval/rejection criteria", ex);
    String errorMessage = String.format(
        "Fatal error evaluating approval/rejection criteria: %s", ngErrorHelper.getErrorSummary(ex.getMessage()));
    logCallback.saveExecutionLog(
        LogHelper.color(errorMessage, LogColor.Red), LogLevel.INFO, CommandExecutionStatus.FAILURE);
    approvalInstanceService.finalizeStatus(instance.getId(), ApprovalStatus.FAILED, errorMessage);
  }

  protected abstract boolean evaluateCriteria(TicketNG ticket, CriteriaSpecDTO criteriaSpec);
}
