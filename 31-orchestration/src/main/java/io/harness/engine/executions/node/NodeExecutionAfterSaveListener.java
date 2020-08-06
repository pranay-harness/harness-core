package io.harness.engine.executions.node;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.execution.NodeExecution;
import io.harness.timeout.TimeoutEngine;
import io.harness.timeout.trackers.events.StatusUpdateTimeoutEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;

import java.util.List;

@OwnedBy(CDC)
@Slf4j
public class NodeExecutionAfterSaveListener extends AbstractMongoEventListener<NodeExecution> {
  @Inject private TimeoutEngine timeoutEngine;

  @Override
  public void onAfterSave(AfterSaveEvent<NodeExecution> event) {
    NodeExecution nodeExecution = event.getSource();
    List<String> timeoutInstanceIds = nodeExecution.getTimeoutInstanceIds();
    if (EmptyPredicate.isNotEmpty(timeoutInstanceIds)) {
      timeoutEngine.onEvent(timeoutInstanceIds, new StatusUpdateTimeoutEvent(nodeExecution.getStatus()));
    }
  }
}
