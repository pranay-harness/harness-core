package io.harness.pms.barriers.service;

import static io.harness.distribution.barrier.Barrier.State.STANDING;

import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.pms.barriers.beans.BarrierExecutionInfo;
import io.harness.pms.barriers.beans.BarrierExecutionInfo.BarrierExecutionInfoBuilder;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierSetupInfo;
import io.harness.steps.barriers.service.BarrierService;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class PMSBarrierServiceImpl implements PMSBarrierService {
  private final NodeExecutionService nodeExecutionService;
  private final BarrierService barrierService;

  @Override
  public List<BarrierSetupInfo> getBarrierSetupInfoList(String yaml) {
    return barrierService.getBarrierSetupInfoList(yaml);
  }

  @Override
  public List<BarrierExecutionInfo> getBarrierExecutionInfoList(String stageSetupId, String planExecutionId) {
    NodeExecution stageNodeExecution = nodeExecutionService.getByPlanNodeUuid(stageSetupId, planExecutionId);
    List<BarrierExecutionInstance> barrierInstances = barrierService.findByStageIdentifierAndPlanExecutionIdAnsStateIn(
        stageNodeExecution.getNode().getIdentifier(), planExecutionId, Sets.newHashSet(STANDING));

    return barrierInstances.stream()
        .map(instance -> {
          BarrierExecutionInfoBuilder builder = BarrierExecutionInfo.builder()
                                                    .name(instance.getName())
                                                    .identifier(instance.getIdentifier())
                                                    .stages(instance.getSetupInfo().getStages())
                                                    .timeoutIn(instance.getSetupInfo().getTimeout());

          try {
            NodeExecution barrierNode =
                nodeExecutionService.getByPlanNodeUuid(instance.getPlanNodeId(), planExecutionId);
            builder.started(true);
            builder.startedAt(barrierNode.getStartTs());
          } catch (InvalidRequestException ignore) {
            builder.started(false);
          }
          return builder.build();
        })
        .collect(Collectors.toList());
  }

  @Override
  public BarrierExecutionInfo getBarrierExecutionInfo(String barrierSetupId, String planExecutionId) {
    BarrierExecutionInstance barrierInstance =
        barrierService.findByPlanNodeIdAndPlanExecutionId(barrierSetupId, planExecutionId);
    return BarrierExecutionInfo.builder()
        .name(barrierInstance.getName())
        .identifier(barrierInstance.getIdentifier())
        .stages(barrierInstance.getSetupInfo().getStages())
        .timeoutIn(barrierInstance.getSetupInfo().getTimeout())
        .build();
  }
}
