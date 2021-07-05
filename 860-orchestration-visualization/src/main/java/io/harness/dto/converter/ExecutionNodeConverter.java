package io.harness.dto.converter;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateInfo;
import io.harness.beans.ExecutionNode;
import io.harness.beans.GraphVertex;
import io.harness.data.structure.CollectionUtils;
import io.harness.dto.GraphDelegateSelectionLogParams;
import io.harness.dto.LevelDTO;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class ExecutionNodeConverter {
  public Function<GraphVertex, ExecutionNode> toGraphVertexDTO = graphVertex
      -> ExecutionNode.builder()
             .uuid(graphVertex.getUuid())
             .identifier(graphVertex.getIdentifier())
             .name(graphVertex.getName())
             .startTs(graphVertex.getStartTs())
             .endTs(graphVertex.getEndTs())
             .setupId(graphVertex.getPlanNodeId())
             .delegateInfoList(
                 mapDelegateSelectionLogParamsToDelegateInfo(graphVertex.getGraphDelegateSelectionLogParams()))
             .stepType(graphVertex.getStepType())
             //             .baseFqn(getFQNUsingLevels(graphVertex.getAmbiance().getLevelsList())
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

  private List<DelegateInfo> mapDelegateSelectionLogParamsToDelegateInfo(
      List<GraphDelegateSelectionLogParams> delegateSelectionLogParams) {
    return delegateSelectionLogParams.stream()
        .filter(param -> param.getSelectionLogParams() != null)
        .map(ExecutionNodeConverter::getDelegateInfoForUI)
        .collect(Collectors.toList());
  }
  private DelegateInfo getDelegateInfoForUI(GraphDelegateSelectionLogParams graphDelegateSelectionLogParams) {
    return DelegateInfo.builder()
        .id(graphDelegateSelectionLogParams.getSelectionLogParams().getDelegateId())
        .name(graphDelegateSelectionLogParams.getSelectionLogParams().getDelegateName())
        .taskId(graphDelegateSelectionLogParams.getTaskId())
        .taskName(graphDelegateSelectionLogParams.getTaskName())
        .build();
  }
  public String getFQNUsingLevels(List<LevelDTO> levels) {
    List<String> fqnList = new ArrayList<>();
    for (LevelDTO level : levels) {
      if (!YamlUtils.shouldNotIncludeInQualifiedName(level.getIdentifier())
          && !level.getIdentifier().equals(YAMLFieldNameConstants.PARALLEL + level.getSetupId())
          && !level.isSkipExpressionChain()) {
        fqnList.add(level.getIdentifier());
      }
    }
    return String.join(".", fqnList);
  }
}
