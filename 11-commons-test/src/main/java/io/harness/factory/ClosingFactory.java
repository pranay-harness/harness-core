package io.harness.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Iterator;
import java.util.LinkedList;

public class ClosingFactory {
  private static final Logger logger = LoggerFactory.getLogger(ClosingFactory.class);

  private LinkedList<Closeable> servers = new LinkedList<>();

  public synchronized void addServer(Closeable server) {
    servers.add(server);
  }

  public synchronized void stopServers() {
    for (Iterator<Closeable> closeableIterator = servers.descendingIterator(); closeableIterator.hasNext();) {
      Closeable server = closeableIterator.next();
      try {
        server.close();
      } catch (Exception exception) {
        logger.error("Error when closing", exception);
      }
    }
    servers.clear();
  }
}
