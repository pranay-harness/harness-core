package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.mongodb.morphia.logging.MorphiaLoggerFactory.registerLogger;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import io.harness.govern.DependencyModule;
import io.harness.logging.MorphiaLoggerFactory;
import io.harness.mongo.index.migrator.Migrator;
import io.harness.morphia.MorphiaModule;
import io.harness.serializer.KryoModule;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ObjectFactory;

import java.util.Set;

@Slf4j
public class MongoModule extends DependencyModule {
  public static final MongoClientOptions defaultMongoClientOptions = MongoClientOptions.builder()
                                                                         .retryWrites(true)
                                                                         .connectTimeout(30000)
                                                                         .serverSelectionTimeout(90000)
                                                                         .maxConnectionIdleTime(600000)
                                                                         .connectionsPerHost(300)
                                                                         .build();

  public static AdvancedDatastore createDatastore(Morphia morphia, String uri) {
    MongoClientURI clientUri = new MongoClientURI(uri, MongoClientOptions.builder(defaultMongoClientOptions));
    MongoClient mongoClient = new MongoClient(clientUri);

    AdvancedDatastore datastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, clientUri.getDatabase());
    datastore.setQueryFactory(new QueryFactory());

    return datastore;
  }

  public MongoModule() {
    try {
      registerLogger(MorphiaLoggerFactory.class);
    } catch (Exception e) {
      // happens when MorphiaLoggerFactory.get has already been called.
      logger.warn("Failed to register logger", e);
    }
  }

  @Override
  protected void configure() {
    MapBinder.newMapBinder(binder(), String.class, Migrator.class);
  }

  @Provides
  @Singleton
  public ObjectFactory objectFactory() {
    return new HObjectFactory();
  }

  @Provides
  @Named("primaryDatastore")
  @Singleton
  public AdvancedDatastore primaryDatastore(
      MongoConfig mongoConfig, Morphia morphia, ObjectFactory objectFactory, IndexManager indexManager) {
    MongoClientOptions primaryMongoClientOptions = MongoClientOptions.builder()
                                                       .retryWrites(defaultMongoClientOptions.getRetryWrites())
                                                       .connectTimeout(mongoConfig.getConnectTimeout())
                                                       .serverSelectionTimeout(mongoConfig.getServerSelectionTimeout())
                                                       .maxConnectionIdleTime(mongoConfig.getMaxConnectionIdleTime())
                                                       .connectionsPerHost(mongoConfig.getConnectionsPerHost())
                                                       .readPreference(mongoConfig.getReadPreference())
                                                       .build();
    MongoClientURI uri =
        new MongoClientURI(mongoConfig.getUri(), MongoClientOptions.builder(primaryMongoClientOptions));
    MongoClient mongoClient = new MongoClient(uri);

    AdvancedDatastore primaryDatastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, uri.getDatabase());
    primaryDatastore.setQueryFactory(new QueryFactory());

    indexManager.ensureIndexes(mongoConfig.getIndexManagerMode(), primaryDatastore, morphia);

    HObjectFactory hObjectFactory = (HObjectFactory) objectFactory;

    ClassRefactoringManager.updateMovedClasses(primaryDatastore, hObjectFactory.getMorphiaInterfaceImplementers());
    hObjectFactory.setDatastore(primaryDatastore);

    return primaryDatastore;
  }

  @Provides
  @Named("locksMongoClient")
  @Singleton
  public MongoClient getLocksMongoClient(MongoConfig mongoConfig) {
    MongoClientURI uri;
    if (isNotEmpty(mongoConfig.getLocksUri())) {
      uri = new MongoClientURI(mongoConfig.getLocksUri(), MongoClientOptions.builder(defaultMongoClientOptions));
    } else {
      uri = new MongoClientURI(mongoConfig.getUri(), MongoClientOptions.builder(defaultMongoClientOptions));
    }
    return new MongoClient(uri);
  }

  @Provides
  @Named("locksDatabase")
  @Singleton
  public String getLocksDatabase(MongoConfig mongoConfig) {
    MongoClientURI uri;
    if (isNotEmpty(mongoConfig.getLocksUri())) {
      uri = new MongoClientURI(mongoConfig.getLocksUri(), MongoClientOptions.builder(defaultMongoClientOptions));
    } else {
      uri = new MongoClientURI(mongoConfig.getUri(), MongoClientOptions.builder(defaultMongoClientOptions));
    }
    return uri.getDatabase();
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(MorphiaModule.getInstance(), KryoModule.getInstance());
  }
}
