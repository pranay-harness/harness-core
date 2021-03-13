package io.harness.aggregator.consumers;

import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.aggregator.MongoOffsetBackingStore;
import io.harness.aggregator.OpType;
import io.harness.aggregator.services.apis.AggregatorService;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.debezium.embedded.EmbeddedEngineChangeEvent;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.serde.DebeziumSerdes;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@Singleton
@Slf4j
public class AccessControlDebeziumChangeConsumer implements DebeziumEngine.ChangeConsumer<ChangeEvent<String, String>> {
  private static final String ROLE_ASSIGNMENTS = "roleassignments";
  private static final String ROLES = "roles";
  private static final String RESOURCE_GROUPS = "resourcegroups";
  private static final String UNKNOWN_PROPERTIES_IGNORED = "unknown.properties.ignored";
  private static final String OP_FIELD = "__op";
  private final Deserializer<RoleAssignmentDBO> roleAssignmentDeserializer;
  private final Deserializer<RoleDBO> roleDeserializer;
  private final Deserializer<ResourceGroupDBO> resourceGroupDeserializer;
  private final Deserializer<String> idDeserializer;
  private final RetryPolicy<Object> retryPolicy;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private AggregatorService aggregatorService;
  @Inject @Named("roleAssignment") private ChangeConsumer roleAssignmentChangeConsumer;
  @Inject @Named("role") private ChangeConsumer roleChangeConsumer;
  @Inject @Named("resourceGroup") private ChangeConsumer resourceGroupChangeConsumer;

  public AccessControlDebeziumChangeConsumer() {
    // configuring id deserializer
    Serde<String> idSerde = DebeziumSerdes.payloadJson(String.class);
    idSerde.configure(Maps.newHashMap(ImmutableMap.of("from.field", "id")), true);
    idDeserializer = idSerde.deserializer();

    Map<String, String> valueDeserializerConfig = Maps.newHashMap(ImmutableMap.of(UNKNOWN_PROPERTIES_IGNORED, "true"));

    // configuring role assignment deserializer
    Serde<RoleAssignmentDBO> roleAssignmentSerde = DebeziumSerdes.payloadJson(RoleAssignmentDBO.class);
    roleAssignmentSerde.configure(valueDeserializerConfig, false);
    roleAssignmentDeserializer = roleAssignmentSerde.deserializer();

    // configuring role deserializer
    Serde<RoleDBO> roleSerde = DebeziumSerdes.payloadJson(RoleDBO.class);
    roleSerde.configure(valueDeserializerConfig, false);
    roleDeserializer = roleSerde.deserializer();

    // configuring resource group deserializer
    Serde<ResourceGroupDBO> resourceGroupSerde = DebeziumSerdes.payloadJson(ResourceGroupDBO.class);
    roleSerde.configure(valueDeserializerConfig, false);
    resourceGroupDeserializer = resourceGroupSerde.deserializer();

    // configuring retry policy
    retryPolicy = RetryUtils.getRetryPolicy("[Retrying] attempt: {}", "[Failed] attempt: {}",
        ImmutableList.of(Exception.class), Duration.ofSeconds(1), 3, log);
  }

  @SneakyThrows
  @Override
  public void handleBatch(List<ChangeEvent<String, String>> list,
      DebeziumEngine.RecordCommitter<ChangeEvent<String, String>> recordCommitter) {
    for (ChangeEvent<String, String> changeEvent : list) {
      String id = idDeserializer.deserialize(null, changeEvent.key().getBytes());
      Optional<String> collectionNameOptional = getCollectionName(changeEvent.destination());
      Optional<OpType> opTypeOptional =
          getOperationType(((EmbeddedEngineChangeEvent<String, String>) changeEvent).sourceRecord());
      if (!collectionNameOptional.isPresent() || !opTypeOptional.isPresent()) {
        log.error("Unable to get collection name/ operation type from event: {}, ignoring it", changeEvent);
        continue;
      }
      String collectionName = collectionNameOptional.get();
      OpType opType = opTypeOptional.get();
      try {
        switch (collectionName) {
          case ROLE_ASSIGNMENTS:
            RoleAssignmentDBO roleAssignmentDBO = roleAssignmentDeserializer.deserialize(
                null, changeEvent.value() == null ? null : changeEvent.value().getBytes());
            roleAssignmentChangeConsumer.consumeEvent(opType, id, roleAssignmentDBO);
            break;
          case ROLES:
            RoleDBO roleDBO =
                roleDeserializer.deserialize(null, changeEvent.value() == null ? null : changeEvent.value().getBytes());
            roleChangeConsumer.consumeEvent(opType, id, roleDBO);
            break;
          case RESOURCE_GROUPS:
            ResourceGroupDBO resourceGroupDBO = resourceGroupDeserializer.deserialize(
                null, changeEvent.value() == null ? null : changeEvent.value().getBytes());
            resourceGroupChangeConsumer.consumeEvent(opType, id, resourceGroupDBO);
            break;
          default:
            log.info("Unknown collection name, no consumer defined for this collection");
        }
        recordCommitter.markProcessed(changeEvent);
      } catch (Exception exception) {
        log.error(
            "[FATAL ERROR] Exception while processing event: {}, cannot recover from this, preparing to full sync now.",
            changeEvent);
        prepareForFullSync();
      }
    }
    recordCommitter.markBatchFinished();
  }

  private Optional<OpType> getOperationType(SourceRecord sourceRecord) {
    return Optional.ofNullable(sourceRecord.headers().lastWithName(OP_FIELD))
        .flatMap(x -> OpType.fromString((String) x.value()));
  }

  private void prepareForFullSync() {
    mongoTemplate.findAllAndRemove(new Query(), MongoOffsetBackingStore.class);
  }

  private Optional<String> getCollectionName(String sourceRecordTopic) {
    return Optional.of(sourceRecordTopic.split("\\.")).filter(x -> x.length >= 2).map(x -> x[2]);
  }
}
