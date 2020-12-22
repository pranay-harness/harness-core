package io.harness.pms.execution.handlers;

import static io.harness.pms.plan.execution.PlanExecutionResource.EMBEDDED_USER;

import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.beans.ExecutionErrorInfo;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.pipeline.ExecutionSummaryInfo;
import io.harness.pms.pipeline.ExecutionTriggerInfo;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.TriggerType;
import io.harness.pms.pipeline.entity.PipelineExecutionSummaryEntity;
import io.harness.pms.pipeline.mappers.GraphLayoutDtoMapper;
import io.harness.pms.pipeline.resource.GraphLayoutNodeDTO;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.events.SyncOrchestrationEventHandler;
import io.harness.repositories.executions.PmsExecutionSummaryRespository;

import com.google.cloud.Timestamp;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class ExecutionSummaryCreateEventHandler implements SyncOrchestrationEventHandler {
  @Inject PMSPipelineService pmsPipelineService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private PmsExecutionSummaryRespository pmsExecutionSummaryRespository;

  @Override
  public void handleEvent(OrchestrationEvent event) {
    Ambiance ambiance = event.getAmbiance();
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String projectId = AmbianceUtils.getProjectIdentifier(ambiance);
    String orgId = AmbianceUtils.getOrgIdentifier(ambiance);
    String planExecutionId = ambiance.getPlanExecutionId();
    PlanExecution planExecution = planExecutionService.get(planExecutionId);
    String pipelineId = ambiance.getSetupAbstractionsOrDefault(SetupAbstractionKeys.pipelineIdentifier, null);
    Optional<PipelineEntity> pipelineEntity = pmsPipelineService.get(accountId, orgId, projectId, pipelineId, false);
    if (!pipelineEntity.isPresent()) {
      return;
    }
    updateExecutionInfoInPipelineEntity(
        accountId, orgId, projectId, pipelineId, pipelineEntity.get().getExecutionSummaryInfo());
    Map<String, GraphLayoutNode> layoutNodeMap = pipelineEntity.get().getLayoutNodeMap();
    String startingNodeId = pipelineEntity.get().getStartingNodeID();
    Map<String, GraphLayoutNodeDTO> layoutNodeDTOMap = new HashMap<>();
    for (Map.Entry<String, GraphLayoutNode> entry : layoutNodeMap.entrySet()) {
      layoutNodeDTOMap.put(entry.getKey(), GraphLayoutDtoMapper.toDto(entry.getValue()));
    }
    PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity =
        PipelineExecutionSummaryEntity.builder()
            .layoutNodeMap(layoutNodeDTOMap)
            .pipelineIdentifier(pipelineId)
            .startingNodeId(pipelineEntity.get().getStartingNodeID())
            .planExecutionId(planExecutionId)
            .name(pipelineEntity.get().getName())
            .inputSetYaml(ambiance.getSetupAbstractionsMap().get(SetupAbstractionKeys.inputSetYaml))
            .status(ExecutionStatus.NOT_STARTED)
            .startTs(planExecution.getStartTs())
            .startingNodeId(startingNodeId)
            .accountId(accountId)
            .projectIdentifier(projectId)
            .orgIdentifier(orgId)
            .executionTriggerInfo(
                ExecutionTriggerInfo.builder().triggerType(TriggerType.MANUAL).triggeredBy(EMBEDDED_USER).build())
            .build();
    pmsExecutionSummaryRespository.save(pipelineExecutionSummaryEntity);
  }

  public void updateExecutionInfoInPipelineEntity(
      String accountId, String orgId, String projectId, String pipelineId, ExecutionSummaryInfo executionSummaryInfo) {
    if (executionSummaryInfo == null) {
      executionSummaryInfo = ExecutionSummaryInfo.builder().build();
    }
    executionSummaryInfo.setLastExecutionStatus(ExecutionStatus.RUNNING);
    Map<String, Integer> deploymentsMap = executionSummaryInfo.getDeployments();
    Date todaysDate = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    String strDate = formatter.format(todaysDate);
    if (deploymentsMap.containsKey(strDate)) {
      deploymentsMap.put(strDate, deploymentsMap.get(strDate) + 1);
    } else {
      deploymentsMap.put(strDate, 1);
    }
    executionSummaryInfo.setLastExecutionTs(todaysDate.getTime());
    pmsPipelineService.saveExecutionInfo(accountId, orgId, projectId, pipelineId, executionSummaryInfo);
  }
}
