package io.harness.pms.pipeline.mappers;

import io.harness.DelegateInfoHelper;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateInfo;
import io.harness.beans.ExecutionNode;
import io.harness.data.structure.EmptyPredicate;
import io.harness.dto.GraphDelegateSelectionLogParams;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.resolver.outcome.mapper.PmsOutcomeMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class NodeExecutionToExecutioNodeMapper {
  @Inject PmsOutcomeService pmsOutcomeService;
  @Inject private DelegateInfoHelper delegateInfoHelper;

  public ExecutionNode mapNodeExecutionToExecutionNode(NodeExecution nodeExecution) {
    List<Document> outcomes = PmsOutcomeMapper.convertJsonToDocument(pmsOutcomeService.findAllByRuntimeId(
        nodeExecution.getAmbiance().getPlanExecutionId(), nodeExecution.getUuid(), true));

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
        .stepParameters(nodeExecution.getResolvedStepInputs())
        .startTs(nodeExecution.getStartTs())
        .endTs(nodeExecution.getEndTs())
        .stepType(nodeExecution.getNode().getStepType().getType())
        .taskIdToProgressDataMap(nodeExecution.getProgressDataMap())
        .status(ExecutionStatus.getExecutionStatus(nodeExecution.getStatus()))
        .failureInfo(nodeExecution.getFailureInfo())
        .interruptHistories(nodeExecution.getInterruptHistories())
        .skipInfo(nodeExecution.getSkipInfo())
        .nodeRunInfo(nodeExecution.getNodeRunInfo())
        .executableResponses(nodeExecution.getExecutableResponses())
        .unitProgresses(nodeExecution.getUnitProgresses())
        .outcomes(outcomes)
        .baseFqn(null)
        .delegateInfoList(delegateInfoList)
        .build();
  }
}
