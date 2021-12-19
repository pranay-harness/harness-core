package io.harness.pms.approval.servicenow;

import static io.harness.exception.WingsException.USER_SRE;

import static java.util.Objects.isNull;

import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.servicenow.ServiceNowTaskNGResponse;
import io.harness.eraro.ErrorCode;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.ApprovalStepNGException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ServiceNowException;
import io.harness.logging.AutoLogContext;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.logstreaming.NGLogCallback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.serializer.KryoSerializer;
import io.harness.servicenow.ServiceNowTicketNG;
import io.harness.steps.approval.step.ApprovalInstanceService;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.jira.beans.CriteriaSpecDTO;
import io.harness.steps.approval.step.servicenow.entities.ServiceNowApprovalInstance;
import io.harness.steps.approval.step.servicenow.evaluation.ServiceNowCriteriaEvaluator;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.ResponseData;
import io.harness.waiter.PushThroughNotifyCallback;

import software.wings.beans.LogColor;
import software.wings.beans.LogHelper;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceNowApprovalCallback implements PushThroughNotifyCallback {
  private final String approvalInstanceId;
  @Inject private ApprovalInstanceService approvalInstanceService;
  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private NGErrorHelper ngErrorHelper;

  @Builder
  public ServiceNowApprovalCallback(String approvalInstanceId) {
    this.approvalInstanceId = approvalInstanceId;
  }

  @Override
  public void push(Map<String, ResponseData> response) {
    ServiceNowApprovalInstance instance = (ServiceNowApprovalInstance) approvalInstanceService.get(approvalInstanceId);
    try (AutoLogContext ignore = instance.autoLogContext()) {
      pushInternal(response, instance);
    }
  }

  private void pushInternal(Map<String, ResponseData> response, ServiceNowApprovalInstance instance) {
    Ambiance ambiance = instance.getAmbiance();
    NGLogCallback logCallback = new NGLogCallback(logStreamingStepClientFactory, ambiance, null, false);

    if (instance.hasExpired()) {
      log.info("Approval instance has expired");
      logCallback.saveExecutionLog(LogHelper.color("Approval step timed out before completion", LogColor.Red),
          LogLevel.INFO, CommandExecutionStatus.FAILURE);
      approvalInstanceService.finalizeStatus(instance.getId(), ApprovalStatus.EXPIRED);
    }

    ServiceNowTaskNGResponse serviceNowTaskNGResponse;
    try {
      ResponseData responseData = response.values().iterator().next();
      responseData = (ResponseData) kryoSerializer.asInflatedObject(((BinaryResponseData) responseData).getData());
      if (responseData instanceof ErrorNotifyResponseData) {
        ErrorNotifyResponseData errorResponse = (ErrorNotifyResponseData) responseData;
        String errorMessage = String.format("Failed to fetch ServiceNow ticket: %s", errorResponse.getErrorMessage());
        logCallback.saveExecutionLog(
            LogHelper.color(errorMessage, LogColor.Red), LogLevel.INFO, CommandExecutionStatus.FAILURE);
        log.error(errorMessage, errorResponse.getException());
        return;
      }
      serviceNowTaskNGResponse = (ServiceNowTaskNGResponse) responseData;
      if (isNull(serviceNowTaskNGResponse.getTicket())) {
        log.info("Invalid ticket number");
        String errorMessage = String.format("Invalid ticket number: %s", instance.getTicketNumber());
        logCallback.saveExecutionLog(
            LogHelper.color(errorMessage, LogColor.Red), LogLevel.INFO, CommandExecutionStatus.FAILURE);
        approvalInstanceService.finalizeStatus(instance.getId(), ApprovalStatus.FAILED, errorMessage);
        return;
      }
      logCallback.saveExecutionLog(String.format("Ticket url: %s", serviceNowTaskNGResponse.getTicket().getUrl()));
    } catch (Exception ex) {
      logCallback.saveExecutionLog(
          LogHelper.color(String.format("Error fetching serviceNow ticket response: %s. Retrying in sometime...",
                              ExceptionUtils.getMessage(ex)),
              LogColor.Red));
      log.error("Failed to fetch serviceNow ticket", ex);
      return;
    }

    try {
      checkApprovalAndRejectionCriteria(serviceNowTaskNGResponse.getTicket(), instance, logCallback);
    } catch (Exception ex) {
      if (ex instanceof ApprovalStepNGException && ((ApprovalStepNGException) ex).isFatal()) {
        log.error("Error while evaluating approval/rejection criteria", ex);
        String errorMessage = String.format(
            "Fatal error evaluating approval/rejection criteria: %s", ngErrorHelper.getErrorSummary(ex.getMessage()));
        logCallback.saveExecutionLog(
            LogHelper.color(errorMessage, LogColor.Red), LogLevel.INFO, CommandExecutionStatus.FAILURE);
        approvalInstanceService.finalizeStatus(instance.getId(), ApprovalStatus.FAILED, errorMessage);
        return;
      }

      logCallback.saveExecutionLog(
          LogHelper.color(String.format("Error evaluating approval/rejection criteria: %s. Retrying in sometime...",
                              ngErrorHelper.getErrorSummary(ex.getMessage())),
              LogColor.Red));
      throw new ServiceNowException(
          "Error while evaluating approval/rejection criteria", ErrorCode.SERVICENOW_ERROR, USER_SRE, ex);
    }
  }

  private void checkApprovalAndRejectionCriteria(
      ServiceNowTicketNG ticket, ServiceNowApprovalInstance instance, NGLogCallback logCallback) {
    if (isNull(instance.getApprovalCriteria()) || isNull(instance.getApprovalCriteria().getCriteriaSpecDTO())) {
      throw new InvalidRequestException("Approval criteria can't be empty");
    }

    logCallback.saveExecutionLog("Evaluating approval criteria...");
    CriteriaSpecDTO approvalCriteriaSpec = instance.getApprovalCriteria().getCriteriaSpecDTO();
    boolean approvalEvaluationResult = ServiceNowCriteriaEvaluator.evaluateCriteria(ticket, approvalCriteriaSpec);
    if (approvalEvaluationResult) {
      log.info("Approval criteria has been met");
      logCallback.saveExecutionLog(LogHelper.color("Approval criteria has been met", LogColor.Cyan), LogLevel.INFO,
          CommandExecutionStatus.SUCCESS);
      approvalInstanceService.finalizeStatus(instance.getId(), ApprovalStatus.APPROVED);
      return;
    }
    logCallback.saveExecutionLog("Approval criteria has not been met");

    if (isNull(instance.getRejectionCriteria()) || isNull(instance.getRejectionCriteria().getCriteriaSpecDTO())
        || instance.getRejectionCriteria().getCriteriaSpecDTO().isEmpty()) {
      logCallback.saveExecutionLog("Rejection criteria is not present");
      return;
    }

    logCallback.saveExecutionLog("Evaluating rejection criteria...");
    CriteriaSpecDTO rejectionCriteriaSpec = instance.getRejectionCriteria().getCriteriaSpecDTO();
    boolean rejectionEvaluationResult = ServiceNowCriteriaEvaluator.evaluateCriteria(ticket, rejectionCriteriaSpec);
    if (rejectionEvaluationResult) {
      log.info("Rejection criteria has been met");
      logCallback.saveExecutionLog(LogHelper.color("Rejection criteria has been met", LogColor.Red), LogLevel.INFO,
          CommandExecutionStatus.FAILURE);
      approvalInstanceService.finalizeStatus(instance.getId(), ApprovalStatus.REJECTED);
      return;
    }
    logCallback.saveExecutionLog("Rejection criteria has also not been met");
  }
}
