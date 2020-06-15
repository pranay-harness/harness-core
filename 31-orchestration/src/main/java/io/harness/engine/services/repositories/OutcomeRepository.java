package io.harness.engine.services.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.data.OutcomeInstance;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
@HarnessRepo
public interface OutcomeRepository extends CrudRepository<OutcomeInstance, String> {
  List<OutcomeInstance> findByPlanExecutionIdAndNameAndProducedBySetupIdOrderByCreatedAtDesc(
      String planExecutionId, String name, String producerSetupId);

  List<OutcomeInstance> findByPlanExecutionIdAndNameAndLevelRuntimeIdIdxIn(
      String planExecutionId, String name, List<String> levelRuntimeIdIndexes);

  List<OutcomeInstance> findByPlanExecutionIdAndProducedByRuntimeIdOrderByCreatedAtDesc(
      String planExecutionId, String producerRuntimeId);
}
