package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecutionMetadata;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@OwnedBy(PIPELINE)
@HarnessRepo
public interface PlanExecutionMetadataRepository extends CrudRepository<PlanExecutionMetadata, String> {
  Optional<PlanExecutionMetadata> findByPlanExecutionId(String planExecutionId);
}
