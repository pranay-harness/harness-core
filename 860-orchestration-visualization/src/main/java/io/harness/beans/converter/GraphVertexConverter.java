package io.harness.beans.converter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.GraphVertex;
import io.harness.data.structure.EmptyPredicate;
import io.harness.execution.NodeExecution;
import io.harness.pms.execution.ExecutableResponse;
import io.harness.pms.sdk.core.data.Metadata;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;
import io.harness.serializer.JsonUtils;

import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import net.sf.json.JSON;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class GraphVertexConverter {
  public GraphVertex convertFrom(NodeExecution nodeExecution) {
    return GraphVertex.builder()
        .uuid(nodeExecution.getUuid())
        .ambiance(nodeExecution.getAmbiance())
        .planNodeId(nodeExecution.getNode().getUuid())
        .identifier(nodeExecution.getNode().getIdentifier())
        .name(nodeExecution.getNode().getName())
        .startTs(nodeExecution.getStartTs())
        .endTs(nodeExecution.getEndTs())
        .initialWaitDuration(nodeExecution.getInitialWaitDuration())
        .lastUpdatedAt(nodeExecution.getLastUpdatedAt())
        .stepType(nodeExecution.getNode().getStepType().getType())
        .status(nodeExecution.getStatus())
        .failureInfo(nodeExecution.getFailureInfo())
        .stepParameters(nodeExecution.getResolvedStepParameters() == null
                ? null
                : JsonOrchestrationUtils.asMap(nodeExecution.getResolvedStepParameters().toJson()))
        .mode(nodeExecution.getMode())
        .executableResponsesMetadata(getExecutableResponsesMetadata(nodeExecution))
        .interruptHistories(nodeExecution.getInterruptHistories())
        .retryIds(nodeExecution.getRetryIds())
        .skipType(nodeExecution.getNode().getSkipType())
        .progressDataMap(nodeExecution.getProgressDataMap())
        .build();
  }

  public GraphVertex convertFrom(NodeExecution nodeExecution, List<Outcome> outcomes) {
    return GraphVertex.builder()
        .uuid(nodeExecution.getUuid())
        .ambiance(nodeExecution.getAmbiance())
        .planNodeId(nodeExecution.getNode().getUuid())
        .identifier(nodeExecution.getNode().getIdentifier())
        .name(nodeExecution.getNode().getName())
        .startTs(nodeExecution.getStartTs())
        .endTs(nodeExecution.getEndTs())
        .initialWaitDuration(nodeExecution.getInitialWaitDuration())
        .lastUpdatedAt(nodeExecution.getLastUpdatedAt())
        .stepType(nodeExecution.getNode().getStepType().getType())
        .status(nodeExecution.getStatus())
        .failureInfo(nodeExecution.getFailureInfo())
        .stepParameters(nodeExecution.getResolvedStepParameters() == null
                ? null
                : JsonOrchestrationUtils.asMap(nodeExecution.getResolvedStepParameters().toJson()))
        .mode(nodeExecution.getMode())
        .executableResponsesMetadata(getExecutableResponsesMetadata(nodeExecution))
        .interruptHistories(nodeExecution.getInterruptHistories())
        .retryIds(nodeExecution.getRetryIds())
        .skipType(nodeExecution.getNode().getSkipType())
        .outcomes(outcomes)
        .progressDataMap(nodeExecution.getProgressDataMap())
        .build();
  }

  private List<Map<String, Metadata>> getExecutableResponsesMetadata(NodeExecution nodeExecution) {
    if (EmptyPredicate.isEmpty(nodeExecution.getExecutableResponses())) {
      return Collections.emptyList();
    }
    return nodeExecution.getExecutableResponses()
        .stream()
        .map(r
            -> r.getMetadataMap().entrySet().stream().collect(
                Collectors.toMap(Map.Entry::getKey, v -> JsonUtils.asObject(v.getValue(), Metadata.class))))
        .collect(Collectors.toList());
  }
}
