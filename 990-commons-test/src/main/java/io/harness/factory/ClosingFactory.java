/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.factory;

import java.io.Closeable;
import java.util.Iterator;
import java.util.LinkedList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClosingFactory implements AutoCloseable {
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
        log.error("Error when closing", exception);
      }
    }
    servers.clear();
  }

  @Override
  public void close() throws Exception {
    stopServers();
  }
}
