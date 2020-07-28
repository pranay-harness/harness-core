package io.harness.steps.barriers.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@OwnedBy(CDC)
@HarnessRepo
public interface BarrierNodeRepository extends CrudRepository<BarrierExecutionInstance, String> {
  List<BarrierExecutionInstance> findByIdentifierAndPlanExecutionId(String identifier, String planExecutionId);
  Optional<BarrierExecutionInstance> findByPlanNodeId(String planNodeId);
}
