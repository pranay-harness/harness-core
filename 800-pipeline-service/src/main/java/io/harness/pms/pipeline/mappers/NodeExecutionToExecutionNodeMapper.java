package io.harness.pms.pipeline.mappers;

import io.harness.DelegateInfoHelper;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateInfo;
import io.harness.beans.ExecutionNode;
import io.harness.data.structure.EmptyPredicate;
import io.harness.dto.GraphDelegateSelectionLogParams;
import io.harness.dto.converter.FailureInfoDTOConverter;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.pms.data.PmsOutcome;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.utils.OrchestrationMapBackwardCompatibilityUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class NodeExecutionToExecutionNodeMapper {
  @Inject PmsOutcomeService pmsOutcomeService;
  @Inject private DelegateInfoHelper delegateInfoHelper;

  public ExecutionNode mapNodeExecutionToExecutionNode(NodeExecution nodeExecution) {
    Map<String, PmsOutcome> outcomes =
        PmsOutcome.convertJsonToOrchestrationMap(pmsOutcomeService.findAllOutcomesMapByRuntimeId(
            nodeExecution.getAmbiance().getPlanExecutionId(), nodeExecution.getUuid()));

    List<GraphDelegateSelectionLogParams> graphDelegateSelectionLogParamsList =
        delegateInfoHelper.getDelegateInformationForGivenTask(nodeExecution.getExecutableResponses(),
            nodeExecution.getMode(), AmbianceUtils.getAccountId(nodeExecution.getAmbiance()));

    List<DelegateInfo> delegateInfoList = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(graphDelegateSelectionLogParamsList)) {
      for (GraphDelegateSelectionLogParams graphDelegateSelectionLogParams : graphDelegateSelectionLogParamsList) {
        delegateInfoList.add(ExecutionGraphMapper.getDelegateInfoForUI(graphDelegateSelectionLogParams));
      }
    }

    return ExecutionNode.builder()
        .uuid(nodeExecution.getUuid())
        .setupId(nodeExecution.getNode().getUuid())
        .name(nodeExecution.getNode().getName())
        .identifier(nodeExecution.getNode().getIdentifier())
        .stepParameters(nodeExecution.getPmsStepParameters())
        .startTs(nodeExecution.getStartTs())
        .endTs(nodeExecution.getEndTs())
        .stepType(nodeExecution.getNode().getStepType().getType())
        .status(ExecutionStatus.getExecutionStatus(nodeExecution.getStatus()))
        .failureInfo(FailureInfoDTOConverter.toFailureInfoDTO(nodeExecution.getFailureInfo()))
        .interruptHistories(nodeExecution.getInterruptHistories())
        .skipInfo(nodeExecution.getSkipInfo())
        .nodeRunInfo(nodeExecution.getNodeRunInfo())
        .executableResponses(nodeExecution.getExecutableResponses())
        .unitProgresses(nodeExecution.getUnitProgresses())
        .progressData(nodeExecution.getPmsProgressData())
        .outcomes(OrchestrationMapBackwardCompatibilityUtils.convertToOrchestrationMap(outcomes))
        .baseFqn(null)
        .delegateInfoList(delegateInfoList)
        .build();
  }
}
