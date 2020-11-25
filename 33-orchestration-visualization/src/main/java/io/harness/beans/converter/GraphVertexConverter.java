package io.harness.beans.converter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.GraphVertex;
import io.harness.data.Outcome;
import io.harness.data.structure.EmptyPredicate;
import io.harness.execution.NodeExecution;
import io.harness.facilitator.modes.ExecutableResponse;
import io.harness.serializer.JsonUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

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
                : JsonUtils.asMap(nodeExecution.getResolvedStepParameters().toJson()))
        .mode(nodeExecution.getMode())
        .executableResponsesMetadata(getExecutableResponsesMetadata(nodeExecution))
        .interruptHistories(nodeExecution.getInterruptHistories())
        .retryIds(nodeExecution.getRetryIds())
        .skipType(nodeExecution.getNode().getSkipGraphType())
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
                : JsonUtils.asMap(nodeExecution.getResolvedStepParameters().toJson()))
        .mode(nodeExecution.getMode())
        .executableResponsesMetadata(getExecutableResponsesMetadata(nodeExecution))
        .interruptHistories(nodeExecution.getInterruptHistories())
        .retryIds(nodeExecution.getRetryIds())
        .skipType(nodeExecution.getNode().getSkipGraphType())
        .outcomes(outcomes)
        .build();
  }

  private List<Map<String, String>> getExecutableResponsesMetadata(NodeExecution nodeExecution) {
    if (EmptyPredicate.isEmpty(nodeExecution.getExecutableResponses())) {
      return Collections.emptyList();
    }
    return nodeExecution.getExecutableResponses()
        .stream()
        .map(ExecutableResponse::getMetadata)
        .collect(Collectors.toList());
  }
}
