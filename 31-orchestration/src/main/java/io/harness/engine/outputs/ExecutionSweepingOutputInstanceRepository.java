package io.harness.engine.outputs;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.ExecutionSweepingOutputInstance;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@OwnedBy(CDC)
@HarnessRepo
public interface ExecutionSweepingOutputInstanceRepository
    extends CrudRepository<ExecutionSweepingOutputInstance, String> {
  List<ExecutionSweepingOutputInstance> findByPlanExecutionIdAndNameAndLevelRuntimeIdIdxIn(
      String planExecutionId, String name, List<String> prepareLevelRuntimeIdIndices);
}
