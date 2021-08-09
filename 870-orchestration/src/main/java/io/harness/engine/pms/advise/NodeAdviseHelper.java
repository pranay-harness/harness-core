package io.harness.engine.pms.advise;

import io.harness.engine.pms.advise.publisher.NodeAdviseEventPublisher;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.Status;

import com.google.inject.Inject;

public class NodeAdviseHelper {
  @Inject private NodeAdviseEventPublisher nodeAdviseEventPublisher;

  public void queueAdvisingEvent(NodeExecution nodeExecution, Status fromStatus) {
    nodeAdviseEventPublisher.publishEvent(nodeExecution.getUuid(), fromStatus);
  }
}
