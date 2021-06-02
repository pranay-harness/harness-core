package io.harness.aggregator.consumers;

import io.harness.accesscontrol.AccessControlEntity;
import io.harness.aggregator.OpType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateFieldException;

import com.google.inject.Singleton;
import io.debezium.embedded.EmbeddedEngineChangeEvent;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.dao.DuplicateKeyException;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class AccessControlDebeziumChangeConsumer implements DebeziumEngine.ChangeConsumer<ChangeEvent<String, String>> {
  private static final String OP_FIELD = "__op";
  private final Deserializer<String> idDeserializer;
  private final Map<String, Deserializer<? extends AccessControlEntity>> collectionToDeserializerMap;
  private final Map<String, ChangeConsumer<? extends AccessControlEntity>> collectionToConsumerMap;
  private final Retry retry;
  private final ChangeEventFailureHandler changeEventFailureHandler;

  public AccessControlDebeziumChangeConsumer(Deserializer<String> idDeserializer,
      Map<String, Deserializer<? extends AccessControlEntity>> collectionToDeserializerMap,
      Map<String, ChangeConsumer<? extends AccessControlEntity>> collectionToConsumerMap,
      ChangeEventFailureHandler changeEventFailureHandler) {
    this.idDeserializer = idDeserializer;
    this.collectionToDeserializerMap = collectionToDeserializerMap;
    this.collectionToConsumerMap = collectionToConsumerMap;
    this.changeEventFailureHandler = changeEventFailureHandler;
    IntervalFunction intervalFunction = IntervalFunction.ofExponentialBackoff(1000, 2);
    RetryConfig retryConfig = RetryConfig.custom()
                                  .ignoreExceptions(DuplicateKeyException.class, DuplicateFieldException.class)
                                  .intervalFunction(intervalFunction)
                                  .maxAttempts(10)
                                  .build();
    retry = Retry.of("debeziumEngineRetry", retryConfig);
  }

  private boolean handleEvent(ChangeEvent<String, String> changeEvent) {
    String id = idDeserializer.deserialize(null, changeEvent.key().getBytes());
    String collectionName = getCollectionName(changeEvent.destination());
    OpType opType = getOperationType(((EmbeddedEngineChangeEvent<String, String>) changeEvent).sourceRecord());

    log.info("Received {} event for entity: {} in collection: {}", opType, id, collectionName);

    ChangeConsumer<? extends AccessControlEntity> changeConsumer = collectionToConsumerMap.get(collectionName);
    changeConsumer.consumeEvent(opType, id, deserialize(changeEvent));
    return true;
  }

  @SneakyThrows
  @Override
  public void handleBatch(List<ChangeEvent<String, String>> list,
      DebeziumEngine.RecordCommitter<ChangeEvent<String, String>> recordCommitter) {
    for (ChangeEvent<String, String> changeEvent : list) {
      try {
        handleEvent(changeEvent);
      } catch (Exception exception) {
        changeEventFailureHandler.handle(changeEvent, exception);
        log.error(
            "Exception caught when trying to process event: {}. Retrying this event with exponential backoff now...",
            changeEvent, exception);
        retry.executeSupplier(() -> handleEvent(changeEvent));
      }
      recordCommitter.markProcessed(changeEvent);
    }
    recordCommitter.markBatchFinished();
  }

  private <T extends AccessControlEntity> T deserialize(ChangeEvent<String, String> changeEvent) {
    String collectionName = getCollectionName(changeEvent.destination());
    Deserializer<? extends AccessControlEntity> deserializer = collectionToDeserializerMap.get(collectionName);
    return (T) deserializer.deserialize(null, getValue(changeEvent));
  }

  private byte[] getValue(ChangeEvent<String, String> changeEvent) {
    return changeEvent.value() == null ? null : changeEvent.value().getBytes();
  }

  private OpType getOperationType(SourceRecord sourceRecord) {
    return Optional.ofNullable(sourceRecord.headers().lastWithName(OP_FIELD))
        .flatMap(x -> OpType.fromString((String) x.value()))
        .orElseThrow(() -> new IllegalArgumentException("Unsupported operation type "));
  }

  private String getCollectionName(String sourceRecordTopic) {
    return Optional.of(sourceRecordTopic.split("\\."))
        .filter(x -> x.length >= 2)
        .map(x -> x[2])
        .orElseThrow(() -> new IllegalArgumentException("Unknown collection name: " + sourceRecordTopic));
  }
}
