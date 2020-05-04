package io.harness.testlib.module;

import static io.harness.govern.Switch.unhandled;
import static java.time.Duration.ofMillis;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.ExtractedArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.runtime.Network;
import io.harness.factory.ClosingFactory;
import io.harness.govern.DependencyModule;
import io.harness.govern.DependencyProviderModule;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.QueryFactory;
import io.harness.morphia.MorphiaModule;
import io.harness.testlib.RealMongo;
import io.harness.threading.Morpheus;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ObjectFactory;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

@Slf4j
public class TestMongoModule extends DependencyProviderModule implements MongoRuleMixin {
  IRuntimeConfig runtimeConfig =
      new RuntimeConfigBuilder()
          .defaultsWithLogger(Command.MongoD, LoggerFactory.getLogger(RealMongo.class))
          .artifactStore(new ExtractedArtifactStoreBuilder()
                             .defaults(Command.MongoD)
                             .download(new DownloadConfigBuilder()
                                           .defaultsForCommand(Command.MongoD)
                                           .downloadPath("https://storage.googleapis.com/harness-tests/")
                                           .build()))
          .build();

  MongodStarter starter = MongodStarter.getInstance(runtimeConfig);

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
  public MongoClient realMongoClient(ClosingFactory closingFactory) throws Exception {
    Exception persistent = null;

    // FreeServerPort releases the port before it returns it. This creates a race between the moment it is obtain
    // again and reserved for mongo. In rare cases this can cause the function to fail with port already in use
    // exception.
    //
    // There is no good way to eliminate the race, since the port must be free mongo to be able to grab it.
    //
    // Lets retry a number of times to reduce the likelihood almost to zero.
    for (int i = 0; i < 20; i++) {
      int port = Network.getFreeServerPort();
      IMongodConfig mongodConfig = new MongodConfigBuilder()
                                       .version(Version.Main.V3_6)
                                       .net(new Net("127.0.0.1", port, Network.localhostIsIPv6()))
                                       .build();
      try {
        // It seems that the starter is not thread safe. We still have a likelihood for multiprocessor problems
        // but lets at least do what is cheap to have.
        synchronized (starter) {
          MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
          mongodExecutable.start();
          closingFactory.addServer(new Closeable() {
            @Override
            public void close() throws IOException {
              mongodExecutable.stop();
            }
          });
        }
        final MongoClient mongoClient = new MongoClient("localhost", port);
        closingFactory.addServer(mongoClient);
        return mongoClient;
      } catch (Exception e) {
        // Note this handles race int the port, but also in the starter prepare
        Morpheus.sleep(ofMillis(250));
        persistent = e;
      }
    }

    throw persistent;
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
