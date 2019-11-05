package io.harness.waiter;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.waiter.NotifyEvent.Builder.aNotifyEvent;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.persistence.HPersistence;
import io.harness.queue.Queue;
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
  @Inject private Queue<NotifyEvent> notifyQueue;

  public String waitForAll(NotifyCallback callback, String... correlationIds) {
    Preconditions.checkArgument(isNotEmpty(correlationIds), "correlationIds are null or empty");

    if (logger.isDebugEnabled()) {
      logger.debug("Received waitForAll on - correlationIds : {}", Arrays.toString(correlationIds));
    }

    final WaitInstanceBuilder waitInstanceBuilder = WaitInstance.builder().uuid(generateUuid()).callback(callback);

    final List<String> list;
    if (correlationIds.length == 1) {
      list = singletonList(correlationIds[0]);
    } else {
      // In case of multiple items, we have to make sure that all of them are unique
      Set<String> set = new HashSet<>();
      Collections.addAll(set, correlationIds);
      list = new ArrayList<>(set);
    }

    return persistence.save(waitInstanceBuilder.correlationIds(list).waitingOnCorrelationIds(list).build());
  }

  public <T extends ResponseData> String notify(String correlationId, T response) {
    return notify(correlationId, response, response instanceof ErrorNotifyResponseData);
  }

  private <T extends ResponseData> String notify(String correlationId, T response, boolean error) {
    Preconditions.checkArgument(isNotBlank(correlationId), "correlationId is null or empty");

    if (logger.isDebugEnabled()) {
      logger.debug("notify request received for the correlationId : {}", correlationId);
    }

    try {
      String notificationId = persistence.save(NotifyResponse.<T>builder()
                                                   .uuid(correlationId)
                                                   .createdAt(currentTimeMillis())
                                                   .response(response)
                                                   .error(error || response instanceof ErrorNotifyResponseData)
                                                   .build());

      final Query<WaitInstance> query = persistence.createQuery(WaitInstance.class, excludeAuthority)
                                            .filter(WaitInstanceKeys.waitingOnCorrelationIds, correlationId);

      final UpdateOperations<WaitInstance> operations =
          persistence.createUpdateOperations(WaitInstance.class)
              .removeAll(WaitInstanceKeys.waitingOnCorrelationIds, correlationId);

      WaitInstance waitInstance;
      while ((waitInstance = persistence.findAndModify(query, operations, HPersistence.returnNewOptions)) != null) {
        if (isEmpty(waitInstance.getWaitingOnCorrelationIds())) {
          notifyQueue.send(aNotifyEvent().waitInstanceId(waitInstance.getUuid()).build());
        }
      }

      return notificationId;
    } catch (DuplicateKeyException exception) {
      logger.warn("Unexpected rate of DuplicateKeyException per correlation", exception);
    } catch (Exception exception) {
      logger.error("Failed to notify for response of type " + response.getClass().getSimpleName(), exception);
    }
    return null;
  }
}
