package io.harness.rule;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;

import java.net.InetSocketAddress;

public interface DatabaseRuleMixin {
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

  default MongoClient fakeMongoClient(int port) {
    MongoServer mongoServer = MongoServerFactory.createMongoServer();
    mongoServer.bind("localhost", port);
    InetSocketAddress serverAddress = mongoServer.getLocalAddress();
    return new MongoClient(new ServerAddress(serverAddress));
  }
}
