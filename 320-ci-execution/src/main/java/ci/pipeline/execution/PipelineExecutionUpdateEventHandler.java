package ci.pipeline.execution;

import static io.harness.pms.execution.utils.StatusUtils.isFinalStatus;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.ci.CIK8CleanupTaskParams;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class PipelineExecutionUpdateEventHandler implements AsyncOrchestrationEventHandler {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private GitBuildStatusUtility gitBuildStatusUtility;
  @Inject private PodCleanupUtility podCleanupUtility;

  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(2);
  private final int MAX_ATTEMPTS = 3;

  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Override
  public void handleEvent(OrchestrationEvent event) {
    NodeExecutionProto nodeExecutionProto = event.getNodeExecutionProto();
    NodeExecution nodeExecution = NodeExecutionMapper.fromNodeExecutionProto(nodeExecutionProto);
    Ambiance ambiance = nodeExecution.getAmbiance();
    String accountId = AmbianceHelper.getAccountId(ambiance);
    try {
      if (gitBuildStatusUtility.shouldSendStatus(nodeExecution)) {
        log.info("Received event with status {} to update git status for stage {}, planExecutionId {}",
            nodeExecution.getStatus(), nodeExecution.getNode().getIdentifier(), ambiance.getPlanExecutionId());
        gitBuildStatusUtility.sendStatusToGit(nodeExecution, ambiance, accountId);
      }
    } catch (Exception ex) {
      log.error("Failed to send git status update task for node {}, planExecutionId {}", nodeExecution.getUuid(),
          ambiance.getPlanExecutionId(), ex);
    }

    try {
      RetryPolicy<Object> retryPolicy = getRetryPolicy(format("[Retrying failed call to clean pod attempt: {}"),
          format("Failed to clean pod after retrying {} times"));

      Failsafe.with(retryPolicy).run(() -> {
        if (Objects.equals(nodeExecution.getNode().getGroup(), StepOutcomeGroup.STAGE.name())
            && isFinalStatus(nodeExecution.getStatus())) {
          CIK8CleanupTaskParams cik8CleanupTaskParams = podCleanupUtility.buildAndfetchCleanUpParameters(ambiance);

          log.info("Received event with status {} to clean podName {}, planExecutionId {}, stage {}",
              nodeExecution.getStatus(), cik8CleanupTaskParams.getPodNameList(), ambiance.getPlanExecutionId(),
              nodeExecution.getNode().getIdentifier());

          DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                        .accountId(accountId)
                                                        .taskSetupAbstractions(ambiance.getSetupAbstractions())
                                                        .executionTimeout(java.time.Duration.ofSeconds(120))
                                                        .taskType("CI_CLEANUP")
                                                        .taskParameters(cik8CleanupTaskParams)
                                                        .taskDescription("CI cleanup pod task")
                                                        .build();
          String taskId = delegateGrpcClientWrapper.submitAsyncTask(delegateTaskRequest, Duration.ZERO);
          log.info("Submitted cleanup request with taskId {} for podName {}, planExecutionId {}, stage {}", taskId,
              cik8CleanupTaskParams.getPodNameList(), ambiance.getPlanExecutionId(),
              nodeExecution.getNode().getIdentifier());
        }
      });
    } catch (Exception ex) {
      log.error("Failed to send cleanup call for node {}", nodeExecution.getUuid(), ex);
    }
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
