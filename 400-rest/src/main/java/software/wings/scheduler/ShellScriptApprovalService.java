/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.scheduler;

import static software.wings.scheduler.approval.ApprovalPollingHandler.PUMP_INTERVAL;
import static software.wings.scheduler.approval.ApprovalPollingHandler.TARGET_INTERVAL;

import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.shell.ShellScriptApprovalTaskParameters;
import io.harness.shell.ScriptType;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.ShellScriptApprovalExecutionData;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.TaskType;
import software.wings.beans.approval.ApprovalPollingJobEntity;
import software.wings.service.intfc.ApprovalPolingService;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

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
  private final ApprovalPolingService approvalPolingService;

  @Inject
  public ShellScriptApprovalService(
      DelegateService delegateService, WaitNotifyEngine waitNotifyEngine, ApprovalPolingService approvalPolingService) {
    this.delegateService = delegateService;
    this.waitNotifyEngine = waitNotifyEngine;
    this.approvalPolingService = approvalPolingService;
  }

  public void handleShellScriptPolling(ApprovalPollingJobEntity scriptApprovalPollingEntity) {
    String accountId = scriptApprovalPollingEntity.getAccountId();
    String appId = scriptApprovalPollingEntity.getAppId();
    String approvalId = scriptApprovalPollingEntity.getApprovalId();
    String activityId = scriptApprovalPollingEntity.getActivityId();
    String scriptString = scriptApprovalPollingEntity.getScriptString();
    long retryInterval = scriptApprovalPollingEntity.getRetryInterval();
    long delayUntilNext = retryInterval - PUMP_INTERVAL.toMillis();

    boolean shouldRetry = !tryShellScriptApproval(accountId, appId, approvalId, activityId, scriptString);
    if (shouldRetry && retryInterval != TARGET_INTERVAL.toMillis()) {
      long nextIteration = System.currentTimeMillis() + delayUntilNext;
      approvalPolingService.updateNextIteration(scriptApprovalPollingEntity.getUuid(), nextIteration);
    }
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
                                    .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, appId)
                                    .waitId(activityId)
                                    .data(TaskData.builder()
                                              .async(false)
                                              .taskType(TaskType.SHELL_SCRIPT_APPROVAL.name())
                                              .parameters(new Object[] {shellScriptApprovalTaskParameters})
                                              .timeout(TimeUnit.MINUTES.toMillis(TIME_OUT_IN_MINUTES))
                                              .build())
                                    .build();

    DelegateResponseData responseData = null;
    try {
      responseData = delegateService.executeTask(delegateTask);
    } catch (Exception e) {
      log.error("Failed to fetch Approval Status from Script", e);
      return true;
    }

    if (responseData instanceof ShellScriptApprovalExecutionData) {
      ShellScriptApprovalExecutionData executionData = (ShellScriptApprovalExecutionData) responseData;
      if (executionData.getApprovalAction() == Action.APPROVE || executionData.getApprovalAction() == Action.REJECT) {
        try {
          approveWorkflow(approvalId, appId, executionData.getExecutionStatus());
        } catch (Exception e) {
          log.error("Failed to Approve/Reject Status", e);
        }
        return true;
      }
    } else if (responseData instanceof ErrorNotifyResponseData) {
      log.error("Shell Script Approval task failed unexpectedly {}", responseData);
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
