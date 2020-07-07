package io.harness.engine.barriers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.barriers.BarrierExecutionInstance;
import io.harness.distribution.barrier.Barrier.State;

import java.util.List;

@OwnedBy(CDC)
public interface BarrierService {
  BarrierExecutionInstance save(BarrierExecutionInstance barrierExecutionInstance);
  List<BarrierExecutionInstance> saveAll(List<BarrierExecutionInstance> barrierExecutionInstances);
  BarrierExecutionInstance get(String barrierUuid);
  BarrierExecutionInstance update(BarrierExecutionInstance barrierExecutionInstance);
  BarrierExecutionInstance updateState(String uuid, State state);
  List<BarrierExecutionInstance> findByIdentifierAndPlanExecutionId(String identifier, String planExecutionId);
  BarrierExecutionInstance findByPlanNodeId(String planNodeId);
}
