package io.harness.engine.advise.publisher;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.utils.OrchestrationEventsFrameworkUtils;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecutionMapper;
import io.harness.pms.contracts.advisers.AdviseEvent;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.InterruptEffectProto;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public class RedisNodeAdviseEventPublisher implements NodeAdviseEventPublisher {
  @Inject NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEventsFrameworkUtils eventsFrameworkUtils;

  @Override
  public String publishEvent(String nodeExecutionId, Status fromStatus) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    NodeExecutionProto nodeExecutionProto = NodeExecutionMapper.toNodeExecutionProto(nodeExecution);

    AdviseEvent adviseEvent =
        AdviseEvent.newBuilder()
            .setAmbiance(nodeExecutionProto.getAmbiance())
            .setFailureInfo(nodeExecutionProto.getFailureInfo())
            .addAllAdviserObtainments(nodeExecutionProto.getNode().getAdviserObtainmentsList())
            .setIsPreviousAdviserExpired(isPreviousAdviserExpired(nodeExecutionProto.getInterruptHistoriesList()))
            .addAllRetryIds(nodeExecutionProto.getRetryIdsList())
            .setNotifyId(generateUuid())
            .setToStatus(nodeExecutionProto.getStatus())
            .setFromStatus(fromStatus)
            .build();

    Producer producer = eventsFrameworkUtils.obtainProducerForNodeAdviseEvent(nodeExecution.getNode().getServiceName());

    producer.send(Message.newBuilder()
                      .putAllMetadata(ImmutableMap.of(SERVICE_NAME, nodeExecution.getNode().getServiceName()))
                      .setData(adviseEvent.toByteString())
                      .build());

    return adviseEvent.getNotifyId();
  }

  private boolean isPreviousAdviserExpired(List<InterruptEffectProto> interruptHistories) {
    if (interruptHistories.size() == 0) {
      return false;
    }
    return interruptHistories.get(interruptHistories.size() - 1).getInterruptConfig().getIssuedBy().hasTimeoutIssuer();
  }
}
