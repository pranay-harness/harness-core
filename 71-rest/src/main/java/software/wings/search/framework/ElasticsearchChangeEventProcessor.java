package software.wings.search.framework;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;
import software.wings.search.framework.SearchSourceEntitySyncState.SearchSourceEntitySyncStateKeys;
import software.wings.search.framework.changestreams.ChangeEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
class ElasticsearchChangeEventProcessor {
  @Inject private Set<SearchEntity<?>> searchEntities;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ChangeEventMetricsTracker changeEventMetricsTracker;
  private ExecutorService executorService =
      Executors.newFixedThreadPool(10, new ThreadFactoryBuilder().setNameFormat("change-processor-%d").build());

  private boolean saveSearchSourceEntitySyncStateToken(Class<? extends PersistentEntity> sourceClass, String token) {
    String sourceClassName = sourceClass.getCanonicalName();

    Query<SearchSourceEntitySyncState> query = wingsPersistence.createQuery(SearchSourceEntitySyncState.class)
                                                   .field(SearchSourceEntitySyncStateKeys.sourceEntityClass)
                                                   .equal(sourceClassName);

    UpdateOperations<SearchSourceEntitySyncState> updateOperations =
        wingsPersistence.createUpdateOperations(SearchSourceEntitySyncState.class)
            .set(SearchSourceEntitySyncStateKeys.lastSyncedToken, token);

    SearchSourceEntitySyncState searchSourceEntitySyncState =
        wingsPersistence.upsert(query, updateOperations, HPersistence.upsertReturnNewOptions);
    if (searchSourceEntitySyncState == null || !searchSourceEntitySyncState.getLastSyncedToken().equals(token)) {
      logger.error(
          String.format("Search Entity %s token %s could not be updated", sourceClass.getCanonicalName(), token));
      return false;
    }
    return true;
  }

  private Callable<Boolean> getProcessChangeEventTask(ChangeHandler changeHandler, ChangeEvent changeEvent) {
    return () -> changeHandler.handleChange(changeEvent);
  }

  boolean processChange(ChangeEvent<?> changeEvent) {
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
        logger.error("Change Event thread interrupted", e);
      } catch (ExecutionException e) {
        logger.error("Change event thread interrupted due to exception", e.getCause());
      }
      if (!isChangeHandled) {
        logger.error("Could not process changeEvent {}", changeEvent.toString());
        return false;
      }
    }

    boolean isSaved = saveSearchSourceEntitySyncStateToken(sourceClass, changeEvent.getToken());
    if (!isSaved) {
      logger.error(String.format("Could not save token. ChangeEvent %s could not be processed for entity %s",
          changeEvent.toString(), sourceClass.getCanonicalName()));
    }

    double timeTaken = Duration.between(start, Instant.now()).toMillis();
    changeEventMetricsTracker.updateAverage(changeEvent.getEntityType().toString(), timeTaken);
    logger.info(
        "Time taken for changeEvent {}:{} is {}", changeEvent.getEntityType(), changeEvent.getChangeType(), timeTaken);
    logger.info("Running average: {}", changeEventMetricsTracker.getRunningAverageTime());
    logger.info("No. of change Events processed: {}", changeEventMetricsTracker.getNumChangeEvents());

    return isSaved;
  }

  void shutdown() {
    executorService.shutdownNow();
  }
}
