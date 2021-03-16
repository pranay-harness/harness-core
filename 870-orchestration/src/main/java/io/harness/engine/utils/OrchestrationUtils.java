package io.harness.engine.utils;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.StatusUtils;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@UtilityClass
@Slf4j
public class OrchestrationUtils {
  public Status calculateEndStatus(List<NodeExecution> nodeExecutions, String planExecutionId) {
    List<Status> statuses = nodeExecutions.stream()
                                .map(NodeExecution::getStatus)
                                .filter(s -> !StatusUtils.finalizableStatuses().contains(s))
                                .collect(Collectors.toList());
    return StatusUtils.calculateEndStatus(statuses, planExecutionId);
  }
}
