package io.harness.engine.utils;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.StatusUtils;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class OrchestrationUtils {
  public Status calculateStatus(List<NodeExecution> nodeExecutions, String planExecutionId) {
    List<Status> statuses = nodeExecutions.stream().map(NodeExecution::getStatus).collect(Collectors.toList());
    return StatusUtils.calculateStatus(statuses, planExecutionId);
  }
}
