package software.wings.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.delegate.task.shell.ShellScriptApprovalTaskParameters;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.ShellScriptApprovalExecutionData;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.TaskType;
import software.wings.beans.approval.ApprovalPollingJobEntity;
import software.wings.service.intfc.DelegateService;

import java.util.concurrent.TimeUnit;

/**
 * Handles shell script approvals.
 */
@Singleton
@Slf4j
public class ShellScriptApprovalService {
  private static final long TIME_OUT_IN_MINUTES = 5;
  private static final String SCRIPT_APPROVAL_DIRECTORY = "/tmp";
  private static final String SCRIPT_APPROVAL_COMMAND = "Execute Approval Script";
  private static final String SCRIPT_APPROVAL_ENV_VARIABLE = "HARNESS_APPROVAL_STATUS";

  private final DelegateService delegateService;
  private final WaitNotifyEngine waitNotifyEngine;

  @Inject
  public ShellScriptApprovalService(DelegateService delegateService, WaitNotifyEngine waitNotifyEngine) {
    this.delegateService = delegateService;
    this.waitNotifyEngine = waitNotifyEngine;
  }

  public void handleShellScriptPolling(ApprovalPollingJobEntity scriptApprovalPollingEntity) {
    String accountId = scriptApprovalPollingEntity.getAccountId();
    String appId = scriptApprovalPollingEntity.getAppId();
    String approvalId = scriptApprovalPollingEntity.getApprovalId();
    String activityId = scriptApprovalPollingEntity.getActivityId();
    String scriptString = scriptApprovalPollingEntity.getScriptString();

    tryShellScriptApproval(accountId, appId, approvalId, activityId, scriptString);
  }

  boolean tryShellScriptApproval(
      String accountId, String appId, String approvalId, String activityId, String scriptString) {
    ShellScriptApprovalTaskParameters shellScriptApprovalTaskParameters =
        ShellScriptApprovalTaskParameters.builder()
            .accountId(accountId)
            .appId(appId)
            .activityId(activityId)
            .commandName(SCRIPT_APPROVAL_COMMAND)
            .outputVars(SCRIPT_APPROVAL_ENV_VARIABLE)
            .workingDirectory(SCRIPT_APPROVAL_DIRECTORY)
            .scriptType(ScriptType.BASH)
            .script(scriptString)
            .build();

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(accountId)
                                    .appId(appId)
                                    .waitId(activityId)
                                    .data(TaskData.builder()
                                              .async(false)
                                              .taskType(TaskType.SHELL_SCRIPT_APPROVAL.name())
                                              .parameters(new Object[] {shellScriptApprovalTaskParameters})
                                              .timeout(TimeUnit.MINUTES.toMillis(TIME_OUT_IN_MINUTES))
                                              .build())
                                    .build();

    ResponseData responseData = null;
    try {
      responseData = delegateService.executeTask(delegateTask);
    } catch (Exception e) {
      logger.error("Failed to fetch Approval Status from Script", e);
      return true;
    }

    if (responseData instanceof ShellScriptApprovalExecutionData) {
      ShellScriptApprovalExecutionData executionData = (ShellScriptApprovalExecutionData) responseData;
      if (executionData.getApprovalAction() == Action.APPROVE || executionData.getApprovalAction() == Action.REJECT) {
        try {
          approveWorkflow(approvalId, appId, executionData.getExecutionStatus());
        } catch (Exception e) {
          logger.error("Failed to Approve/Reject Status", e);
        }
        return true;
      }
    } else if (responseData instanceof ErrorNotifyResponseData) {
      logger.error("Shell Script Approval task failed unexpectedly {}", responseData);
      return true;
    }
    return false;
  }

  private void approveWorkflow(String approvalId, String appId, ExecutionStatus approvalStatus) {
    ApprovalStateExecutionData executionData = ApprovalStateExecutionData.builder()
                                                   .appId(appId)
                                                   .approvalId(approvalId)
                                                   .approvedOn(System.currentTimeMillis())
                                                   .build();

    if (approvalStatus == ExecutionStatus.SUCCESS || approvalStatus == ExecutionStatus.REJECTED) {
      executionData.setApprovedOn(System.currentTimeMillis());
    }

    executionData.setStatus(approvalStatus);
    waitNotifyEngine.doneWith(approvalId, executionData);
  }
}
