package io.harness.engine.pms.resume.publisher;

import static io.harness.pms.events.PmsEventFrameworkConstants.PIPELINE_MONITORING_ENABLED;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.engine.utils.OrchestrationEventsFrameworkUtils;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.execution.NodeExecution;
import io.harness.pms.PmsFeatureFlagService;
import io.harness.pms.contracts.execution.ChildChainExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.TaskChainExecutableResponse;
import io.harness.pms.contracts.resume.ChainDetails;
import io.harness.pms.contracts.resume.NodeResumeEvent;
import io.harness.pms.execution.utils.AmbianceUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class RedisNodeResumeEventPublisher implements NodeResumeEventPublisher {
  @Inject private PmsFeatureFlagService pmsFeatureFlagService;
  @Inject private OrchestrationEventsFrameworkUtils eventsFrameworkUtils;

  @Override
  public void publishEvent(NodeExecution nodeExecution, Map<String, ByteString> responseMap, boolean isError) {
    NodeResumeEvent.Builder resumeEventBuilder =
        NodeResumeEvent.newBuilder()
            .setAmbiance(nodeExecution.getAmbiance())
            .setExecutionMode(nodeExecution.getMode())
            .setStepParameters(nodeExecution.getNode().getStepParametersBytes())
            .addAllRefObjects(nodeExecution.getNode().getRebObjectsList())
            .setAsyncError(isError)
            .putAllResponse(responseMap);

    ChainDetails chainDetails = buildChainDetails(nodeExecution);
    if (chainDetails != null) {
      resumeEventBuilder.setChainDetails(chainDetails);
    }
    Producer producer = eventsFrameworkUtils.obtainProducerForNodeResumeEvent(nodeExecution.getNode().getServiceName());

    // TODO : Refactor this and make generic
    Map<String, String> metadataMap = new HashMap<>();
    metadataMap.put(SERVICE_NAME, nodeExecution.getNode().getServiceName());
    if (pmsFeatureFlagService.isEnabled(
            AmbianceUtils.getAccountId(nodeExecution.getAmbiance()), FeatureName.PIPELINE_MONITORING)) {
      metadataMap.put(PIPELINE_MONITORING_ENABLED, "true");
    }

    producer.send(
        Message.newBuilder().putAllMetadata(metadataMap).setData(resumeEventBuilder.build().toByteString()).build());
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
