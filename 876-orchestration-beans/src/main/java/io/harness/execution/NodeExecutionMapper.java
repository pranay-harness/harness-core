package io.harness.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.pms.execution.NodeExecutionProto;
import io.harness.serializer.ProtoUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
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
        .progressDataMap(new LinkedHashMap<>())
        .build();
  }

  public NodeExecutionProto toNodeExecutionProto(NodeExecution nodeExecution) {
    NodeExecutionProto.Builder builder = NodeExecutionProto.newBuilder()
                                             .setUuid(nodeExecution.getUuid())
                                             .setAmbiance(nodeExecution.getAmbiance())
                                             .setNode(nodeExecution.getNode())
                                             .setStatus(nodeExecution.getStatus());

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
      builder.setResolvedStepParameters(nodeExecution.getResolvedStepParameters().toJson());
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
    return builder.build();
  }
}
