package io.harness.engine.barriers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.barriers.BarrierExecutionInstance;

import java.util.List;

@OwnedBy(CDC)
public interface BarrierService {
  BarrierExecutionInstance save(BarrierExecutionInstance barrierExecutionInstance);
  List<BarrierExecutionInstance> saveAll(List<BarrierExecutionInstance> barrierExecutionInstances);
  BarrierExecutionInstance get(String barrierUuid);
  List<BarrierExecutionInstance> findByIdentifierAndPlanExecutionId(String identifier, String planExecutionId);
  BarrierExecutionInstance findByPlanNodeId(String planNodeId);
}
