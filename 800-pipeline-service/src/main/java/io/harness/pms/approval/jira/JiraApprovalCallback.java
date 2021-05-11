package io.harness.pms.approval.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER_SRE;

import static java.util.Objects.isNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.jira.JiraTaskNGResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.HarnessJiraException;
import io.harness.exception.InvalidRequestException;
import io.harness.jira.JiraIssueNG;
import io.harness.logging.AutoLogContext;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.logstreaming.NGLogCallback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.approval.step.ApprovalInstanceService;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.jira.beans.CriteriaSpecDTO;
import io.harness.steps.approval.step.jira.entities.JiraApprovalInstance;
import io.harness.steps.approval.step.jira.evaluation.CriteriaEvaluator;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.ResponseData;
import io.harness.waiter.PushThroughNotifyCallback;

import software.wings.beans.LogColor;
import software.wings.beans.LogHelper;

import com.google.inject.Inject;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Data
@Slf4j
public class JiraApprovalCallback implements PushThroughNotifyCallback {
  private final String approvalInstanceId;

  @Inject private ApprovalInstanceService approvalInstanceService;
  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Inject private KryoSerializer kryoSerializer;

  @Builder
  public JiraApprovalCallback(String approvalInstanceId) {
    this.approvalInstanceId = approvalInstanceId;
  }

  @Override
  public void push(Map<String, ResponseData> response) {
    JiraApprovalInstance instance = (JiraApprovalInstance) approvalInstanceService.get(approvalInstanceId);
    try (AutoLogContext ignore = instance.autoLogContext()) {
      pushInternal(response);
    }
  }

  private void pushInternal(Map<String, ResponseData> response) {
    JiraApprovalInstance instance = (JiraApprovalInstance) approvalInstanceService.get(approvalInstanceId);
    Ambiance ambiance = instance.getAmbiance();
    NGLogCallback logCallback = new NGLogCallback(logStreamingStepClientFactory, ambiance, null, false);

    if (instance.hasExpired()) {
      log.info("Approval instance has expired");
      logCallback.saveExecutionLog(LogHelper.color("Approval step timed out before completion", LogColor.Red),
          LogLevel.INFO, CommandExecutionStatus.FAILURE);
      approvalInstanceService.finalizeStatus(instance.getId(), ApprovalStatus.EXPIRED);
    }

    JiraTaskNGResponse jiraTaskNGResponse;
    try {
      ResponseData responseData = response.values().iterator().next();
      responseData = (ResponseData) kryoSerializer.asInflatedObject(((BinaryResponseData) responseData).getData());
      if (responseData instanceof ErrorNotifyResponseData) {
        ErrorNotifyResponseData errorResponse = (ErrorNotifyResponseData) responseData;
        log.error(String.format("Failed to fetch jira issue: %s", errorResponse.getErrorMessage()),
            errorResponse.getException());
        return;
      }

      jiraTaskNGResponse = (JiraTaskNGResponse) responseData;
      if (isNull(jiraTaskNGResponse.getIssue())) {
        log.info("Invalid issue key");
        logCallback.saveExecutionLog(
            LogHelper.color("Invalid issue key", LogColor.Red), LogLevel.INFO, CommandExecutionStatus.FAILURE);
        approvalInstanceService.finalizeStatus(instance.getId(), ApprovalStatus.FAILED);
        return;
      }

      logCallback.saveExecutionLog(String.format("Issue url: %s", jiraTaskNGResponse.getIssue().getUrl()));
    } catch (Exception ex) {
      logCallback.saveExecutionLog(
          LogHelper.color(String.format("Error fetching jira issue response: %s. Retrying in sometime...",
                              ExceptionUtils.getMessage(ex)),
              LogColor.Red));
      log.error("Failed to fetch jira issue", ex);
      return;
    }

    try {
      checkApprovalAndRejectionCriteria(jiraTaskNGResponse.getIssue(), instance, logCallback);
    } catch (Exception ex) {
      log.error(
          "Error occurred while approving/rejecting criteria for JiraApproval. Continuing to poll in sometime", ex);
    }
  }

  private void checkApprovalAndRejectionCriteria(
      JiraIssueNG issue, JiraApprovalInstance instance, NGLogCallback logCallback) {
    try {
      if (isNull(instance.getApprovalCriteria()) || isNull(instance.getApprovalCriteria().getCriteriaSpecDTO())) {
        throw new InvalidRequestException("Approval criteria can't be empty");
      }

      logCallback.saveExecutionLog("Evaluating approval criteria...");
      CriteriaSpecDTO approvalCriteriaSpec = instance.getApprovalCriteria().getCriteriaSpecDTO();
      boolean approvalEvaluationResult = CriteriaEvaluator.evaluateCriteria(issue, approvalCriteriaSpec);
      if (approvalEvaluationResult) {
        log.info("Approval criteria has been met");
        logCallback.saveExecutionLog(LogHelper.color("Approval criteria has been met", LogColor.Cyan), LogLevel.INFO,
            CommandExecutionStatus.SUCCESS);
        approvalInstanceService.finalizeStatus(instance.getId(), ApprovalStatus.APPROVED);
        return;
      }
      logCallback.saveExecutionLog("Approval criteria has not been met");

      if (isNull(instance.getRejectionCriteria()) || isNull(instance.getRejectionCriteria().getCriteriaSpecDTO())) {
        throw new InvalidRequestException("Rejection criteria can't be empty");
      }

      logCallback.saveExecutionLog("Evaluating rejection criteria...");
      CriteriaSpecDTO rejectionCriteriaSpec = instance.getRejectionCriteria().getCriteriaSpecDTO();
      boolean rejectionEvaluationResult = CriteriaEvaluator.evaluateCriteria(issue, rejectionCriteriaSpec);
      if (rejectionEvaluationResult) {
        log.info("Rejection criteria has been met");
        logCallback.saveExecutionLog(LogHelper.color("Rejection criteria has been met", LogColor.Red), LogLevel.INFO,
            CommandExecutionStatus.FAILURE);
        approvalInstanceService.finalizeStatus(instance.getId(), ApprovalStatus.REJECTED);
        return;
      }
      logCallback.saveExecutionLog("Rejection criteria has also not been met");
    } catch (Exception e) {
      logCallback.saveExecutionLog(
          LogHelper.color(String.format("Error evaluating approval/rejection criteria: %s. Retrying in sometime...",
                              ExceptionUtils.getMessage(e)),
              LogColor.Red));
      throw new HarnessJiraException("Error while evaluating approval/rejection criteria", e, USER_SRE);
    }
  }
}
