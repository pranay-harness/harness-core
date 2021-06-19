package io.harness.engine.observers;

import io.harness.execution.NodeExecution;
import io.harness.logging.AutoLogContext;
import io.harness.logging.AutoLogContext.OverrideBehavior;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.AmbianceUtils;

import java.util.Map;
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

  public AutoLogContext autoLogContext() {
    Map<String, String> logContextMap = AmbianceUtils.logContextMap(nodeExecution.getAmbiance());
    logContextMap.put("observerDataType", this.getClass().getSimpleName());
    return new AutoLogContext(logContextMap, OverrideBehavior.OVERRIDE_NESTS);
  }
}
