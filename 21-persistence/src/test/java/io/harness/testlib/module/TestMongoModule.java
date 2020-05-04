package io.harness.testlib.module;

import static io.harness.govern.Switch.unhandled;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.MongoClient;
import io.harness.factory.ClosingFactory;
import io.harness.govern.DependencyModule;
import io.harness.govern.DependencyProviderModule;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.QueryFactory;
import io.harness.morphia.MorphiaModule;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ObjectFactory;

import java.util.Set;

@Slf4j
public class TestMongoModule extends DependencyProviderModule implements MongoRuleMixin {
  @Provides
  @Named("databaseName")
  @Singleton
  String databaseNameProvider() {
    return databaseName();
  }

  @Provides
  @Named("locksDatabase")
  @Singleton
  String databaseNameProvider(@Named("databaseName") String databaseName) {
    return databaseName;
  }

  @Provides
  @Singleton
  public ObjectFactory objectFactory() {
    return new HObjectFactory();
  }

  @Provides
  @Named("realMongoClient")
  @Singleton
  public MongoClient realMongoClientProvider(ClosingFactory closingFactory) throws Exception {
    return realMongoClient(closingFactory);
  }

  @Provides
  @Named("fakeMongoClient")
  @Singleton
  public MongoClient fakeMongoClientProvider(ClosingFactory closingFactory) throws Exception {
    return fakeMongoClient(closingFactory);
  }

  @Provides
  @Named("locksMongoClient")
  @Singleton
  public MongoClient locksMongoClient(@Named("realMongoClient") MongoClient mongoClient) throws Exception {
    return mongoClient;
  }

  @Provides
  @Named("primaryDatastore")
  @Singleton
  AdvancedDatastore datastore(@Named("databaseName") String databaseName, MongoType type,
      @Named("realMongoClient") Provider<MongoClient> realMongoClient,
      @Named("fakeMongoClient") Provider<MongoClient> fakeMongoClient, Morphia morphia, ObjectFactory objectFactory) {
    MongoClient mongoClient = null;
    switch (type) {
      case REAL:
        mongoClient = realMongoClient.get();
        break;
      case FAKE:
        mongoClient = fakeMongoClient.get();
        break;
      default:
        unhandled(type);
    }

    AdvancedDatastore datastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, databaseName);
    datastore.setQueryFactory(new QueryFactory());
    ((HObjectFactory) objectFactory).setDatastore(datastore);
    return datastore;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(MorphiaModule.getInstance());
  }
}
