package io.harness.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

public class ClosingFactory {
  private static final Logger logger = LoggerFactory.getLogger(ClosingFactory.class);

  private List<Closeable> servers = new ArrayList<>();

  public synchronized void addServer(Closeable server) {
    servers.add(server);
  }

  public synchronized void stopServers() {
    for (AutoCloseable server : servers) {
      try {
        server.close();
      } catch (Exception exception) {
        logger.error("Error when closing", exception);
      }
    }
    servers.clear();
  }
}
