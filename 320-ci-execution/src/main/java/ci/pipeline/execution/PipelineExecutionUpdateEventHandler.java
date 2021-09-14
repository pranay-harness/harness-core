/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package ci.pipeline.execution;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.PmsCommonConstants.AUTO_ABORT_PIPELINE_THROUGH_TRIGGER;
import static io.harness.pms.execution.utils.StatusUtils.isFinalStatus;
import static io.harness.steps.StepUtils.buildAbstractions;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.ci.CIK8CleanupTaskParams;
import io.harness.encryption.Scope;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class PipelineExecutionUpdateEventHandler implements OrchestrationEventHandler {
  @Inject private GitBuildStatusUtility gitBuildStatusUtility;
  @Inject private PodCleanupUtility podCleanupUtility;

  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(2);
  private final int MAX_ATTEMPTS = 3;

  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    Level level = AmbianceUtils.obtainCurrentLevel(ambiance);
    Status status = event.getStatus();
    try {
      if (gitBuildStatusUtility.shouldSendStatus(level.getStepType().getStepCategory())) {
        log.info("Received event with status {} to update git status for stage {}, planExecutionId {}", status,
            level.getIdentifier(), ambiance.getPlanExecutionId());
        if (isAutoAbortThroughTrigger(event)) {
          log.info("Skipping updating Git status as execution was Auto aborted by trigger due to newer execution");
        } else {
          gitBuildStatusUtility.sendStatusToGit(status, event.getResolvedStepParameters(), ambiance, accountId);
        }
      }
    } catch (Exception ex) {
      log.error("Failed to send git status update task for node {}, planExecutionId {}", level.getRuntimeId(),
          ambiance.getPlanExecutionId(), ex);
    }

    try {
      RetryPolicy<Object> retryPolicy = getRetryPolicy(format("[Retrying failed call to clean pod attempt: {}"),
          format("Failed to clean pod after retrying {} times"));

      Failsafe.with(retryPolicy).run(() -> {
        if (level.getStepType().getStepCategory() == StepCategory.STAGE && isFinalStatus(status)) {
          CIK8CleanupTaskParams cik8CleanupTaskParams = podCleanupUtility.buildAndfetchCleanUpParameters(ambiance);

          log.info("Received event with status {} to clean podName {}, planExecutionId {}, stage {}", status,
              cik8CleanupTaskParams.getPodNameList(), ambiance.getPlanExecutionId(), level.getIdentifier());

          Map<String, String> abstractions = buildAbstractions(ambiance, Scope.PROJECT);
          DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                        .accountId(accountId)
                                                        .taskSetupAbstractions(abstractions)
                                                        .executionTimeout(java.time.Duration.ofSeconds(900))
                                                        .taskType("CI_CLEANUP")
                                                        .taskParameters(cik8CleanupTaskParams)
                                                        .taskDescription("CI cleanup pod task")
                                                        .build();

          String taskId = delegateGrpcClientWrapper.submitAsyncTask(delegateTaskRequest, Duration.ZERO);
          log.info("Submitted cleanup request with taskId {} for podName {}, planExecutionId {}, stage {}", taskId,
              cik8CleanupTaskParams.getPodNameList(), ambiance.getPlanExecutionId(), level.getIdentifier());
        }
      });
    } catch (Exception ex) {
      log.error("Failed to send cleanup call for node {}", level.getRuntimeId(), ex);
    }
  }

  // When trigger has "Auto Abort Prev Executions" ebanled, it will abort prev running execution and start a new one.
  // e.g. pull_request  event for same PR
  private boolean isAutoAbortThroughTrigger(OrchestrationEvent event) {
    if (isEmpty(event.getTags())) {
      return false;
    }

    boolean isAutoAbort = false;
    if (event.getTags().contains(AUTO_ABORT_PIPELINE_THROUGH_TRIGGER)) {
      isAutoAbort = true;
    }

    return isAutoAbort;
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withBackoff(10, 60, ChronoUnit.MINUTES)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
