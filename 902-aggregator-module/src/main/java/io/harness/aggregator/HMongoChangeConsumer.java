package io.harness.aggregator;

import com.google.inject.Singleton;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class HMongoChangeConsumer implements DebeziumEngine.ChangeConsumer<ChangeEvent<String, String>> {
  @Override
  public void handleBatch(List<ChangeEvent<String, String>> list,
      DebeziumEngine.RecordCommitter<ChangeEvent<String, String>> recordCommitter) throws InterruptedException {
    for (ChangeEvent<String, String> changeEvent : list) {
      log.info("Received change event: {}", changeEvent);
      recordCommitter.markProcessed(changeEvent);
    }
    recordCommitter.markBatchFinished();
  }
}
