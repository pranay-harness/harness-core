package io.harness.waiter;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.waiter.NotifyEvent.Builder.aNotifyEvent;
import static java.lang.System.currentTimeMillis;
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
  @Inject private HPersistence wingsPersistence;
  @Inject private Queue<NotifyEvent> notifyQueue;

  public String waitForAll(NotifyCallback callback, String... correlationIds) {
    Preconditions.checkArgument(isNotEmpty(correlationIds), "correlationIds are null or empty");

    if (logger.isDebugEnabled()) {
      logger.debug("Received waitForAll on - correlationIds : {}", Arrays.toString(correlationIds));
    }

    final WaitInstanceBuilder waitInstanceBuilder = WaitInstance.builder().uuid(generateUuid()).callback(callback);

    if (correlationIds.length == 1) {
      waitInstanceBuilder.correlationIds(Collections.singletonList(correlationIds[0]));
    } else {
      // In case of multiple items, we have to make sure that all of them are unique
      Set<String> set = new HashSet<>();
      Collections.addAll(set, correlationIds);
      waitInstanceBuilder.correlationIds(new ArrayList<>(set));
    }

    return wingsPersistence.save(waitInstanceBuilder.build());
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
      String notificationId = wingsPersistence.save(NotifyResponse.<T>builder()
                                                        .uuid(correlationId)
                                                        .createdAt(currentTimeMillis())
                                                        .response(response)
                                                        .error(error)
                                                        .build());

      final List<WaitInstance> waitInstances = wingsPersistence.createQuery(WaitInstance.class, excludeAuthority)
                                                   .filter(WaitInstanceKeys.correlationIds, correlationId)
                                                   .asList();

      waitInstances.forEach(
          waitInstance -> notifyQueue.send(aNotifyEvent().waitInstanceId(waitInstance.getUuid()).error(error).build()));

      return notificationId;
    } catch (DuplicateKeyException exception) {
      logger.warn("Unexpected rate of DuplicateKeyException per correlation", exception);
    } catch (Exception exception) {
      logger.error("Failed to notify for response of type " + response.getClass().getSimpleName(), exception);
    }
    return null;
  }
}
