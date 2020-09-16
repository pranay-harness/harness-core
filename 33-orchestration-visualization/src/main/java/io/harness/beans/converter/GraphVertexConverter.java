package io.harness.beans.converter;

import io.harness.beans.GraphVertex;
import io.harness.data.Outcome;
import io.harness.execution.NodeExecution;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class GraphVertexConverter {
  public GraphVertex convertFrom(NodeExecution nodeExecution) {
    return GraphVertex.builder()
        .uuid(nodeExecution.getUuid())
        .planNodeId(nodeExecution.getNode().getUuid())
        .name(nodeExecution.getNode().getName())
        .startTs(nodeExecution.getStartTs())
        .endTs(nodeExecution.getEndTs())
        .initialWaitDuration(nodeExecution.getInitialWaitDuration())
        .lastUpdatedAt(nodeExecution.getLastUpdatedAt())
        .stepType(nodeExecution.getNode().getStepType().getType())
        .status(nodeExecution.getStatus())
        .failureInfo(nodeExecution.getFailureInfo())
        .stepParameters(nodeExecution.getResolvedStepParameters())
        .mode(nodeExecution.getMode())
        .interruptHistories(nodeExecution.getInterruptHistories())
        .retryIds(nodeExecution.getRetryIds())
        .build();
  }

  public GraphVertex convertFrom(NodeExecution nodeExecution, List<Outcome> outcomes) {
    return GraphVertex.builder()
        .uuid(nodeExecution.getUuid())
        .planNodeId(nodeExecution.getNode().getUuid())
        .name(nodeExecution.getNode().getName())
        .startTs(nodeExecution.getStartTs())
        .endTs(nodeExecution.getEndTs())
        .initialWaitDuration(nodeExecution.getInitialWaitDuration())
        .lastUpdatedAt(nodeExecution.getLastUpdatedAt())
        .stepType(nodeExecution.getNode().getStepType().getType())
        .status(nodeExecution.getStatus())
        .failureInfo(nodeExecution.getFailureInfo())
        .stepParameters(nodeExecution.getResolvedStepParameters())
        .mode(nodeExecution.getMode())
        .interruptHistories(nodeExecution.getInterruptHistories())
        .retryIds(nodeExecution.getRetryIds())
        .outcomes(outcomes)
        .build();
  }
}
