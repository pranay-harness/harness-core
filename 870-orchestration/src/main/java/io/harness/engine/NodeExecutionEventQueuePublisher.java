package io.harness.engine;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.queue.QueuePublisher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class NodeExecutionEventQueuePublisher {
  @Inject private QueuePublisher<NodeExecutionEvent> nodeExecutionEventQueuePublisher;

  public void send(NodeExecutionEvent event) {
    nodeExecutionEventQueuePublisher.send(
        Collections.singletonList(event.getNodeExecution().getNode().getServiceName()), event);
  }
}
