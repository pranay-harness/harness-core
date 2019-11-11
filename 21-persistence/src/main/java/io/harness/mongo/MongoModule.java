package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.IndexManager.ensureIndex;
import static io.harness.mongo.IndexManager.updateMovedClasses;
import static org.mongodb.morphia.logging.MorphiaLoggerFactory.registerLogger;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import io.harness.govern.DependencyModule;
import io.harness.govern.DependencyProviderModule;
import io.harness.logging.MorphiaLoggerFactory;
import io.harness.morphia.MorphiaModule;
import io.harness.serializer.KryoModule;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;

import java.util.Set;

@Slf4j
public class MongoModule extends DependencyProviderModule {
  public static final MongoClientOptions mongoClientOptions = MongoClientOptions.builder()
                                                                  .retryWrites(true)
                                                                  .connectTimeout(30000)
                                                                  .serverSelectionTimeout(90000)
                                                                  .maxConnectionIdleTime(600000)
                                                                  .connectionsPerHost(300)
                                                                  .build();

  public static AdvancedDatastore createDatastore(Morphia morphia, String uri) {
    MongoClientURI clientUri = new MongoClientURI(uri, MongoClientOptions.builder(mongoClientOptions));
    MongoClient mongoClient = new MongoClient(clientUri);

    AdvancedDatastore datastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, clientUri.getDatabase());
    datastore.setQueryFactory(new QueryFactory());

    return datastore;
  }

  public MongoModule() {
    registerLogger(MorphiaLoggerFactory.class);
  }

  @Provides
  @Singleton
  public HObjectFactory objectFactory() {
    return new HObjectFactory();
  }

  @Provides
  @Singleton
  public Morphia morphia(@Named("morphiaClasses") Set<Class> classes, HObjectFactory objectFactory) {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(objectFactory);
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.map(classes);
    return morphia;
  }

  @Provides
  @Named("primaryDatastore")
  public AdvancedDatastore primaryDatastore(MongoConfig mongoConfig, Morphia morphia, HObjectFactory objectFactory) {
    MongoClientURI uri = new MongoClientURI(mongoConfig.getUri(), MongoClientOptions.builder(mongoClientOptions));
    MongoClient mongoClient = new MongoClient(uri);

    AdvancedDatastore primaryDatastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, uri.getDatabase());
    primaryDatastore.setQueryFactory(new QueryFactory());

    ensureIndex(primaryDatastore, morphia);
    updateMovedClasses(primaryDatastore, objectFactory.getMorphiaInterfaceImplementers());

    objectFactory.setDatastore(primaryDatastore);

    return primaryDatastore;
  }

  @Provides
  @Singleton
  public DistributedLockSvc distributedLockSvc(MongoConfig mongoConfig) {
    MongoClientURI uri;
    if (isNotEmpty(mongoConfig.getLocksUri())) {
      uri = new MongoClientURI(mongoConfig.getLocksUri(), MongoClientOptions.builder(mongoClientOptions));
    } else {
      uri = new MongoClientURI(mongoConfig.getUri(), MongoClientOptions.builder(mongoClientOptions));
    }
    MongoClient mongoClient = new MongoClient(uri);

    DistributedLockSvcOptions distributedLockSvcOptions =
        new DistributedLockSvcOptions(mongoClient, uri.getDatabase(), "locks");
    distributedLockSvcOptions.setEnableHistory(false);
    return new DistributedLockSvcFactory(distributedLockSvcOptions).getLockSvc();
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(MorphiaModule.getInstance(), KryoModule.getInstance());
  }
}
