package io.harness.aggregator;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.AccessControlEntity;
import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDBO;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.aggregator.consumers.AccessControlDebeziumChangeConsumer;
import io.harness.aggregator.consumers.ChangeConsumer;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import io.debezium.serde.DebeziumSerdes;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;

@OwnedBy(PL)
@Singleton
public class AggregatorApplication {
  private final ChangeConsumer<RoleDBO> roleChangeConsumer;
  private final ChangeConsumer<RoleAssignmentDBO> roleAssignmentChangeConsumer;
  private final ChangeConsumer<ResourceGroupDBO> resourceGroupChangeConsumer;
  private final ChangeConsumer<UserGroupDBO> userGroupChangeConsumer;
  private final AggregatorConfiguration aggregatorConfiguration;
  private final ExecutorService executorService;
  private static final String MONGO_DB_CONNECTOR = "io.debezium.connector.mongodb.MongoDbConnector";
  private static final String CONNECTOR_NAME = "name";
  private static final String OFFSET_STORAGE = "offset.storage";
  private static final String OFFSET_STORAGE_FILE_FILENAME = "offset.storage.file.filename";
  private static final String KEY_CONVERTER_SCHEMAS_ENABLE = "key.converter.schemas.enable";
  private static final String VALUE_CONVERTER_SCHEMAS_ENABLE = "value.converter.schemas.enable";
  private static final String OFFSET_FLUSH_INTERVAL_MS = "offset.flush.interval.ms";
  private static final String CONNECTOR_CLASS = "connector.class";
  private static final String MONGODB_HOSTS = "mongodb.hosts";
  private static final String MONGODB_NAME = "mongodb.name";
  private static final String MONGODB_USER = "mongodb.user";
  private static final String MONGODB_PASSWORD = "mongodb.password";
  private static final String MONGODB_SSL_ENABLED = "mongodb.ssl.enabled";
  private static final String DATABASE_INCLUDE_LIST = "database.include.list";
  private static final String COLLECTION_INCLUDE_LIST = "collection.include.list";
  private static final String TRANSFORMS = "transforms";
  private static final String TRANSFORMS_UNWRAP_TYPE = "transforms.unwrap.type";
  private static final String TRANSFORMS_UNWRAP_DROP_TOMBSTONES = "transforms.unwrap.drop.tombstones";
  private static final String TRANSFORMS_UNWRAP_ADD_HEADERS = "transforms.unwrap.add.headers";
  private static final String DEBEZIUM_CONNECTOR_MONGODB_TRANSFORMS_EXTRACT_NEW_DOCUMENT_STATE =
      "io.debezium.connector.mongodb.transforms.ExtractNewDocumentState";
  private static final String ROLE_ASSIGNMENTS = "roleassignments";
  private static final String ROLES = "roles";
  private static final String RESOURCE_GROUPS = "resourcegroups";
  private static final String USER_GROUPS = "usergroups";
  private static final String UNKNOWN_PROPERTIES_IGNORED = "unknown.properties.ignored";

  @Inject
  public AggregatorApplication(ChangeConsumer<RoleDBO> roleChangeConsumer,
      ChangeConsumer<RoleAssignmentDBO> roleAssignmentChangeConsumer,
      ChangeConsumer<ResourceGroupDBO> resourceGroupChangeConsumer,
      ChangeConsumer<UserGroupDBO> userGroupChangeConsumer, AggregatorConfiguration aggregatorConfiguration) {
    this.roleChangeConsumer = roleChangeConsumer;
    this.roleAssignmentChangeConsumer = roleAssignmentChangeConsumer;
    this.resourceGroupChangeConsumer = resourceGroupChangeConsumer;
    this.userGroupChangeConsumer = userGroupChangeConsumer;
    this.aggregatorConfiguration = aggregatorConfiguration;
    this.executorService = Executors.newFixedThreadPool(5);
  }

