package io.harness.engine.observers;

import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.Status;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class NodeUpdateInfo {
  @NonNull NodeExecution nodeExecution;

  public String getNodeExecutionId() {
    return nodeExecution.getUuid();
  }

  public String getPlanExecutionId() {
    return nodeExecution.getAmbiance().getPlanExecutionId();
  }

  public Status getStatus() {
    return nodeExecution.getStatus();
  }
}
