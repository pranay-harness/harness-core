package io.harness.steps.barriers.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.barrier.Barrier.State;
import io.harness.execution.NodeExecution;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierSetupInfo;

import java.util.List;
import java.util.Set;

@OwnedBy(CDC)
public interface BarrierService {
  BarrierExecutionInstance save(BarrierExecutionInstance barrierExecutionInstance);
  List<BarrierExecutionInstance> saveAll(List<BarrierExecutionInstance> barrierExecutionInstances);
  BarrierExecutionInstance get(String barrierUuid);
  BarrierExecutionInstance update(BarrierExecutionInstance barrierExecutionInstance);
  BarrierExecutionInstance updateState(String uuid, State state);
  List<BarrierExecutionInstance> findByIdentifierAndPlanExecutionId(String identifier, String planExecutionId);
  BarrierExecutionInstance findByPlanNodeId(String planNodeId);
  List<BarrierExecutionInstance> findByStageIdentifierAndPlanExecutionIdAnsStateIn(
      String stageIdentifier, String planExecutionId, Set<State> stateSet);
  List<NodeExecution> findBarrierNodesByPlanExecutionIdAndIdentifier(String planExecutionId, String identifier);
  List<BarrierSetupInfo> getBarrierSetupInfoList(String yaml);
}
