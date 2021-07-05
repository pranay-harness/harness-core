package io.harness.dto.converter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.GraphVertex;
import io.harness.data.structure.CollectionUtils;
import io.harness.dto.GraphVertexDTO;

import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class GraphVertexDTOConverter {
  public Function<GraphVertex, GraphVertexDTO> toGraphVertexDTO = graphVertex
      -> GraphVertexDTO.builder()
             .uuid(graphVertex.getUuid())
             //             .ambiance(AmbianceDTOConverter.toAmbianceDTO.apply(graphVertex.getAmbiance()))
             //             .planNodeId(graphVertex.getPlanNodeId())
             .identifier(graphVertex.getIdentifier())
             .name(graphVertex.getName())
             .startTs(graphVertex.getStartTs())
             .endTs(graphVertex.getEndTs())
             //             .initialWaitDuration(graphVertex.getInitialWaitDuration())
             //             .lastUpdatedAt(graphVertex.getLastUpdatedAt())
             .stepType(graphVertex.getStepType())
             .status(graphVertex.getStatus())
             .failureInfo(FailureInfoDTOConverter.toFailureInfoDTO(graphVertex.getFailureInfo()))
             .skipInfo(graphVertex.getSkipInfo())
             .nodeRunInfo(graphVertex.getNodeRunInfo())
             .stepParameters(graphVertex.getStepParameters())
             //             .mode(graphVertex.getMode())
             .executableResponses(CollectionUtils.emptyIfNull(graphVertex.getExecutableResponses()))
             //             .graphDelegateSelectionLogParams(
             //                 CollectionUtils.emptyIfNull(graphVertex.getGraphDelegateSelectionLogParams()))
             .interruptHistories(graphVertex.getInterruptHistories())
             //             .retryIds(graphVertex.getRetryIds())
             //             .skipType(graphVertex.getSkipType())
             .outcomes(graphVertex.getOutcomeDocuments())
             .unitProgresses(graphVertex.getUnitProgresses())
             .progressData(graphVertex.getProgressData())
             .build();
}
