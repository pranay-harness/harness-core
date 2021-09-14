/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.persistence.PersistentEntity;

import software.wings.dl.WingsPersistence;
import software.wings.search.framework.SearchSourceEntitySyncState.SearchSourceEntitySyncStateKeys;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(PL)
@Slf4j
public class ChangeEventProcessorTask implements Runnable {
  private ExecutorService executorService;
  private Set<SearchEntity<?>> searchEntities;
  private WingsPersistence wingsPersistence;
  private ChangeEventMetricsTracker changeEventMetricsTracker;
  private BlockingQueue<ChangeEvent<?>> changeEventQueue;
  private long logMetricsCounter;

  ChangeEventProcessorTask(Set<SearchEntity<?>> searchEntities, WingsPersistence wingsPersistence,
      ChangeEventMetricsTracker changeEventMetricsTracker, BlockingQueue<ChangeEvent<?>> changeEventQueue) {
    this.searchEntities = searchEntities;
    this.wingsPersistence = wingsPersistence;
    this.changeEventMetricsTracker = changeEventMetricsTracker;
    this.changeEventQueue = changeEventQueue;
    this.logMetricsCounter = 0;
  }

  public void run() {
    executorService = Executors.newFixedThreadPool(
        searchEntities.size(), new ThreadFactoryBuilder().setNameFormat("change-processor-%d").build());
    try {
      boolean isRunningSuccessfully = true;
      while (isRunningSuccessfully) {
        ChangeEvent<?> changeEvent = changeEventQueue.poll(Integer.MAX_VALUE, TimeUnit.MINUTES);
        if (changeEvent != null) {
          isRunningSuccessfully = processChange(changeEvent);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("ChangeEvent processor interrupted");
    } finally {
      log.info("Shutting down search consumer service");
      executorService.shutdownNow();
    }
  }

  private boolean saveSearchSourceEntitySyncStateToken(Class<? extends PersistentEntity> sourceClass, String token) {
    String sourceClassName = sourceClass.getCanonicalName();

    Query<SearchSourceEntitySyncState> query = wingsPersistence.createQuery(SearchSourceEntitySyncState.class)
                                                   .field(SearchSourceEntitySyncStateKeys.sourceEntityClass)
                                                   .equal(sourceClassName);

    UpdateOperations<SearchSourceEntitySyncState> updateOperations =
        wingsPersistence.createUpdateOperations(SearchSourceEntitySyncState.class)
            .set(SearchSourceEntitySyncStateKeys.lastSyncedToken, token);

    SearchSourceEntitySyncState searchSourceEntitySyncState =
        wingsPersistence.upsert(query, updateOperations, upsertReturnNewOptions);
    if (searchSourceEntitySyncState == null || !searchSourceEntitySyncState.getLastSyncedToken().equals(token)) {
      log.error(String.format("Search Entity %s token %s could not be updated", sourceClass.getCanonicalName(), token));
      return false;
    }
    return true;
  }

  private Callable<Boolean> getProcessChangeEventTask(ChangeHandler changeHandler, ChangeEvent changeEvent) {
    return () -> changeHandler.handleChange(changeEvent);
  }

  private boolean processChange(ChangeEvent<?> changeEvent) {
    Instant start = Instant.now();
    Class<? extends PersistentEntity> sourceClass = changeEvent.getEntityType();
    List<Future<Boolean>> processChangeEventTaskFutures = new ArrayList<>();

    for (SearchEntity<?> searchEntity : searchEntities) {
      if (searchEntity.getSubscriptionEntities().contains(sourceClass)) {
        ChangeHandler changeHandler = searchEntity.getChangeHandler();
        Callable<Boolean> processChangeEventTask = getProcessChangeEventTask(changeHandler, changeEvent);
        processChangeEventTaskFutures.add(executorService.submit(processChangeEventTask));
      }
    }

    for (Future<Boolean> processChangeEventFuture : processChangeEventTaskFutures) {
      boolean isChangeHandled = false;
      try {
        isChangeHandled = processChangeEventFuture.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.error("Change Event thread interrupted", e);
      } catch (ExecutionException e) {
        log.error("Change event thread interrupted due to exception", e.getCause());
      }
      if (!isChangeHandled) {
        log.error("Could not process changeEvent {}", changeEvent.toString());
        return false;
      }
    }

    boolean isSaved = saveSearchSourceEntitySyncStateToken(sourceClass, changeEvent.getToken());
    if (!isSaved) {
      log.error("Could not save token. ChangeEvent {} could not be processed for entity {}", changeEvent.toString(),
          sourceClass.getCanonicalName());
    }

    double timeTaken = Duration.between(start, Instant.now()).toMillis();
    changeEventMetricsTracker.updateAverage(changeEvent.getEntityType().toString(), timeTaken);
    logMetrics(changeEvent, timeTaken);
    return isSaved;
  }

  private void logMetrics(ChangeEvent<?> changeEvent, double timeTaken) {
    logMetricsCounter++;
    boolean shouldLogMetrics = (logMetricsCounter % 5000) == 0;
    if (shouldLogMetrics) {
      log.info("Search change event blocking queue size {}", changeEventQueue.size());
      log.info("Time taken for changeEvent {}:{} is {}", changeEvent.getEntityType(), changeEvent.getChangeType(),
          timeTaken);
      log.info("Running average: {}", changeEventMetricsTracker.getRunningAverageTime());
      log.info("No. of change Events processed: {}", changeEventMetricsTracker.getNumChangeEvents());
    }
  }
}
