package io.harness.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.interrupts.InterruptEffect;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.interrupts.InterruptEffectProto;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
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

  public NodeExecutionProto toNodeExecutionProto(NodeExecution nodeExecution) {
    NodeExecutionProto.Builder builder = NodeExecutionProto.newBuilder()
                                             .setUuid(nodeExecution.getUuid())
                                             .setAmbiance(nodeExecution.getAmbiance())
                                             .setNode(nodeExecution.getNode())
                                             .setStatus(nodeExecution.getStatus())
                                             .setOldRetry(nodeExecution.isOldRetry())
                                             .addAllRetryIds(CollectionUtils.emptyIfNull(nodeExecution.getRetryIds()));

    if (nodeExecution.getMode() != null) {
      builder.setMode(nodeExecution.getMode());
    }
    if (nodeExecution.getStartTs() != null) {
      builder.setStartTs(ProtoUtils.unixMillisToTimestamp(nodeExecution.getStartTs()));
    }
    if (nodeExecution.getEndTs() != null) {
      builder.setEndTs(ProtoUtils.unixMillisToTimestamp(nodeExecution.getEndTs()));
    }
    if (nodeExecution.getInitialWaitDuration() != null) {
      builder.setInitialWaitDuration(ProtoUtils.javaDurationToDuration(nodeExecution.getInitialWaitDuration()));
    }
    if (nodeExecution.getResolvedStepParameters() != null) {
      builder.setResolvedStepParameters(RecastOrchestrationUtils.toJson(nodeExecution.getResolvedStepParameters()));
    }
    if (nodeExecution.getResolvedStepInputs() != null) {
      builder.setResolvedStepInputs(RecastOrchestrationUtils.toJson(nodeExecution.getResolvedStepInputs()));
    }
    if (nodeExecution.getResolvedInputs() != null) {
      builder.setResolvedStepInputs(nodeExecution.getResolvedInputs().toJson());
    }
    if (nodeExecution.getNotifyId() != null) {
      builder.setNotifyId(nodeExecution.getNotifyId());
    }
    if (nodeExecution.getParentId() != null) {
      builder.setParentId(nodeExecution.getParentId());
    }
    if (nodeExecution.getNextId() != null) {
      builder.setNextId(nodeExecution.getNextId());
    }
    if (nodeExecution.getPreviousId() != null) {
      builder.setPreviousId(nodeExecution.getPreviousId());
    }
    if (nodeExecution.getExecutableResponses() != null) {
      builder.addAllExecutableResponses(nodeExecution.getExecutableResponses());
    }
    if (nodeExecution.getOutcomeRefs() != null) {
      builder.addAllOutcomeRefs(nodeExecution.getOutcomeRefs());
    }
    if (nodeExecution.getFailureInfo() != null) {
      builder.setFailureInfo(nodeExecution.getFailureInfo());
    }
    if (EmptyPredicate.isNotEmpty(nodeExecution.getInterruptHistories())) {
      builder.addAllInterruptHistories(nodeExecution.getInterruptHistories()
                                           .stream()
                                           .map(NodeExecutionMapper::toInterruptEffect)
                                           .collect(Collectors.toList()));
    }

    return builder.build();
  }
}
