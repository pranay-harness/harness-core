package io.harness.rule;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.harness.factory.ClosingFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

public interface MongoRuleMixin {
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

  default MongoClient fakeMongoClient(int port, ClosingFactory closingFactory) {
    final MongoServer mongoServer = new MongoServer(new MemoryBackend());
    closingFactory.addServer(new Closeable() {
      @Override
      public void close() throws IOException {
        mongoServer.shutdownNow();
      }
    });

    mongoServer.bind("localhost", port);
    InetSocketAddress serverAddress = mongoServer.getLocalAddress();
    MongoClient client = new MongoClient(new ServerAddress(serverAddress));

    closingFactory.addServer(client);
    return client;
  }
}
