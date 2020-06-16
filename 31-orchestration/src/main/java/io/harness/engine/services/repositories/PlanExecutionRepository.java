package io.harness.engine.services.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.execution.PlanExecution;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
public interface PlanExecutionRepository extends CrudRepository<PlanExecution, String> {}
