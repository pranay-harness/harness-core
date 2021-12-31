package io.harness.pms.approval.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER_SRE;

import static java.util.Objects.isNull;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.task.jira.JiraTaskNGResponse;
import io.harness.exception.ApprovalStepNGException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.HarnessJiraException;
import io.harness.jira.JiraIssueNG;
import io.harness.logging.AutoLogContext;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.logstreaming.NGLogCallback;
import io.harness.pms.approval.AbstractApprovalCallback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.serializer.KryoSerializer;
import io.harness.servicenow.TicketNG;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.beans.CriteriaSpecDTO;
import io.harness.steps.approval.step.evaluation.CriteriaEvaluator;
import io.harness.steps.approval.step.jira.entities.JiraApprovalInstance;
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
public class JiraApprovalCallback extends AbstractApprovalCallback implements PushThroughNotifyCallback {
  private final String approvalInstanceId;

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
      updateApprovalInstanceAndLog(logCallback, "Approval instance has expired", LogColor.Red,
          CommandExecutionStatus.FAILURE, ApprovalStatus.EXPIRED, instance.getId());
    }

    JiraTaskNGResponse jiraTaskNGResponse;
    try {
      ResponseData responseData = response.values().iterator().next();
      responseData = (ResponseData) kryoSerializer.asInflatedObject(((BinaryResponseData) responseData).getData());
      if (responseData instanceof ErrorNotifyResponseData) {
        handleErrorNotifyResponse(logCallback, (ErrorNotifyResponseData) responseData, "Failed to fetch jira issue:");
        return;
      }

      jiraTaskNGResponse = (JiraTaskNGResponse) responseData;
      if (isNull(jiraTaskNGResponse.getIssue())) {
        log.info("Invalid issue key");
        String errorMessage = String.format("Invalid issue key: %s", instance.getIssueKey());
        logCallback.saveExecutionLog(
            LogHelper.color(errorMessage, LogColor.Red), LogLevel.INFO, CommandExecutionStatus.FAILURE);
        approvalInstanceService.finalizeStatus(instance.getId(), ApprovalStatus.FAILED, errorMessage);
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
      checkApprovalAndRejectionCriteria(jiraTaskNGResponse.getIssue(), instance, logCallback,
          instance.getApprovalCriteria(), instance.getRejectionCriteria());
    } catch (Exception ex) {
      if (ex instanceof ApprovalStepNGException && ((ApprovalStepNGException) ex).isFatal()) {
        handleFatalException(instance, logCallback, (ApprovalStepNGException) ex);
        return;
      }

      logCallback.saveExecutionLog(
          LogHelper.color(String.format("Error evaluating approval/rejection criteria: %s. Retrying in sometime...",
                              ExceptionUtils.getMessage(ex)),
              LogColor.Red));
      throw new HarnessJiraException("Error while evaluating approval/rejection criteria", ex, USER_SRE);
    }
  }

  @Override
  protected boolean evaluateCriteria(TicketNG ticket, CriteriaSpecDTO criteriaSpec) {
    return CriteriaEvaluator.evaluateCriteria((JiraIssueNG) ticket, criteriaSpec);
  }
}
