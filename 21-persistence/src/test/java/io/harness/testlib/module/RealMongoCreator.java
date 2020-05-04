package io.harness.testlib.module;

import static java.time.Duration.ofMillis;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

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
import io.harness.exception.GeneralException;
import io.harness.threading.Morpheus;
import lombok.Builder;
import lombok.Value;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class RealMongoCreator {
  private static ExecutorService executorService =
      Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("RealMongoCreator-%d").build());

  private static IRuntimeConfig runtimeConfig =
      new RuntimeConfigBuilder()
          .defaultsWithLogger(Command.MongoD, LoggerFactory.getLogger(io.harness.testlib.RealMongo.class))
          .artifactStore(new ExtractedArtifactStoreBuilder()
                             .defaults(Command.MongoD)
                             .download(new DownloadConfigBuilder()
                                           .defaultsForCommand(Command.MongoD)
                                           .downloadPath("https://storage.googleapis.com/harness-tests/")
                                           .build()))
          .build();

  private static MongodStarter starter = MongodStarter.getInstance(runtimeConfig);

  @Value
  @Builder
  static class RealMongo implements Closeable {
    MongodExecutable mongodExecutable;
    MongoClient mongoClient;

    @Override
    public void close() {
      executorService.submit(() -> {
        mongoClient.close();
        mongodExecutable.stop();
      });
    }
  }

  private static RealMongo realMongo() throws Exception {
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
        MongodExecutable mongodExecutable;
        synchronized (starter) {
          mongodExecutable = starter.prepare(mongodConfig);
          mongodExecutable.start();
        }
        MongoClient mongoClient = new MongoClient("localhost", port);
        return RealMongo.builder().mongodExecutable(mongodExecutable).mongoClient(mongoClient).build();
      } catch (Exception e) {
        // Note this handles race int the port, but also in the starter prepare
        Morpheus.sleep(ofMillis(250));
        persistent = e;
      }
    }
    throw persistent;
  }

  static RealMongo takeRealMongo() {
    try {
      return realMongo();
    } catch (Exception exception) {
      throw new GeneralException("", exception);
    }
  }
}
