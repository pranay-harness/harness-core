package io.harness.aggregator.consumers;

import io.harness.accesscontrol.AccessControlEntity;
import io.harness.aggregator.OpType;

import com.google.inject.Singleton;
import io.debezium.embedded.EmbeddedEngineChangeEvent;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.connect.source.SourceRecord;

@Singleton
@Slf4j
public class AccessControlDebeziumChangeConsumer implements DebeziumEngine.ChangeConsumer<ChangeEvent<String, String>> {
  private static final String OP_FIELD = "__op";
  private final Deserializer<String> idDeserializer;
  private final Map<String, Deserializer<? extends AccessControlEntity>> collectionToDeserializerMap;
  private final Map<String, ChangeConsumer<? extends AccessControlEntity>> collectionToConsumerMap;

  public AccessControlDebeziumChangeConsumer(Deserializer<String> idDeserializer,
      Map<String, Deserializer<? extends AccessControlEntity>> collectionToDeserializerMap,
      Map<String, ChangeConsumer<? extends AccessControlEntity>> collectionToConsumerMap) {
    this.idDeserializer = idDeserializer;
    this.collectionToDeserializerMap = collectionToDeserializerMap;
    this.collectionToConsumerMap = collectionToConsumerMap;
  }

  private void handleChangeEvent(ChangeEvent<String, String> changeEvent) {
    String id = idDeserializer.deserialize(null, changeEvent.key().getBytes());
    Optional<String> collectionNameOptional = getCollectionName(changeEvent.destination());
    Optional<OpType> opTypeOptional =
        getOperationType(((EmbeddedEngineChangeEvent<String, String>) changeEvent).sourceRecord());
    if (StringUtils.isEmpty(id) || !collectionNameOptional.isPresent() || !opTypeOptional.isPresent()) {
      log.error("Unable to get id / collection name/ operation type from event: {}, ignoring it", changeEvent);
      return;
    }

    String collectionName = collectionNameOptional.get();
    OpType opType = opTypeOptional.get();
    Deserializer<? extends AccessControlEntity> deserializer = collectionToDeserializerMap.get(collectionName);
    Optional.ofNullable(deserializer).ifPresent(des -> {
      ChangeConsumer<? extends AccessControlEntity> changeConsumer = collectionToConsumerMap.get(collectionName);
      log.info("Processing {} event for entity: {} in collection: {}", opType, id, collectionName);
      Optional.ofNullable(changeConsumer)
          .ifPresent(consumer -> consumer.consumeEvent(opType, id, deserialize(changeEvent)));
    });
  }

  @SneakyThrows
  @Override
  public void handleBatch(List<ChangeEvent<String, String>> list,
      DebeziumEngine.RecordCommitter<ChangeEvent<String, String>> recordCommitter) {
    for (ChangeEvent<String, String> changeEvent : list) {
      try {
        handleChangeEvent(changeEvent);
      } catch (Exception ex) {
        log.error("Error occurred while processing change event: {}", changeEvent);
        throw ex;
      }
      recordCommitter.markProcessed(changeEvent);
    }
    recordCommitter.markBatchFinished();
  }

  private <T extends AccessControlEntity> T deserialize(ChangeEvent<String, String> changeEvent) {
    Deserializer<? extends AccessControlEntity> deserializer =
        collectionToDeserializerMap.get(getCollectionName(changeEvent.destination()).orElse(""));
    if (deserializer == null) {
      return null;
    }
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
