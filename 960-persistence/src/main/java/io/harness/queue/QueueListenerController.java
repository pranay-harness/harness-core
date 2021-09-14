/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queue;

import io.harness.config.WorkersConfiguration;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class QueueListenerController implements Managed {
  private ExecutorService executorService =
      Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("queue-listener-%d").build());
  private List<QueueListener<?>> abstractQueueListeners = new ArrayList<>();
  @Inject private WorkersConfiguration workersConfiguration;
  public void register(QueueListener<?> listener, int threads) {
    if (!workersConfiguration.confirmWorkerIsActive(listener.getClass())) {
      log.info("Not initializing QueueListener: [{}], worker has been configured as inactive", listener.getClass());
      return;
    }
    IntStream.rangeClosed(1, threads).forEach(value -> {
      abstractQueueListeners.add(listener);
      executorService.submit(listener);
    });
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#start()
   */
  @Override
  public void start() throws Exception {
    // Do nothing
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#stop()
   */
  @Override
  public void stop() throws Exception {
    abstractQueueListeners.forEach(QueueListener::shutDown);
    executorService.shutdownNow();
    executorService.awaitTermination(1, TimeUnit.HOURS);
  }
}
