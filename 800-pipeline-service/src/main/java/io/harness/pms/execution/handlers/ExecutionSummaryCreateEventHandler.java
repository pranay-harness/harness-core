package io.harness.pms.execution.handlers;

import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.pipeline.PipelineEntity;
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
            .build();
    pmsExecutionSummaryRespository.save(pipelineExecutionSummaryEntity);
  }
}
