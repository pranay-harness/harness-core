package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;
import static org.mongodb.morphia.logging.MorphiaLoggerFactory.registerLogger;

import io.harness.exception.UnexpectedException;
import io.harness.logging.MorphiaLoggerFactory;
import io.harness.mongo.index.migrator.Migrator;
import io.harness.morphia.MorphiaModule;
import io.harness.serializer.KryoModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ObjectFactory;

@Slf4j
public class MongoModule extends AbstractModule {
  private static volatile MongoModule instance;

  public static MongoModule getInstance() {
    if (instance == null) {
      instance = new MongoModule();
    }
    return instance;
  }

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

  private MongoModule() {
    try {
      registerLogger(MorphiaLoggerFactory.class);
    } catch (Exception e) {
      // happens when MorphiaLoggerFactory.get has already been called.
      log.warn("Failed to register logger", e);
    }
  }

  @Override
  protected void configure() {
    install(ObjectFactoryModule.getInstance());
    install(MorphiaModule.getInstance());
    install(KryoModule.getInstance());

    MapBinder.newMapBinder(binder(), String.class, Migrator.class);
  }

  @Provides
  @Named("primaryDatastore")
  @Singleton
  public AdvancedDatastore primaryDatastore(MongoConfig mongoConfig, @Named("morphiaClasses") Set<Class> classes,
      @Named("morphiaInterfaceImplementersClasses") Map<String, Class> morphiaInterfaceImplementers, Morphia morphia,
      ObjectFactory objectFactory, IndexManager indexManager) {
    for (Class clazz : classes) {
      if (morphia.getMapper().getMCMap().get(clazz.getName()).getCollectionName().startsWith("!!!custom_")) {
        throw new UnexpectedException(format("The custom collection name for %s is not provided", clazz.getName()));
      }
    }

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

    indexManager.ensureIndexes(mongoConfig.getIndexManagerMode(), primaryDatastore, morphia, null);

    HObjectFactory hObjectFactory = (HObjectFactory) objectFactory;

    ClassRefactoringManager.updateMovedClasses(primaryDatastore, morphiaInterfaceImplementers);
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
}
