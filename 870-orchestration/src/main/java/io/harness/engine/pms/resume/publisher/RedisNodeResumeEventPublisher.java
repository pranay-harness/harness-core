package io.harness.engine.pms.resume.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.engine.pms.data.PmsTransputHelper;
import io.harness.engine.pms.execution.strategy.identity.IdentityStep;
import io.harness.execution.NodeExecution;
import io.harness.plan.PlanNode;
import io.harness.pms.contracts.execution.ChildChainExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.TaskChainExecutableResponse;
import io.harness.pms.contracts.resume.ChainDetails;
import io.harness.pms.contracts.resume.NodeResumeEvent;
import io.harness.pms.events.base.PmsEventCategory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class RedisNodeResumeEventPublisher implements NodeResumeEventPublisher {
  @Inject private PmsEventSender eventSender;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PmsTransputHelper transputHelper;

  @Override
  public void publishEvent(NodeExecution nodeExecution, Map<String, ByteString> responseMap, boolean isError) {
    PlanNode planNode = nodeExecution.getNode();
    String serviceName = planNode.getServiceName();
    NodeResumeEvent.Builder resumeEventBuilder =
        NodeResumeEvent.newBuilder()
            .setAmbiance(nodeExecution.getAmbiance())
            .setExecutionMode(nodeExecution.getMode())
            .setStepParameters(nodeExecution.getResolvedStepParametersBytes())
            .addAllResolvedInput(transputHelper.resolveInputs(nodeExecution.getAmbiance(), planNode.getRefObjects()))
            .setAsyncError(isError)
            .putAllResponse(responseMap);

    ChainDetails chainDetails = buildChainDetails(nodeExecution);
    if (chainDetails != null) {
      resumeEventBuilder.setChainDetails(chainDetails);
    }

    eventSender.sendEvent(resumeEventBuilder.getAmbiance(), resumeEventBuilder.build().toByteString(),
        PmsEventCategory.NODE_RESUME, serviceName, true);
  }

  @Override
  public void publishEventForIdentityNode(
      NodeExecution nodeExecution, Map<String, ByteString> responseMap, boolean isError, String serviceName) {
    NodeResumeEvent.Builder resumeEventBuilder =
        NodeResumeEvent.newBuilder()
            .setAmbiance(IdentityStep.modifyAmbiance(nodeExecution.getAmbiance()))
            .setExecutionMode(nodeExecution.getMode())
            .setStepParameters(nodeExecution.getResolvedStepParametersBytes())
            .setAsyncError(isError)
            .putAllResponse(responseMap);

    ChainDetails chainDetails = buildChainDetails(nodeExecution);
    if (chainDetails != null) {
      resumeEventBuilder.setChainDetails(chainDetails);
    }

    eventSender.sendEvent(resumeEventBuilder.getAmbiance(), resumeEventBuilder.build().toByteString(),
        PmsEventCategory.NODE_RESUME, serviceName, true);
  }

  public ChainDetails buildChainDetails(NodeExecution nodeExecution) {
    ExecutionMode mode = nodeExecution.getMode();

    if (mode == ExecutionMode.TASK_CHAIN || mode == ExecutionMode.CHILD_CHAIN) {
      switch (mode) {
        case TASK_CHAIN:
          TaskChainExecutableResponse lastLinkResponse =
              Objects.requireNonNull(nodeExecution.obtainLatestExecutableResponse()).getTaskChain();
          return ChainDetails.newBuilder()
              .setIsEnd(lastLinkResponse.getChainEnd())
              .setPassThroughData(lastLinkResponse.getPassThroughData())
              .build();
        case CHILD_CHAIN:
          ChildChainExecutableResponse lastChildChainExecutableResponse = Preconditions.checkNotNull(
              Objects.requireNonNull(nodeExecution.obtainLatestExecutableResponse()).getChildChain());
          boolean chainEnd =
              lastChildChainExecutableResponse.getLastLink() || lastChildChainExecutableResponse.getSuspend();
          return ChainDetails.newBuilder()
              .setIsEnd(chainEnd)
              .setPassThroughData(lastChildChainExecutableResponse.getPassThroughData())
              .build();
        default:
          log.error("This Should Not Happen not a chain mode");
      }
    }
    return null;
  }
}
