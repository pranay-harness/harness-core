package ci.pipeline.execution;

import static io.harness.pms.execution.utils.StatusUtils.isFinalStatus;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.plancreator.steps.common.StageElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.steps.StepOutcomeGroup;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class PipelineExecutionUpdateEventHandler implements AsyncOrchestrationEventHandler {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private GitBuildStatusUtility gitBuildStatusUtility;
  @Inject private PodCleanupUtility podCleanupUtility;

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
      if (Objects.equals(nodeExecution.getNode().getGroup(), StepOutcomeGroup.STAGE.name())
          && isFinalStatus(nodeExecution.getStatus())) {
        StageElementParameters integrationStageStepParameters = RecastOrchestrationUtils.fromDocument(
            nodeExecution.getResolvedStepParameters(), StageElementParameters.class);

        log.info("Received event with status {} to clean stage {}, planExecutionId {}", nodeExecution.getStatus(),
            integrationStageStepParameters.getIdentifier(), ambiance.getPlanExecutionId());
        DelegateTaskRequest delegateTaskRequest =
            DelegateTaskRequest.builder()
                .accountId(accountId)
                .taskSetupAbstractions(ambiance.getSetupAbstractions())
                .executionTimeout(java.time.Duration.ofSeconds(60))
                .taskType("CI_CLEANUP")
                .taskParameters(podCleanupUtility.buildAndfetchCleanUpParameters(ambiance))
                .taskDescription("CI cleanup pod task")
                .build();

        String taskId = delegateGrpcClientWrapper.submitAsyncTask(delegateTaskRequest, Duration.ZERO);
        log.info("Submitted cleanup request  with taskId {} for Integration stage  {}, planExecutionId {}", taskId,
            integrationStageStepParameters.getIdentifier(), ambiance.getPlanExecutionId());
      }
    } catch (Exception ex) {
      log.error("Failed to send cleanup call for node {}", nodeExecution.getUuid(), ex);
    }
  }
}
