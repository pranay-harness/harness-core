package io.harness.aggregator;

import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.aggregator.services.HACLAggregatorServiceImpl;
import io.harness.aggregator.services.apis.ACLAggregatorService;
import io.harness.resourcegroupclient.remote.ResourceGroupClient;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AggregatorModule extends AbstractModule {
  public static final String MONGO_DB_CONNECTOR = "io.debezium.connector.mongodb.MongoDbConnector";
  private static AggregatorModule instance;
  private final AggregatorConfiguration configuration;
  private final ExecutorService executorService;

  public AggregatorModule(AggregatorConfiguration configuration, ExecutorService executorService) {
    this.configuration = configuration;
    this.executorService = executorService;
  }

  public static synchronized AggregatorModule getInstance(
      AggregatorConfiguration aggregatorConfiguration, ExecutorService executorService) {
    if (instance == null) {
      instance = new AggregatorModule(aggregatorConfiguration, executorService);
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(ACLAggregatorService.class).to(HACLAggregatorServiceImpl.class).in(Scopes.SINGLETON);
    registerRequiredBindings();

    HMongoChangeConsumer hMongoChangeConsumer = new HMongoChangeConsumer();
    DebeziumEngine<ChangeEvent<String, String>> debeziumEngine = getEngine(hMongoChangeConsumer);
    executorService.submit(debeziumEngine);
  }

  private DebeziumEngine<ChangeEvent<String, String>> getEngine(
      DebeziumEngine.ChangeConsumer<ChangeEvent<String, String>> changeConsumer) {
    DebeziumConfig debeziumConfig = configuration.getDebeziumConfig();
    Properties props = new Properties();
    props.setProperty("name", debeziumConfig.getConnectorName());
    props.setProperty("offset.storage", MongoOffsetBackingStore.class.getName());
    props.setProperty("offset.storage.file.filename", debeziumConfig.getOffsetStorageFileName());
    props.setProperty("key.converter.schemas.enable", debeziumConfig.getKeyConverterSchemasEnable());
    props.setProperty("value.converter.schemas.enable", debeziumConfig.getValueConverterSchemasEnable());
    props.setProperty("offset.flush.interval.ms", debeziumConfig.getOffsetFlushIntervalMillis());

    /* begin connector properties */
    props.setProperty("connector.class", MONGO_DB_CONNECTOR);
    props.setProperty("mongodb.hosts", debeziumConfig.getMongodbHosts());
    props.setProperty("mongodb.name", debeziumConfig.getMongodbName());
    Optional.ofNullable(debeziumConfig.getMongodbUser())
        .filter(x -> !x.isEmpty())
        .ifPresent(x -> props.setProperty("mongodb.user", x));
    Optional.ofNullable(debeziumConfig.getMongodbPassword())
        .filter(x -> !x.isEmpty())
        .ifPresent(x -> props.setProperty("mongodb.password", x));
    props.setProperty("mongodb.ssl.enabled", debeziumConfig.getSslEnabled());
    props.setProperty("database.include.list", debeziumConfig.getDatabaseIncludeList());
    props.setProperty("collection.include.list", debeziumConfig.getCollectionIncludeList());

    // Create the engine with this configuration and return
    return DebeziumEngine.create(Json.class).using(props).notifying(changeConsumer).build();
  }

  private void registerRequiredBindings() {
    requireBinding(RoleService.class);
    requireBinding(RoleAssignmentService.class);
    requireBinding(ACLService.class);
    requireBinding(ResourceGroupClient.class);
  }
}
