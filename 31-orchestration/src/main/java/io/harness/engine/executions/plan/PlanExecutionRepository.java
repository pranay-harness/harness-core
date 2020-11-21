package io.harness.engine.executions.plan;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecution;

import org.springframework.data.repository.CrudRepository;

@OwnedBy(CDC)
@HarnessRepo
public interface PlanExecutionRepository extends CrudRepository<PlanExecution, String> {}
