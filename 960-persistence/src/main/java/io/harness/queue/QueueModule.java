/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
