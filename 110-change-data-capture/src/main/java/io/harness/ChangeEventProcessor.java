package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.changestreamsframework.ChangeEvent;
import io.harness.persistence.PersistentEntity;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
  private BlockingQueue<ChangeEvent<?>> changeEventQueue = new LinkedBlockingQueue<>(1000);
  private ExecutorService changeEventExecutorService =
      Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("primary-change-processor").build());
  private Future<?> changeEventProcessorTaskFuture;

  void startProcessingChangeEvents(Set<Class<? extends PersistentEntity>> subscribedClasses) {
    ChangeEventProcessorTask changeEventProcessorTask =
        new ChangeEventProcessorTask(subscribedClasses, changeEventQueue);
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