  public void run() {
    Map<String, ChangeConsumer<? extends AccessControlEntity>> collectionToConsumerMap = new HashMap<>();
    Map<String, Deserializer<? extends AccessControlEntity>> collectionToDeserializerMap = new HashMap<>();

    collectionToConsumerMap.put(ROLE_ASSIGNMENTS, roleAssignmentChangeConsumer);
    collectionToConsumerMap.put(ROLES, roleChangeConsumer);
    collectionToConsumerMap.put(RESOURCE_GROUPS, resourceGroupChangeConsumer);
    collectionToConsumerMap.put(USER_GROUPS, userGroupChangeConsumer);

    // configuring id deserializer
    Serde<String> idSerde = DebeziumSerdes.payloadJson(String.class);
    idSerde.configure(Maps.newHashMap(ImmutableMap.of("from.field", "id")), true);
    Deserializer<String> idDeserializer = idSerde.deserializer();

    Map<String, String> valueDeserializerConfig = Maps.newHashMap(ImmutableMap.of(UNKNOWN_PROPERTIES_IGNORED, "true"));

    // configuring role assignment deserializer
    Serde<RoleAssignmentDBO> roleAssignmentSerde = DebeziumSerdes.payloadJson(RoleAssignmentDBO.class);
    roleAssignmentSerde.configure(valueDeserializerConfig, false);
    collectionToDeserializerMap.put(ROLE_ASSIGNMENTS, roleAssignmentSerde.deserializer());

    // configuring role deserializer
    Serde<RoleDBO> roleSerde = DebeziumSerdes.payloadJson(RoleDBO.class);
    roleSerde.configure(valueDeserializerConfig, false);
    collectionToDeserializerMap.put(ROLES, roleSerde.deserializer());

    // configuring resource group deserializer
    Serde<ResourceGroupDBO> resourceGroupSerde = DebeziumSerdes.payloadJson(ResourceGroupDBO.class);
    resourceGroupSerde.configure(valueDeserializerConfig, false);
    collectionToDeserializerMap.put(RESOURCE_GROUPS, resourceGroupSerde.deserializer());

    // configuring resource group deserializer
    Serde<UserGroupDBO> userGroupSerde = DebeziumSerdes.payloadJson(UserGroupDBO.class);
    userGroupSerde.configure(valueDeserializerConfig, false);
    collectionToDeserializerMap.put(USER_GROUPS, userGroupSerde.deserializer());

    AccessControlDebeziumChangeConsumer accessControlDebeziumChangeConsumer =
        new AccessControlDebeziumChangeConsumer(idDeserializer, collectionToDeserializerMap, collectionToConsumerMap);

    // configuring debezium
    DebeziumEngine<ChangeEvent<String, String>> debeziumEngine =
        getEngine(aggregatorConfiguration.getDebeziumConfig(), accessControlDebeziumChangeConsumer);
    executorService.submit(debeziumEngine);
  }

  private static DebeziumEngine<ChangeEvent<String, String>> getEngine(
      DebeziumConfig debeziumConfig, AccessControlDebeziumChangeConsumer changeConsumer) {
    Properties props = new Properties();
    props.setProperty(CONNECTOR_NAME, debeziumConfig.getConnectorName());
    props.setProperty(OFFSET_STORAGE, MongoOffsetBackingStore.class.getName());
    props.setProperty(OFFSET_STORAGE_FILE_FILENAME, debeziumConfig.getOffsetStorageFileName());
    props.setProperty(KEY_CONVERTER_SCHEMAS_ENABLE, debeziumConfig.getKeyConverterSchemasEnable());
    props.setProperty(VALUE_CONVERTER_SCHEMAS_ENABLE, debeziumConfig.getValueConverterSchemasEnable());
    props.setProperty(OFFSET_FLUSH_INTERVAL_MS, debeziumConfig.getOffsetFlushIntervalMillis());

    /* begin connector properties */
    props.setProperty(CONNECTOR_CLASS, MONGO_DB_CONNECTOR);
    props.setProperty(MONGODB_HOSTS, debeziumConfig.getMongodbHosts());
    props.setProperty(MONGODB_NAME, debeziumConfig.getMongodbName());
    Optional.ofNullable(debeziumConfig.getMongodbUser())
        .filter(x -> !x.isEmpty())
        .ifPresent(x -> props.setProperty(MONGODB_USER, x));
    Optional.ofNullable(debeziumConfig.getMongodbPassword())
        .filter(x -> !x.isEmpty())
        .ifPresent(x -> props.setProperty(MONGODB_PASSWORD, x));
    props.setProperty(MONGODB_SSL_ENABLED, debeziumConfig.getSslEnabled());
    props.setProperty(DATABASE_INCLUDE_LIST, debeziumConfig.getDatabaseIncludeList());
    props.setProperty(COLLECTION_INCLUDE_LIST, debeziumConfig.getCollectionIncludeList());
    props.setProperty(TRANSFORMS, "unwrap");
    props.setProperty(TRANSFORMS_UNWRAP_TYPE, DEBEZIUM_CONNECTOR_MONGODB_TRANSFORMS_EXTRACT_NEW_DOCUMENT_STATE);
    props.setProperty(TRANSFORMS_UNWRAP_DROP_TOMBSTONES, "false");
    props.setProperty(TRANSFORMS_UNWRAP_ADD_HEADERS, "op");

    return DebeziumEngine.create(Json.class).using(props).notifying(changeConsumer).build();
  }
}
