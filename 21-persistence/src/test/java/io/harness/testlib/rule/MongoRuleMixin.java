package io.harness.testlib.rule;

import static java.time.Duration.ofMillis;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.DownloadConfigBuilder;
import de.flapdoodle.embed.mongo.config.ExtractedArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.Version.Main;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.runtime.Network;
import io.harness.factory.ClosingFactory;
import io.harness.testlib.RealMongo;
import io.harness.threading.Morpheus;
import lombok.Builder;
import lombok.Value;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.util.List;

public interface MongoRuleMixin {
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

  MongoClientOptions.Builder mongoClientOptions = MongoClientOptions.builder()
                                                      .retryWrites(true)
                                                      .connectTimeout(30000)
                                                      .serverSelectionTimeout(90000)
                                                      .maxConnectionIdleTime(600000)
                                                      .connectionsPerHost(300);

  default String databaseName() {
    return System.getProperty("dbName", "harness");
  }

  default MongoClientURI mongoClientUri() {
    return new MongoClientURI(
        System.getProperty("mongoUri", "mongodb://localhost:27017/" + databaseName()), mongoClientOptions);
  }

  default MongoClient fakeMongoClient(ClosingFactory closingFactory) {
    final MongoServer mongoServer = new MongoServer(new MemoryBackend());
    if (closingFactory != null) {
      closingFactory.addServer(new Closeable() {
        @Override
        public void close() throws IOException {
          mongoServer.shutdownNow();
        }
      });
    }

    mongoServer.bind("localhost", 0);
    InetSocketAddress serverAddress = mongoServer.getLocalAddress();
    MongoClient client = new MongoClient(new ServerAddress(serverAddress));

    if (closingFactory != null) {
      closingFactory.addServer(client);
    }
    return client;
  }

  default MongoClient realMongoClient(ClosingFactory closingFactory) throws Exception {
    Exception persistent = null;

    // FreeServerPort releases the port before it returns it. This creates a race between the moment it is obtain again
    // and reserved for mongo. In rare cases this can cause the function to fail with port already in use exception.
    //
    // There is no good way to eliminate the race, since the port must be free mongo to be able to grab it.
    //
    // Lets retry a number of times to reduce the likelihood almost to zero.
    for (int i = 0; i < 20; i++) {
      int port = Network.getFreeServerPort();
      IMongodConfig mongodConfig = new MongodConfigBuilder()
                                       .version(Main.V3_6)
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

  enum MongoType { FAKE, REAL }

  @Value
  @Builder
  class MongoInfo {
    MongoType type;
    MongoClient client;
  }

  default MongoInfo testMongo(List<Annotation> annotations, ClosingFactory closingFactory) throws Exception {
    if (annotations.stream().anyMatch(RealMongo.class ::isInstance)) {
      return MongoInfo.builder().type(MongoType.REAL).client(realMongoClient(closingFactory)).build();
    }
    return MongoInfo.builder().type(MongoType.FAKE).client(fakeMongoClient(closingFactory)).build();
  }
}
