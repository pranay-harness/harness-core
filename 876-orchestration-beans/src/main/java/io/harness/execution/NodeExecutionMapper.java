package io.harness.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.interrupts.InterruptEffectProto;
import io.harness.serializer.ProtoUtils;

import java.util.Collections;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class NodeExecutionMapper {
  public NodeExecution fromNodeExecutionProto(NodeExecutionProto proto) {
    return NodeExecution.builder()
        .uuid(proto.getUuid())
        .ambiance(proto.getAmbiance())
        .node(proto.getNode())
        .mode(proto.getMode())
        .startTs(ProtoUtils.timestampToUnixMillis(proto.getStartTs()))
        .endTs(ProtoUtils.timestampToUnixMillis(proto.getEndTs()))
        .resolvedStepInputs(proto.getResolvedStepInputs())
        .initialWaitDuration(ProtoUtils.durationToJavaDuration(proto.getInitialWaitDuration()))
        .resolvedStepParameters(proto.getResolvedStepParameters())
        .notifyId(proto.getNotifyId())
        .parentId(proto.getParentId())
        .nextId(proto.getParentId())
        .previousId(proto.getPreviousId())
        .status(proto.getStatus())
        .executableResponses(CollectionUtils.emptyIfNull(proto.getExecutableResponsesList()))
        .interruptHistories(Collections.emptyList())
        .failureInfo(proto.getFailureInfo())
        .retryIds(Collections.emptyList())
        .oldRetry(false)
        .timeoutInstanceIds(Collections.emptyList())
        .timeoutDetails(null)
        .outcomeRefs(CollectionUtils.emptyIfNull(proto.getOutcomeRefsList()))
        .retryIds(proto.getRetryIdsList())
        .oldRetry(proto.getOldRetry())
        .interruptHistories(proto.getInterruptHistoriesList()
                                .stream()
                                .map(NodeExecutionMapper::fromInterruptEffectProto)
                                .collect(Collectors.toList()))
        .build();
  }

  private InterruptEffect fromInterruptEffectProto(InterruptEffectProto interruptEffectProto) {
    return InterruptEffect.builder()
        .interruptId(interruptEffectProto.getInterruptId())
        .interruptType(interruptEffectProto.getInterruptType())
        .interruptConfig(interruptEffectProto.getInterruptConfig())
        .tookEffectAt(ProtoUtils.timestampToUnixMillis(interruptEffectProto.getTookEffectAt()))
        .build();
  }

  private InterruptEffectProto toInterruptEffect(InterruptEffect interruptEffect) {
    return InterruptEffectProto.newBuilder()
        .setTookEffectAt(ProtoUtils.unixMillisToTimestamp(interruptEffect.getTookEffectAt()))
        .setInterruptConfig(interruptEffect.getInterruptConfig())
        .setInterruptId(interruptEffect.getInterruptId())
        .setInterruptType(interruptEffect.getInterruptType())
        .build();
  }
}
