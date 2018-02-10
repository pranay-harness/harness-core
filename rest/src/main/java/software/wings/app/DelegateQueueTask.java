package software.wings.app;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.DelegateTask.Status.QUEUED;
import static software.wings.common.Constants.DELEGATE_SYNC_CACHE;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;
import static software.wings.waitnotify.ErrorNotifyResponseData.Builder.anErrorNotifyResponseData;

import com.google.inject.Inject;

import org.atmosphere.cpr.BroadcasterFactory;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTask.Status;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.utils.CacheHelper;
import software.wings.waitnotify.WaitNotifyEngine;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.cache.Caching;

/**
 * Scheduled Task to look for finished WaitInstances and send messages to NotifyEventQueue.
 *
 * @author Rishi
 */
public class DelegateQueueTask implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(DelegateQueueTask.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private BroadcasterFactory broadcasterFactory;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private CacheHelper cacheHelper;
  @Inject private Clock clock;

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    if (isMaintenance()) {
      return;
    }

    try (AcquiredLock lock = persistentLocker.acquireLock(
             DelegateQueueTask.class, DelegateQueueTask.class.getName(), Duration.ofMinutes(1))) {
      // Release tasks acquired by delegate but not started execution. Introduce "ACQUIRED" status may be ?
      Query<DelegateTask> releaseLongQueuedTasks =
          wingsPersistence.createQuery(DelegateTask.class)
              .field("status")
              .equal(Status.QUEUED)
              .field("delegateId")
              .exists()
              .field("lastUpdatedAt")
              .lessThan(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5));

      wingsPersistence.update(
          releaseLongQueuedTasks, wingsPersistence.createUpdateOperations(DelegateTask.class).unset("delegateId"));
      // Find async tasks which are timed out and update their status to FAILED.
      List<DelegateTask> longRunningTimedOutTasks = new ArrayList<>();
      try {
        longRunningTimedOutTasks = wingsPersistence.createQuery(DelegateTask.class)
                                       .field("status")
                                       .equal(Status.STARTED)
                                       .asList()
                                       .stream()
                                       .filter(DelegateTask::isTimedOut)
                                       .collect(toList());
      } catch (com.esotericsoftware.kryo.KryoException kryo) {
        logger.warn("Delegate task schema backwards incompatibility", kryo);
        for (Key<DelegateTask> key :
            wingsPersistence.createQuery(DelegateTask.class).field("status").equal(Status.STARTED).asKeyList()) {
          try {
            wingsPersistence.get(DelegateTask.class, key.getId().toString());
          } catch (com.esotericsoftware.kryo.KryoException ex) {
            wingsPersistence.delete(DelegateTask.class, key.getId().toString());
          }
        }
      }

      if (!longRunningTimedOutTasks.isEmpty()) {
        logger.info("Found {} long running tasks, to be killed", longRunningTimedOutTasks.size());
        longRunningTimedOutTasks.forEach(delegateTask -> {
          Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                                .field("status")
                                                .equal(Status.STARTED)
                                                .field(Mapper.ID_KEY)
                                                .equal(delegateTask.getUuid());
          UpdateOperations<DelegateTask> updateOperations =
              wingsPersistence.createUpdateOperations(DelegateTask.class).set("status", Status.ERROR);

          DelegateTask updatedDelegateTask =
              wingsPersistence.getDatastore().findAndModify(updateQuery, updateOperations);

          if (updatedDelegateTask == null) {
            logger.error("Long running delegate task {} could not be updated to ERROR status", delegateTask.getUuid());
          } else if (isBlank(updatedDelegateTask.getWaitId())) {
            logger.error("Long running delegate task {} with type {} has no wait ID. Nothing to notify",
                delegateTask.getUuid(), delegateTask.getTaskType().name());
          } else {
            logger.info("Long running delegate task {} is terminated", updatedDelegateTask.getUuid());
            waitNotifyEngine.notify(updatedDelegateTask.getWaitId(),
                anErrorNotifyResponseData()
                    .withErrorMessage("Delegate timeout. Delegate ID: " + updatedDelegateTask.getDelegateId())
                    .build());
          }
        });
      }

      // Find async tasks which have been queued for too long and update their status to ERROR.
      List<DelegateTask> queuedTimedOutTasks = new ArrayList<>();
      try {
        queuedTimedOutTasks =
            wingsPersistence.createQuery(DelegateTask.class)
                .field("status")
                .equal(Status.QUEUED)
                .asList()
                .stream()
                .filter(delegateTask -> clock.millis() - delegateTask.getLastUpdatedAt() > TimeUnit.HOURS.toMillis(1))
                .collect(toList());
      } catch (com.esotericsoftware.kryo.KryoException kryo) {
        logger.warn("Delegate task schema backwards incompatibility", kryo);
        for (Key<DelegateTask> key :
            wingsPersistence.createQuery(DelegateTask.class).field("status").equal(Status.QUEUED).asKeyList()) {
          try {
            wingsPersistence.get(DelegateTask.class, key.getId().toString());
          } catch (com.esotericsoftware.kryo.KryoException ex) {
            wingsPersistence.delete(DelegateTask.class, key.getId().toString());
          }
        }
      }

      if (!queuedTimedOutTasks.isEmpty()) {
        logger.info("Found {} long queued tasks, to be killed", queuedTimedOutTasks.size());
        queuedTimedOutTasks.forEach(delegateTask -> {
          Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                                .field("status")
                                                .equal(Status.QUEUED)
                                                .field(Mapper.ID_KEY)
                                                .equal(delegateTask.getUuid());
          UpdateOperations<DelegateTask> updateOperations =
              wingsPersistence.createUpdateOperations(DelegateTask.class).set("status", Status.ERROR);

          DelegateTask updatedDelegateTask =
              wingsPersistence.getDatastore().findAndModify(updateQuery, updateOperations);

          if (updatedDelegateTask == null) {
            logger.error("Queued delegate task {} could not be updated to ERROR status", delegateTask.getUuid());
          } else if (isBlank(updatedDelegateTask.getWaitId())) {
            logger.error("Queued delegate task {} with type {} has no wait ID. Nothing to notify",
                delegateTask.getUuid(), delegateTask.getTaskType().name());
          } else {
            logger.info("Queued delegate task {} is terminated", updatedDelegateTask.getUuid());
            waitNotifyEngine.notify(updatedDelegateTask.getWaitId(),
                anErrorNotifyResponseData().withErrorMessage("Task queued too log").build());
          }
        });
      }

      // Re-broadcast queued sync tasks not picked up and not in process of validation and remove timed out tasks
      Cache<String, DelegateTask> delegateSyncCache =
          cacheHelper.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class);
      Iterator<Cache.Entry<String, DelegateTask>> iterator = delegateSyncCache.iterator();
      try {
        while (iterator.hasNext()) {
          Cache.Entry<String, DelegateTask> stringDelegateTaskEntry = iterator.next();
          if (stringDelegateTaskEntry != null) {
            try {
              DelegateTask syncDelegateTask = stringDelegateTaskEntry.getValue();
              if (syncDelegateTask.getStatus().equals(Status.QUEUED) && syncDelegateTask.getDelegateId() == null) {
                // If it's timed out, remove it
                if (clock.millis() - syncDelegateTask.getCreatedAt() > syncDelegateTask.getTimeout()) {
                  logger.warn("Evicting old delegate sync task {}", syncDelegateTask.getUuid());
                  Caching.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class)
                      .remove(syncDelegateTask.getUuid());
                } else {
                  Set<String> validatingDelegates = syncDelegateTask.getValidatingDelegateIds();
                  Set<String> completeDelegates = syncDelegateTask.getValidationCompleteDelegateIds();
                  if ((isEmpty(validatingDelegates) && isEmpty(completeDelegates))
                      || completeDelegates.containsAll(validatingDelegates)) {
                    syncDelegateTask.setValidatingDelegateIds(null);
                    syncDelegateTask.setValidationCompleteDelegateIds(null);
                    Caching.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class)
                        .put(syncDelegateTask.getUuid(), syncDelegateTask);

                    logger.info("Re-broadcast queued sync task [{}] {} Account: {}", syncDelegateTask.getUuid(),
                        syncDelegateTask.getTaskType().name(), syncDelegateTask.getAccountId());
                    broadcasterFactory.lookup("/stream/delegate/" + syncDelegateTask.getAccountId(), true)
                        .broadcast(syncDelegateTask);
                  }
                }
              }
            } catch (Exception ex) {
              logger.error("Could not fetch delegate task from queue", ex);
              logger.warn("Remove Delegate task {} from cache", stringDelegateTaskEntry.getKey());
              Caching.getCache(DELEGATE_SYNC_CACHE, String.class, DelegateTask.class)
                  .remove(stringDelegateTaskEntry.getKey());
            }
          }
        }
      } catch (Exception e) {
        delegateSyncCache.clear();
      }

      // Re-broadcast queued async tasks not picked up by any Delegate and not in process of validation
      List<DelegateTask> unassignedTasks = null;
      try {
        unassignedTasks = wingsPersistence.createQuery(DelegateTask.class)
                              .field("status")
                              .equal(Status.QUEUED)
                              .field("delegateId")
                              .doesNotExist()
                              .asList();
      } catch (com.esotericsoftware.kryo.KryoException kryo) {
        logger.warn("Delegate task schema backwards incompatibility", kryo);
        for (Key<DelegateTask> key : wingsPersistence.createQuery(DelegateTask.class)
                                         .field("status")
                                         .equal(Status.QUEUED)
                                         .field("delegateId")
                                         .doesNotExist()
                                         .asKeyList()) {
          try {
            wingsPersistence.get(DelegateTask.class, key.getId().toString());
          } catch (com.esotericsoftware.kryo.KryoException ex) {
            wingsPersistence.delete(DelegateTask.class, key.getId().toString());
          }
        }
      }

      if (isNotEmpty(unassignedTasks)) {
        unassignedTasks.forEach(delegateTask -> {
          Set<String> validatingDelegates = delegateTask.getValidatingDelegateIds();
          Set<String> completeDelegates = delegateTask.getValidationCompleteDelegateIds();
          if ((isEmpty(validatingDelegates) && isEmpty(completeDelegates))
              || completeDelegates.containsAll(validatingDelegates)) {
            UpdateOperations<DelegateTask> updateOperations =
                wingsPersistence.createUpdateOperations(DelegateTask.class)
                    .unset("validatingDelegateIds")
                    .unset("validationCompleteDelegateIds");
            Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                                  .field("accountId")
                                                  .equal(delegateTask.getAccountId())
                                                  .field("status")
                                                  .equal(QUEUED)
                                                  .field("delegateId")
                                                  .doesNotExist()
                                                  .field(ID_KEY)
                                                  .equal(delegateTask.getUuid());
            wingsPersistence.update(updateQuery, updateOperations);

            logger.info("Re-broadcast queued async task [{}]", delegateTask.getUuid());
            broadcasterFactory.lookup("/stream/delegate/" + delegateTask.getAccountId(), true).broadcast(delegateTask);
          }
        });
      }

    } catch (WingsException exception) {
      exception.logProcessedMessages(logger);
    } catch (Exception exception) {
      logger.error("Error seen in the Notifier call", exception);
    }
  }
}
