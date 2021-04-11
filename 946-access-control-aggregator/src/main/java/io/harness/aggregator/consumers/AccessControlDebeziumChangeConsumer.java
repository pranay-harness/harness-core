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
import org.apache.commons.lang3.StringUtils;
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

  public AccessControlDebeziumChangeConsumer(Deserializer<String> idDeserializer,
      Map<String, Deserializer<? extends AccessControlEntity>> collectionToDeserializerMap,
      Map<String, ChangeConsumer<? extends AccessControlEntity>> collectionToConsumerMap) {
    this.idDeserializer = idDeserializer;
    this.collectionToDeserializerMap = collectionToDeserializerMap;
    this.collectionToConsumerMap = collectionToConsumerMap;
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
    Optional<String> collectionNameOptional = getCollectionName(changeEvent.destination());
    Optional<OpType> opTypeOptional =
        getOperationType(((EmbeddedEngineChangeEvent<String, String>) changeEvent).sourceRecord());
    if (StringUtils.isEmpty(id) || !collectionNameOptional.isPresent() || !opTypeOptional.isPresent()) {
      log.error("Unable to get entity id / collection name / operation type from event: {}, ignoring it", changeEvent);
      return true;
    }

    String collectionName = collectionNameOptional.get();
    OpType opType = opTypeOptional.get();
    Deserializer<? extends AccessControlEntity> deserializer = collectionToDeserializerMap.get(collectionName);

    log.info("Received {} event for entity: {} in collection: {}", opType, id, collectionName);

    Optional.ofNullable(deserializer).ifPresent(des -> {
      ChangeConsumer<? extends AccessControlEntity> changeConsumer = collectionToConsumerMap.get(collectionName);
      Optional.ofNullable(changeConsumer)
          .ifPresent(consumer -> consumer.consumeEvent(opType, id, deserialize(changeEvent)));
    });
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
        log.error("Exception caught when trying to process event: {}", changeEvent, exception);
        log.error("Retrying this event with exponential backoff now...");
        retry.executeSupplier(() -> handleEvent(changeEvent));
      }
      recordCommitter.markProcessed(changeEvent);
    }
    recordCommitter.markBatchFinished();
  }

  private <T extends AccessControlEntity> T deserialize(ChangeEvent<String, String> changeEvent) {
    Optional<String> collectionNameOptional = getCollectionName(changeEvent.destination());
    Deserializer<? extends AccessControlEntity> deserializer =
        collectionToDeserializerMap.get(collectionNameOptional.orElseThrow(
            () -> new IllegalStateException("No deserializer found for collection: " + changeEvent.destination())));
    return (T) deserializer.deserialize(null, getValue(changeEvent));
  }

  private byte[] getValue(ChangeEvent<String, String> changeEvent) {
    return changeEvent.value() == null ? null : changeEvent.value().getBytes();
  }

  private Optional<OpType> getOperationType(SourceRecord sourceRecord) {
    return Optional.ofNullable(sourceRecord.headers().lastWithName(OP_FIELD))
        .flatMap(x -> OpType.fromString((String) x.value()));
  }

  private Optional<String> getCollectionName(String sourceRecordTopic) {
    return Optional.of(sourceRecordTopic.split("\\.")).filter(x -> x.length >= 2).map(x -> x[2]);
  }
}
