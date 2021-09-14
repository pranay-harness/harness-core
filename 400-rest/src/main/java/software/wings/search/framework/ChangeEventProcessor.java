/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.changestreams.ChangeEvent;

import software.wings.dl.WingsPersistence;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
class ChangeEventProcessor {
  @Inject private Set<SearchEntity<?>> searchEntities;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ChangeEventMetricsTracker changeEventMetricsTracker;
  private BlockingQueue<ChangeEvent<?>> changeEventQueue = new LinkedBlockingQueue<>(1000);
  private ExecutorService changeEventExecutorService =
      Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("primary-change-processor").build());
  private Future<?> changeEventProcessorTaskFuture;

  void startProcessingChangeEvents() {
    ChangeEventProcessorTask changeEventProcessorTask =
        new ChangeEventProcessorTask(searchEntities, wingsPersistence, changeEventMetricsTracker, changeEventQueue);
    changeEventProcessorTaskFuture = changeEventExecutorService.submit(changeEventProcessorTask);
  }

  boolean processChangeEvent(ChangeEvent<?> changeEvent) {
    try {
      log.info(
          "Adding change event of type {}:{} in the queue", changeEvent.getEntityType(), changeEvent.getChangeType());
      changeEventQueue.put(changeEvent);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted while waiting to add a change event in the queue", e.getCause());
      return false;
    }
    return true;
  }

  boolean isAlive() {
    return !changeEventProcessorTaskFuture.isDone();
  }

  void shutdown() {
    changeEventProcessorTaskFuture.cancel(true);
    changeEventExecutorService.shutdownNow();
  }
}
