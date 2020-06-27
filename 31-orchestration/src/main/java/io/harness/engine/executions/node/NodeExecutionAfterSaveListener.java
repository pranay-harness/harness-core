package io.harness.engine.executions.node;

import io.harness.execution.NodeExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;

@Slf4j
public class NodeExecutionAfterSaveListener extends AbstractMongoEventListener<NodeExecution> {
  @Override
  public void onAfterSave(AfterSaveEvent<NodeExecution> event) {
    logger.info("After Save Event Called for ");
  }
}
