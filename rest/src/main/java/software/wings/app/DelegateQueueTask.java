package software.wings.app;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;
import static software.wings.dl.HQuery.excludeAuthority;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.service.impl.DelegateServiceImpl.VALIDATION_TIMEOUT;

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
import software.wings.waitnotify.ErrorNotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
      Query<DelegateTask> releaseLongQueuedTasks = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                                       .filter("status", Status.QUEUED)
                                                       .field("delegateId")
                                                       .exists()
                                                       .field("lastUpdatedAt")
                                                       .lessThan(clock.millis() - TimeUnit.MINUTES.toMillis(5));

      wingsPersistence.update(
          releaseLongQueuedTasks, wingsPersistence.createUpdateOperations(DelegateTask.class).unset("delegateId"));

      // Find tasks which are timed out and update their status to FAILED.
      List<DelegateTask> longRunningTimedOutTasks = new ArrayList<>();
      try {
        longRunningTimedOutTasks = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                       .filter("status", Status.STARTED)
                                       .asList()
                                       .stream()
                                       .filter(DelegateTask::isTimedOut)
                                       .collect(toList());
      } catch (com.esotericsoftware.kryo.KryoException kryo) {
        logger.warn("Delegate task schema backwards incompatibility", kryo);
        for (Key<DelegateTask> key : wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                         .filter("status", Status.STARTED)
                                         .asKeyList()) {
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
          Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                                .filter("status", Status.STARTED)
                                                .filter(Mapper.ID_KEY, delegateTask.getUuid());
          UpdateOperations<DelegateTask> updateOperations =
              wingsPersistence.createUpdateOperations(DelegateTask.class).set("status", Status.ERROR);

          DelegateTask updatedDelegateTask =
              wingsPersistence.getDatastore().findAndModify(updateQuery, updateOperations);

          if (updatedDelegateTask == null) {
            logger.info("Long running delegate task {} could not be updated to ERROR status", delegateTask.getUuid());
          } else if (isBlank(updatedDelegateTask.getWaitId())) {
            logger.info("Long running delegate task {} with type {} has no wait ID. Nothing to notify",
                delegateTask.getUuid(), delegateTask.getTaskType());
          } else {
            logger.info("Long running delegate task {} is terminated", updatedDelegateTask.getUuid());
            waitNotifyEngine.notify(updatedDelegateTask.getWaitId(),
                ErrorNotifyResponseData.builder()
                    .errorMessage("Delegate timeout. Delegate ID: " + updatedDelegateTask.getDelegateId())
                    .build());
          }
        });
      }

      // Find tasks which have been queued for too long and update their status to ERROR.
      List<DelegateTask> queuedTimedOutTasks = new ArrayList<>();
      try {
        queuedTimedOutTasks = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                  .filter("status", Status.QUEUED)
                                  .field("lastUpdatedAt")
                                  .lessThan(clock.millis() - TimeUnit.HOURS.toMillis(1))
                                  .asList();
      } catch (com.esotericsoftware.kryo.KryoException kryo) {
        logger.warn("Delegate task schema backwards incompatibility", kryo);
        for (Key<DelegateTask> key : wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                         .filter("status", Status.QUEUED)
                                         .asKeyList()) {
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
          Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                                .filter("status", Status.QUEUED)
                                                .filter(Mapper.ID_KEY, delegateTask.getUuid());
          UpdateOperations<DelegateTask> updateOperations =
              wingsPersistence.createUpdateOperations(DelegateTask.class).set("status", Status.ERROR);

          DelegateTask updatedDelegateTask =
              wingsPersistence.getDatastore().findAndModify(updateQuery, updateOperations);

          if (updatedDelegateTask == null) {
            logger.error("Queued delegate task {} could not be updated to ERROR status", delegateTask.getUuid());
          } else if (isBlank(updatedDelegateTask.getWaitId())) {
            logger.error("Queued delegate task {} with type {} has no wait ID. Nothing to notify",
                delegateTask.getUuid(), delegateTask.getTaskType());
          } else {
            logger.info("Queued delegate task {} is terminated", updatedDelegateTask.getUuid());
            waitNotifyEngine.notify(updatedDelegateTask.getWaitId(),
                ErrorNotifyResponseData.builder().errorMessage("Task queued too long").build());
          }
        });
      }

      // Re-broadcast queued tasks not picked up by any Delegate and not in process of validation
      List<DelegateTask> unassignedTasks = null;
      try {
        unassignedTasks = wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                              .filter("status", Status.QUEUED)
                              .field("delegateId")
                              .doesNotExist()
                              .asList();
      } catch (com.esotericsoftware.kryo.KryoException kryo) {
        logger.warn("Delegate task schema backwards incompatibility", kryo);
        for (Key<DelegateTask> key : wingsPersistence.createQuery(DelegateTask.class, excludeAuthority)
                                         .filter("status", Status.QUEUED)
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
        long now = clock.millis();
        unassignedTasks.forEach(delegateTask -> {
          if (delegateTask.getValidationStartedAt() == null
              || now - delegateTask.getValidationStartedAt() > VALIDATION_TIMEOUT) {
            logger.info("Re-broadcast queued task [{}]", delegateTask.getUuid());
            broadcasterFactory.lookup("/stream/delegate/" + delegateTask.getAccountId(), true).broadcast(delegateTask);
          }
        });
      }

    } catch (WingsException exception) {
      exception.logProcessedMessages(MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in the Notifier call", exception);
    }
  }
}
