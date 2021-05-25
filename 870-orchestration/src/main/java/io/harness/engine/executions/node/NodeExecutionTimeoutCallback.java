package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.contracts.interrupts.IssuedBy;
import io.harness.pms.contracts.interrupts.TimeoutIssuer;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.serializer.ProtoUtils;
import io.harness.timeout.TimeoutCallback;
import io.harness.timeout.TimeoutDetails;
import io.harness.timeout.TimeoutInstance;

import com.google.inject.Inject;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@TypeAlias("nodeExecutionTimeoutCallback")
public class NodeExecutionTimeoutCallback implements TimeoutCallback {
  @Inject @Transient private transient NodeExecutionService nodeExecutionService;
  @Inject @Transient private transient InterruptManager interruptManager;

  private final String planExecutionId;
  private final String nodeExecutionId;

  public NodeExecutionTimeoutCallback(String planExecutionId, String nodeExecutionId) {
    this.planExecutionId = planExecutionId;
    this.nodeExecutionId = nodeExecutionId;
  }

  @Override
  public void onTimeout(TimeoutInstance timeoutInstance) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    if (nodeExecution == null || !StatusUtils.finalizableStatuses().contains(nodeExecution.getStatus())) {
      return;
    }

    nodeExecutionService.update(
        nodeExecutionId, ops -> ops.set(NodeExecutionKeys.timeoutDetails, new TimeoutDetails(timeoutInstance)));
    interruptManager.register(
        InterruptPackage.builder()
            .planExecutionId(planExecutionId)
            .nodeExecutionId(nodeExecutionId)
            .interruptType(InterruptType.MARK_EXPIRED)
            .interruptConfig(
                InterruptConfig.newBuilder()
                    .setIssuedBy(
                        IssuedBy.newBuilder()
                            .setTimeoutIssuer(
                                TimeoutIssuer.newBuilder().setTimeoutInstanceId(timeoutInstance.getUuid()).build())
                            .setIssueTime(ProtoUtils.unixMillisToTimestamp(System.currentTimeMillis()))
                            .build())
                    .build())
            .build());
  }
}
