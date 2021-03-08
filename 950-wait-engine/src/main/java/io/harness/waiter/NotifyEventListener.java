package io.harness.waiter;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ResponseData;
import io.harness.waiter.NotifyResponse.NotifyResponseKeys;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;

@Slf4j
public class NotifyEventListener extends QueueListener<NotifyEvent> {
  @Inject private Injector injector;
  @Inject private HPersistence persistence;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private WaitInstanceService waitInstanceService;

  @Inject
  public NotifyEventListener(QueueConsumer<NotifyEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(NotifyEvent message) {
    String waitInstanceId = message.getWaitInstanceId();

    try (AutoLogContext ignore = new WaitInstanceLogContext(message.getWaitInstanceId(), OVERRIDE_ERROR)) {
      final long now = System.currentTimeMillis();
      WaitInstance waitInstance = waitInstanceService.fetchForProcessingWaitInstance(waitInstanceId, now);

      if (waitInstance == null) {
        log.error("WaitInstance was already handled!");
        return;
      }

      boolean isError = false;
      Map<String, ResponseData> responseMap = new HashMap<>();

      Query<NotifyResponse> query = persistence.createQuery(NotifyResponse.class, excludeAuthority)
                                        .field(NotifyResponseKeys.uuid)
                                        .in(waitInstance.getCorrelationIds());

      if (waitInstance.getProgressCallback() != null) {
        query.order(Sort.ascending(NotifyResponseKeys.createdAt));
      }

      try (HIterator<NotifyResponse> notifyResponses = new HIterator(query.fetch())) {
        for (NotifyResponse notifyResponse : notifyResponses) {
          if (notifyResponse.isError()) {
            log.info("Failed notification response {}", notifyResponse.getUuid());
            isError = true;
          }
          if (notifyResponse.getResponseData() != null) {
            responseMap.put(notifyResponse.getUuid(),
                (ResponseData) kryoSerializer.asInflatedObject(notifyResponse.getResponseData()));
          }
        }
      }

      NotifyCallback callback = waitInstance.getCallback();
      if (callback != null) {
        injector.injectMembers(callback);
        try {
          if (isError) {
            callback.notifyError(responseMap);
          } else {
            callback.notify(responseMap);
          }
          log.info("WaitInstance callback finished");
        } catch (Exception exception) {
          log.error("WaitInstance callback failed", exception);
        }
      }

      try {
        persistence.delete(waitInstance);
      } catch (Exception exception) {
        log.error("Failed to delete WaitInstance", exception);
      }

      waitInstanceService.checkProcessingTime(now);
    }
  }
}
