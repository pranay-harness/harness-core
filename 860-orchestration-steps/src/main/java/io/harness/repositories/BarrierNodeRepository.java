package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;

import org.springframework.data.repository.CrudRepository;

@OwnedBy(PIPELINE)
@HarnessRepo
public interface BarrierNodeRepository extends CrudRepository<BarrierExecutionInstance, String> {
  BarrierExecutionInstance findByIdentifierAndPlanExecutionId(String identifier, String planExecutionId);
}
