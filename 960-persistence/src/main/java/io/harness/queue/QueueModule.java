/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queue;

import static java.util.Arrays.asList;

import io.harness.govern.ServersModule;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import java.io.Closeable;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueueModule extends AbstractModule implements ServersModule {
  private static QueueModule instance;

  public static QueueModule getInstance() {
    if (instance == null) {
      instance = new QueueModule();
    }
    return instance;
  }

  @Override
  public List<Closeable> servers(Injector injector) {
    final QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);

    return asList(() -> {
      try {
        queueListenerController.stop();
      } catch (Exception exception) {
        log.error("", exception);
      }
    }, () -> injector.getInstance(TimerScheduledExecutorService.class).shutdownNow());
  }
}
