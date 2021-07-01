package io.harness.dto.converter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionNode;
import io.harness.beans.GraphVertex;
import io.harness.data.structure.CollectionUtils;
import io.harness.pms.execution.ExecutionStatus;

import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class GraphVertexDTOConverter {
  public Function<GraphVertex, ExecutionNode> toGraphVertexDTO = graphVertex
      -> ExecutionNode.builder()
             .uuid(graphVertex.getUuid())
             .identifier(graphVertex.getIdentifier())
             .name(graphVertex.getName())
             .startTs(graphVertex.getStartTs())
             .endTs(graphVertex.getEndTs())
             .stepType(graphVertex.getStepType())
             .status(ExecutionStatus.getExecutionStatus(graphVertex.getStatus()))
             .failureInfo(FailureInfoDTOConverter.toFailureInfoDTO(graphVertex.getFailureInfo()))
             .skipInfo(graphVertex.getSkipInfo())
             .nodeRunInfo(graphVertex.getNodeRunInfo())
             .stepParameters(graphVertex.getStepParameters())
             .executableResponses(CollectionUtils.emptyIfNull(graphVertex.getExecutableResponses()))
             .interruptHistories(graphVertex.getInterruptHistories())
             .outcomes(graphVertex.getOutcomeDocuments())
             .unitProgresses(graphVertex.getUnitProgresses())
             .progressData(graphVertex.getProgressData())
             .build();
}
