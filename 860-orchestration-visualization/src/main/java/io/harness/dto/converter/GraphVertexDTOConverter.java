package io.harness.dto.converter;

import io.harness.beans.GraphVertex;
import io.harness.dto.GraphVertexDTO;

import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GraphVertexDTOConverter {
  public Function<GraphVertex, GraphVertexDTO> toGraphVertexDTO = graphVertex
      -> GraphVertexDTO.builder()
             .uuid(graphVertex.getUuid())
             .ambiance(AmbianceDTOConverter.toAmbianceDTO.apply(graphVertex.getAmbiance()))
             .planNodeId(graphVertex.getPlanNodeId())
             .identifier(graphVertex.getIdentifier())
             .name(graphVertex.getName())
             .startTs(graphVertex.getStartTs())
             .endTs(graphVertex.getEndTs())
             .initialWaitDuration(graphVertex.getInitialWaitDuration())
             .lastUpdatedAt(graphVertex.getLastUpdatedAt())
             .stepType(graphVertex.getStepType())
             .status(graphVertex.getStatus())
             .failureInfo(graphVertex.getFailureInfo())
             .stepParameters(graphVertex.getStepParameters())
             .mode(graphVertex.getMode())
             .executableResponsesMetadata(graphVertex.getExecutableResponsesMetadata())
             .interruptHistories(graphVertex.getInterruptHistories())
             .retryIds(graphVertex.getRetryIds())
             .skipType(graphVertex.getSkipType())
             .outcomes(graphVertex.getOutcomes())
             .progressDataMap(graphVertex.getProgressDataMap())
             .build();
}
