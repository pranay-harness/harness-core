package io.harness.factory;

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.Iterator;
import java.util.LinkedList;

@Slf4j
public class ClosingFactory {
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
