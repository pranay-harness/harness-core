package io.harness;

import io.harness.changestreamsframework.ChangeEvent;
import io.harness.entities.CDCEntity;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;
import software.wings.dl.WingsPersistence;

@Slf4j
class ChangeEventProcessor {
  @Inject private Set<CDCEntity<?>> subscribedClasses;
  @Inject private WingsPersistence wingsPersistence;
  private BlockingQueue<ChangeEvent<?>> changeEventQueue = new LinkedBlockingQueue<>(1000);
  private ExecutorService changeEventExecutorService =
      Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("primary-change-processor").build());
  private Future<?> changeEventProcessorTaskFuture;

  void startProcessingChangeEvents() {
    ChangeEventProcessorTask changeEventProcessorTask =
        new ChangeEventProcessorTask(subscribedClasses, changeEventQueue, wingsPersistence);
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
