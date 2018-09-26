package io.harness.rule;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

import java.util.ArrayList;
import java.util.List;

public class MongoServerFactory {
  private static List<MongoServer> servers = new ArrayList<>();

  public static synchronized MongoServer createMongoServer() {
    final MongoServer mongoServer = new MongoServer(new MemoryBackend());
    servers.add(mongoServer);
    return mongoServer;
  }

  public static synchronized void stopMongoServers() {
    for (MongoServer server : servers) {
      server.shutdownNow();
    }
    servers.clear();
  }
}
