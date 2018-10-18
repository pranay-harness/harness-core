package software.wings.waitnotify;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.delegate.task.protocol.ResponseData;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.persistence.ReadPref;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ExecutionStatus;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
@Singleton
public final class NotifyEventListener extends AbstractQueueListener<NotifyEvent> {
  private static final Logger logger = LoggerFactory.getLogger(NotifyEventListener.class);

  @Inject private Injector injector;

  @Inject private WingsPersistence wingsPersistence;

  @Inject private PersistentLocker persistentLocker;

  public NotifyEventListener() {
    super(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void onMessage(NotifyEvent message) {
    if (logger.isTraceEnabled()) {
      logger.trace("Processing message {}", message);
    }

    String waitInstanceId = message.getWaitInstanceId();

    WaitInstance waitInstance = wingsPersistence.get(WaitInstance.class, waitInstanceId, ReadPref.CRITICAL);

    if (waitInstance == null) {
      logger.info("waitInstance not found for waitInstanceId: {}", waitInstanceId);
      return;
    }

    if (waitInstance.getStatus() != ExecutionStatus.NEW) {
      logger.warn("WaitInstance already processed - waitInstanceId:[{}], status=[{}] skipping ...", waitInstanceId,
          waitInstance.getStatus());
      return;
    }

    List<WaitQueue> waitQueues = wingsPersistence.createQuery(WaitQueue.class, ReadPref.CRITICAL, excludeAuthority)
                                     .filter(WaitQueue.WAIT_INSTANCE_ID_KEY, waitInstanceId)
                                     .asList();

    if (isEmpty(waitQueues)) {
      logger.warn("No entry in the waitQueue found for the waitInstanceId:[{}] skipping ...", waitInstanceId);
      return;
    }

    List<String> correlationIds = message.getCorrelationIds();
    if (isNotEmpty(correlationIds)) {
      Set<String> correlationIdSet = new HashSet<>(correlationIds);
      List<String> missingCorrelationIds = waitQueues.stream()
                                               .map(WaitQueue::getCorrelationId)
                                               .filter(s -> !correlationIdSet.contains(s))
                                               .collect(toList());
      if (isNotEmpty(missingCorrelationIds)) {
        logger.info("Some of the correlationIds still needs to be waited, waitInstanceId: [{}], correlationIds: {}",
            waitInstanceId, missingCorrelationIds);
        return;
      }
    }

    final List<NotifyResponse> notifyResponses =
        wingsPersistence.createQuery(NotifyResponse.class, ReadPref.CRITICAL, excludeAuthority)
            .field(ID_KEY)
            .in(waitQueues.stream().map(WaitQueue::getCorrelationId).collect(toList()))
            .asList();

    Set<String> correlationIdSet = notifyResponses.stream().map(NotifyResponse::getUuid).collect(toSet());
    if (notifyResponses.size() != waitQueues.size()) {
      List<String> missingCorrelationIds = waitQueues.stream()
                                               .map(WaitQueue::getCorrelationId)
                                               .filter(s -> !correlationIdSet.contains(s))
                                               .collect(toList());
      logger.warn(
          "notifyResponses for the correlationIds: {} not found. skipping the callback for the waitInstanceId: [{}]",
          missingCorrelationIds, waitInstanceId);
      return;
    }

    Map<String, ResponseData> responseMap = new HashMap<>();
    Map<String, NotifyResponse> notifyResponseMap = new HashMap<>();

    notifyResponses.forEach(notifyResponse -> {
      responseMap.put(notifyResponse.getUuid(), notifyResponse.getResponse());
      notifyResponseMap.put(notifyResponse.getUuid(), notifyResponse);
    });

    boolean isError = notifyResponses.stream().filter(NotifyResponse::isError).findFirst().isPresent();

    try (AcquiredLock lock =
             persistentLocker.tryToAcquireLock(WaitInstance.class, waitInstanceId, Duration.ofMinutes(1))) {
      if (lock == null) {
        return;
      }
      ExecutionStatus status = ExecutionStatus.SUCCESS;
      NotifyCallback callback = waitInstance.getCallback();
      injector.injectMembers(callback);
      if (callback != null) {
        try {
          if (isError) {
            callback.notifyError(responseMap);
          } else {
            callback.notify(responseMap);
          }
        } catch (Exception exception) {
          status = ExecutionStatus.ERROR;
          logger.error("WaitInstance callback failed - waitInstanceId:" + waitInstanceId, exception);
          try {
            WaitInstanceError waitInstanceError = new WaitInstanceError();
            waitInstanceError.setWaitInstanceId(waitInstanceId);
            waitInstanceError.setResponseMap(responseMap);
            waitInstanceError.setErrorStackTrace(ExceptionUtils.getStackTrace(exception));

            wingsPersistence.save(waitInstanceError);
          } catch (Exception e2) {
            logger.error("Error in persisting waitInstanceError", e2);
          }
        }
      }

      // time to cleanup
      try {
        UpdateOperations<WaitInstance> waitInstanceUpdate =
            wingsPersistence.createUpdateOperations(WaitInstance.class).set("status", status);
        wingsPersistence.update(waitInstance, waitInstanceUpdate);
      } catch (Exception exception) {
        logger.error("Error in waitInstanceUpdate", exception);
      }

      UpdateOperations<NotifyResponse> notifyResponseUpdate =
          wingsPersistence.createUpdateOperations(NotifyResponse.class).set("status", ExecutionStatus.SUCCESS);
      for (WaitQueue waitQueue : waitQueues) {
        try {
          wingsPersistence.delete(waitQueue);
          wingsPersistence.update(notifyResponseMap.get(waitQueue.getCorrelationId()), notifyResponseUpdate);
        } catch (Exception exception) {
          logger.error("Error in waitQueue cleanup", exception);
        }
      }
    }
    logger.trace("Done processing message {}", message);
  }
}
