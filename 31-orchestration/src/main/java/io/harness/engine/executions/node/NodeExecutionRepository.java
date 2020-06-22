package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.Status;
import org.springframework.data.repository.CrudRepository;

import java.util.EnumSet;
import java.util.List;

@OwnedBy(CDC)
@HarnessRepo
public interface NodeExecutionRepository extends CrudRepository<NodeExecution, String> {
  List<NodeExecution> findByAmbiancePlanExecutionId(String planExecutionId);

  List<NodeExecution> findByAmbiancePlanExecutionIdAndOldRetry(String planExecutionId, Boolean oldRetry);

  List<NodeExecution> findByAmbiancePlanExecutionIdAndParentIdOrderByCreatedAtDesc(
      String planExecutionId, String parentId);

  List<NodeExecution> findByAmbiancePlanExecutionIdAndStatus(String planExecutionId, Status status);

  List<NodeExecution> findByAmbiancePlanExecutionIdAndStatusIn(String planExecutionId, EnumSet<Status> statuses);

  List<NodeExecution> findByAmbiancePlanExecutionIdAndParentIdInAndStatusIn(
      String planExecutionId, List<String> parentIds, EnumSet<Status> statuses);
}
