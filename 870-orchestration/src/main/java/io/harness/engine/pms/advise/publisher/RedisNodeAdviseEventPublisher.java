package io.harness.engine.pms.advise.publisher;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.events.PmsEventFrameworkConstants.PIPELINE_MONITORING_ENABLED;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.utils.OrchestrationEventsFrameworkUtils;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.execution.NodeExecution;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.PmsFeatureFlagService;
import io.harness.pms.contracts.advisers.AdviseEvent;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.AmbianceUtils;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class RedisNodeAdviseEventPublisher implements NodeAdviseEventPublisher {
  @Inject NodeExecutionService nodeExecutionService;
  @Inject PmsFeatureFlagService pmsFeatureFlagService;
  @Inject private OrchestrationEventsFrameworkUtils eventsFrameworkUtils;

  @Override
  public String publishEvent(String nodeExecutionId, Status fromStatus) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    AdviseEvent adviseEvent =
        AdviseEvent.newBuilder()
            .setAmbiance(nodeExecution.getAmbiance())
            .setFailureInfo(nodeExecution.getFailureInfo())
            .addAllAdviserObtainments(nodeExecution.getNode().getAdviserObtainmentsList())
            .setIsPreviousAdviserExpired(isPreviousAdviserExpired(nodeExecution.getInterruptHistories()))
            .addAllRetryIds(nodeExecution.getRetryIds())
            .setNotifyId(generateUuid())
            .setToStatus(nodeExecution.getStatus())
            .setFromStatus(fromStatus)
            .build();

    Producer producer = eventsFrameworkUtils.obtainProducerForNodeAdviseEvent(nodeExecution.getNode().getServiceName());

    // TODO : Refactor this and make generic
    Map<String, String> metadataMap = new HashMap<>();
    metadataMap.put(SERVICE_NAME, nodeExecution.getNode().getServiceName());
    if (pmsFeatureFlagService.isEnabled(
            AmbianceUtils.getAccountId(nodeExecution.getAmbiance()), FeatureName.PIPELINE_MONITORING)) {
      metadataMap.put(PIPELINE_MONITORING_ENABLED, "true");
    }

    producer.send(Message.newBuilder().putAllMetadata(metadataMap).setData(adviseEvent.toByteString()).build());

    return adviseEvent.getNotifyId();
  }

  private boolean isPreviousAdviserExpired(List<InterruptEffect> interruptHistories) {
    if (interruptHistories.size() == 0) {
      return false;
    }
    return interruptHistories.get(interruptHistories.size() - 1).getInterruptConfig().getIssuedBy().hasTimeoutIssuer();
  }
}
