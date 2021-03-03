package ci.pipeline.execution;

import static io.harness.pms.execution.utils.StatusUtils.isFinalStatus;

import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.stages.IntegrationStageStepParametersPMS;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.sdk.core.events.AsyncOrchestrationEventHandler;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.steps.StepOutcomeGroup;

import com.google.inject.Inject;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    if (gitBuildStatusUtility.shouldSendStatus(nodeExecution)) {
      gitBuildStatusUtility.sendStatusToGit(nodeExecution, ambiance, accountId);
    }

    if (Objects.equals(nodeExecution.getNode().getGroup(), StepOutcomeGroup.STAGE.name())
        && isFinalStatus(nodeExecution.getStatus())) {
      IntegrationStageStepParametersPMS integrationStageStepParameters = RecastOrchestrationUtils.fromDocument(
          nodeExecution.getResolvedStepParameters(), IntegrationStageStepParametersPMS.class);

      DelegateTaskRequest delegateTaskRequest =
          DelegateTaskRequest.builder()
              .accountId(accountId)
              .taskSetupAbstractions(ambiance.getSetupAbstractions())
              .executionTimeout(java.time.Duration.ofSeconds(60))
              .taskType("CI_CLEANUP")
              .taskParameters(podCleanupUtility.buildAndfetchCleanUpParameters(ambiance))
              .taskDescription("CI cleanup pod task")
              .build();

      String taskId = delegateGrpcClientWrapper.submitAsyncTask(delegateTaskRequest);
      log.info("Submitted cleanup request  with taskId {} for Integration stage  {}", taskId,
          integrationStageStepParameters.getIdentifier());
    }
  }
}
