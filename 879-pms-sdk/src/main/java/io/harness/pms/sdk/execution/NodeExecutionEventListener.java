package io.harness.pms.sdk.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class NodeExecutionEventListener extends QueueListener<NodeExecutionEvent> {
  @Inject
  public NodeExecutionEventListener(QueueConsumer<NodeExecutionEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(NodeExecutionEvent event) {
    log.info("Notifying for NodeExecutionEvent: type: {}, id: {}", event.getEventType().name(),
        event.getNodeExecution().getUuid());
  }
}
