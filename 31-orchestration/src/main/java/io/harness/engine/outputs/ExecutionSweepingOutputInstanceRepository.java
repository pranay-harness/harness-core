package io.harness.engine.outputs;

import io.harness.annotation.HarnessRepo;
import io.harness.beans.ExecutionSweepingOutputInstance;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@HarnessRepo
public interface ExecutionSweepingOutputInstanceRepository
    extends CrudRepository<ExecutionSweepingOutputInstance, String> {
  List<ExecutionSweepingOutputInstance> findByPlanExecutionIdAndNameAndLevelRuntimeIdIdxIn(
      String planExecutionId, String name, List<String> prepareLevelRuntimeIdIndices);
}
