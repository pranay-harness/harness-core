package io.harness.waiter;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.maintenance.MaintenanceController.getMaintenanceFlag;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.HIterator;
import io.harness.persistence.HKeyIterator;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.waiter.NotifyResponse.NotifyResponseKeys;
import io.harness.waiter.WaitInstance.WaitInstanceKeys;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Key;

/**
 * Scheduled Task to look for finished WaitInstances and send messages to NotifyEventQueue.
 */
@Slf4j
public class NotifyResponseCleaner implements Runnable {
  @Inject private HPersistence persistence;
  @Inject private QueueController queueController;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Override
  public void run() {
    if (getMaintenanceFlag() || queueController.isNotPrimary()) {
      return;
    }

    try {
      execute();
    } catch (Exception e) {
      log.error("Exception happened in Notifier execute", e);
    }
  }

  public void execute() {
    try {
      executeInternal();
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, log);
    } catch (Exception exception) {
      log.error("Error seen in the Notifier call", exception);
    }
  }

  public void executeInternal() {
    log.debug("Execute Notifier response processing");

    // Sometimes response might arrive before we schedule the wait. Do not remove responses that are very new.
    final long limit = System.currentTimeMillis() - Duration.ofSeconds(15).toMillis();

    List<String> deleteResponses = new ArrayList<>();

    try (HKeyIterator<NotifyResponse> iterator =
             new HKeyIterator<>(persistence.createQuery(NotifyResponse.class, excludeAuthority)
                                    .field(NotifyResponseKeys.createdAt)
                                    .lessThan(limit)
                                    .fetchKeys())) {
      boolean needHandling = false;
      for (Key<NotifyResponse> key : iterator) {
        String uuid = key.getId().toString();
        try (HIterator<WaitInstance> waitInstances =
                 new HIterator<>(persistence.createQuery(WaitInstance.class, excludeAuthority)
                                     .filter(WaitInstanceKeys.correlationIds, uuid)
                                     .fetch())) {
          if (!waitInstances.hasNext()) {
            deleteResponses.add(uuid);
            if (deleteResponses.size() >= 1000) {
              deleteObsoleteResponses(deleteResponses);
              deleteResponses.clear();
            }
          }

          for (WaitInstance waitInstance : waitInstances) {
            if (isEmpty(waitInstance.getWaitingOnCorrelationIds())) {
              if (waitInstance.getCallbackProcessingAt() < System.currentTimeMillis()) {
                waitNotifyEngine.sendNotification(waitInstance);
              }
            } else if (waitInstance.getWaitingOnCorrelationIds().contains(uuid)) {
              needHandling = true;
            }
          }
        }

        if (needHandling) {
          waitNotifyEngine.handleNotifyResponse(uuid);
        }
      }

      deleteObsoleteResponses(deleteResponses);
    }
  }

  private void deleteObsoleteResponses(List<String> deleteResponses) {
    if (isNotEmpty(deleteResponses)) {
      log.info("Deleting {} not needed responses", deleteResponses.size());
      persistence.deleteOnServer(persistence.createQuery(NotifyResponse.class, excludeAuthority)
                                     .field(NotifyResponseKeys.uuid)
                                     .in(deleteResponses));
    }
  }
}
