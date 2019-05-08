package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.IndexManager.ensureIndex;
import static org.mongodb.morphia.logging.MorphiaLoggerFactory.registerLogger;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import io.harness.logging.MorphiaLoggerFactory;
import io.harness.persistence.ReadPref;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;

import java.util.Set;

@Slf4j
public class MongoModule extends AbstractModule {
  private Morphia morphia;
  private AdvancedDatastore primaryDatastore;
  private AdvancedDatastore secondaryDatastore;
  private DistributedLockSvc distributedLockSvc;

  public static final MongoClientOptions mongoClientOptions =
      MongoClientOptions.builder()
          .retryWrites(true)
          // TODO: Using secondaryPreferred creates issues that need to be investigated
          //.readPreference(ReadPreference.secondaryPreferred())
          .connectTimeout(30000)
          .serverSelectionTimeout(90000)
          .maxConnectionIdleTime(600000)
          .connectionsPerHost(300)
          .build();

  public static AdvancedDatastore createDatastore(
      Morphia morphia, String uri, Set<Class> collectionClasses, ReadPref readPref) {
    MongoClientURI clientUri = new MongoClientURI(uri, MongoClientOptions.builder(mongoClientOptions));
    MongoClient mongoClient = new MongoClient(clientUri);

    AdvancedDatastore datastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, clientUri.getDatabase());
    datastore.setQueryFactory(new QueryFactory());

    return datastore;
  }

  public MongoModule(MongoConfig mongoConfig, Set<Class> collectionClasses) {
    registerLogger(MorphiaLoggerFactory.class);

    morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.map(collectionClasses);

    MongoClientURI uri = new MongoClientURI(mongoConfig.getUri(), MongoClientOptions.builder(mongoClientOptions));
    MongoClient mongoClient = new MongoClient(uri);

    primaryDatastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, uri.getDatabase());
    primaryDatastore.setQueryFactory(new QueryFactory());

    MongoClientURI locksUri = uri;
    MongoClient mongoLocksClient = mongoClient;
    if (isNotEmpty(mongoConfig.getLocksUri())) {
      locksUri = new MongoClientURI(mongoConfig.getLocksUri(), MongoClientOptions.builder(mongoClientOptions));
      mongoLocksClient = new MongoClient(locksUri);
    }

    DistributedLockSvcOptions distributedLockSvcOptions =
        new DistributedLockSvcOptions(mongoLocksClient, locksUri.getDatabase(), "locks");
    distributedLockSvcOptions.setEnableHistory(false);
    distributedLockSvc = new DistributedLockSvcFactory(distributedLockSvcOptions).getLockSvc();

    if (uri.getHosts().size() > 1) {
      secondaryDatastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, uri.getDatabase());
      secondaryDatastore.setQueryFactory(new QueryFactory());
    } else {
      secondaryDatastore = primaryDatastore;
    }

    ensureIndex(primaryDatastore, morphia);
  }

  @Provides
  public Morphia morphia() {
    return morphia;
  }

  @Provides
  @Named("primaryDatastore")
  public AdvancedDatastore primaryDatastore() {
    return primaryDatastore;
  }

  @Provides
  @Named("secondaryDatastore")
  public AdvancedDatastore secondaryDatastore() {
    return secondaryDatastore;
  }

  @Provides
  public DistributedLockSvc distributedLockSvc() {
    return distributedLockSvc;
  }

  @Override
  protected void configure() {
    // Dummy kryo initialization trigger to make sure it is in good condition
    KryoUtils.asBytes(1);
  }
}
