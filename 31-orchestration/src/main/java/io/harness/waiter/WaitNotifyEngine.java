package io.harness.waiter;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.waiter.NotifyEvent.Builder.aNotifyEvent;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.logging.AutoLogRemoveContext;
import io.harness.persistence.HPersistence;
import io.harness.waiter.NotifyResponse.NotifyResponseKeys;
import io.harness.waiter.WaitInstance.WaitInstanceBuilder;
import io.harness.waiter.WaitInstance.WaitInstanceKeys;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * WaitNotifyEngine allows tasks to register in waitQueue and get notified via callback.
 * No entry in the waitQueue found for the correlationIds:
 */
@Singleton
@Slf4j
public class WaitNotifyEngine {
  @Inject private HPersistence persistence;
  @Inject private NotifyQueuePublisherRegister publisherRegister;

  public String waitForAllOn(String publisherName, NotifyCallback callback, String... correlationIds) {
    Preconditions.checkArgument(isNotEmpty(correlationIds), "correlationIds are null or empty");

    if (logger.isDebugEnabled()) {
      logger.debug("Received waitForAll on - correlationIds : {}", Arrays.toString(correlationIds));
    }

    final WaitInstanceBuilder waitInstanceBuilder =
        WaitInstance.builder().uuid(generateUuid()).callback(callback).publisher(publisherName);

    final List<String> list;
    if (correlationIds.length == 1) {
      list = singletonList(correlationIds[0]);
    } else {
      // In case of multiple items, we have to make sure that all of them are unique
      Set<String> set = new HashSet<>();
      Collections.addAll(set, correlationIds);
      list = new ArrayList<>(set);
    }
    waitInstanceBuilder.correlationIds(list).waitingOnCorrelationIds(list);

    final String waitInstanceId = persistence.save(waitInstanceBuilder.build());

    // We cannot combine the logic of obtaining the responses before the save, because this will create a race with
    // storing the responses.

    final List<String> keys = persistence.createQuery(NotifyResponse.class, excludeAuthority)
                                  .field(NotifyResponseKeys.uuid)
                                  .in(list)
                                  .asKeyList()
                                  .stream()
                                  .map(key -> (String) key.getId())
                                  .collect(toList());

    if (isNotEmpty(keys)) {
      final Query<WaitInstance> query =
          persistence.createQuery(WaitInstance.class, excludeAuthority).filter(WaitInstanceKeys.uuid, waitInstanceId);

      final UpdateOperations<WaitInstance> operations = persistence.createUpdateOperations(WaitInstance.class)
                                                            .removeAll(WaitInstanceKeys.waitingOnCorrelationIds, keys);

      WaitInstance waitInstance;
      if ((waitInstance = persistence.findAndModify(query, operations, HPersistence.returnNewOptions)) != null) {
        if (isEmpty(waitInstance.getWaitingOnCorrelationIds())
            && waitInstance.getCallbackProcessingAt() < System.currentTimeMillis()) {
          sendNotification(waitInstance);
        }
      }
    }

    return waitInstanceId;
  }

  public String doneWith(String correlationId, ResponseData response) {
    return doneWith(correlationId, response, response instanceof ErrorNotifyResponseData);
  }

  private String doneWith(String correlationId, ResponseData response, boolean error) {
    Preconditions.checkArgument(isNotBlank(correlationId), "correlationId is null or empty");

    if (logger.isDebugEnabled()) {
      logger.debug("notify request received for the correlationId : {}", correlationId);
    }

    try {
      persistence.save(NotifyResponse.builder()
                           .uuid(correlationId)
                           .createdAt(currentTimeMillis())
                           .response(response)
                           .error(error || response instanceof ErrorNotifyResponseData)
                           .build());
      handleNotifyResponse(correlationId);
      return correlationId;
    } catch (DuplicateKeyException exception) {
      logger.warn("Unexpected rate of DuplicateKeyException per correlation", exception);
    } catch (Exception exception) {
      logger.error("Failed to notify for response of type " + response.getClass().getSimpleName(), exception);
    }
    return null;
  }

  public void sendNotification(WaitInstance waitInstance) {
    try (AutoLogRemoveContext ignore = new AutoLogRemoveContext(WaitInstanceLogContext.ID)) {
      String publisher = waitInstance.getPublisher();

      // TODO: remove after 15/01/20202
      // this is temporary to handle the transition for wait instances without publisher {{
      if (publisher == null) {
        publisher = "general";
      }
      // }}
      final NotifyQueuePublisher notifyQueuePublisher = publisherRegister.obtain(waitInstance.getPublisher());

      if (notifyQueuePublisher == null) {
        // There is nothing smart that we can do.
        // If there is no publisher we should let people evaluate and handle the problem.
        logger.error("Unknown publisher {}", publisher);
        return;
      }

      notifyQueuePublisher.send(aNotifyEvent().waitInstanceId(waitInstance.getUuid()).build());
    }
  }

  public void handleNotifyResponse(String uuid) {
    final Query<WaitInstance> query = persistence.createQuery(WaitInstance.class, excludeAuthority)
                                          .filter(WaitInstanceKeys.waitingOnCorrelationIds, uuid);

    final UpdateOperations<WaitInstance> operations = persistence.createUpdateOperations(WaitInstance.class)
                                                          .removeAll(WaitInstanceKeys.waitingOnCorrelationIds, uuid);

    WaitInstance waitInstance;
    while ((waitInstance = persistence.findAndModify(query, operations, HPersistence.returnNewOptions)) != null) {
      if (isEmpty(waitInstance.getWaitingOnCorrelationIds())) {
        sendNotification(waitInstance);
      }
    }
  }
}
