package io.harness.testlib.module;

import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.harness.factory.ClosingFactory;
import io.harness.govern.ProviderModule;
import io.harness.testlib.RealMongo;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.util.List;

public interface MongoRuleMixin {
  enum MongoType { REAL, FAKE }

  default Module mongoTypeModule(List<Annotation> annotations) {
    return new ProviderModule() {
      @Provides
      @Singleton
      MongoType provideMongoType() {
        return annotations.stream().anyMatch(RealMongo.class ::isInstance) ? MongoType.REAL : MongoType.FAKE;
      }
    };
  }

  default String databaseName() {
    return System.getProperty("dbName", "harness");
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
}
