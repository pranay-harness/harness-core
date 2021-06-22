package io.harness.pms.events.base;

import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.eventsframework.consumer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.event.MessageListener;
import io.harness.serializer.ProtoUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class PmsAbstractMessageListener<T extends com.google.protobuf.Message, H
                                                     extends PmsBaseEventHandler<T>> implements MessageListener {
  private static final Duration THRESHOLD_PROCESS_DURATION = Duration.ofSeconds(5);

  public final String serviceName;
  public final Class<T> entityClass;
  public final H handler;
  public final ExecutorService executorService;

  public PmsAbstractMessageListener(
      String serviceName, Class<T> entityClass, H handler, ExecutorService executorService) {
    this.serviceName = serviceName;
    this.entityClass = entityClass;
    this.handler = handler;
    this.executorService = executorService;
  }

  /**
   * We are always returning true from this method even if exception occurred. If we return false that means we are do
   * not ack the message and it would be delivered to the same consumer group again. This can lead to double
   * notifications
   */

  @Override
  public boolean handleMessage(Message message) {
    long startTs = System.currentTimeMillis();
    if (isProcessable(message)) {
      try {
        log.info(
            "[PMS_SDK] Starting to process {} event with messageId: {}", entityClass.getSimpleName(), message.getId());

        executorService.submit(() -> {
          T entity = extractEntity(message);
          Long issueTimestamp = ProtoUtils.timestampToUnixMillis(message.getTimestamp());
          processMessage(entity, message.getMessage().getMetadataMap(), issueTimestamp);
        });

        log.info("[PMS_SDK] Processing Finished for {} event with messageId: {}", entityClass.getSimpleName(),
            message.getId());
      } catch (Exception ex) {
        log.info("[PMS_SDK] Exception occurred while processing {} event with messageId: {}",
            entityClass.getSimpleName(), message.getId());
      }
    }
    Duration processDuration = Duration.ofMillis(System.currentTimeMillis() - startTs);
    if (THRESHOLD_PROCESS_DURATION.compareTo(processDuration) < 0) {
      log.warn("[PMS_SDK] Processing for {} event took {}s which is more than threshold of {}s",
          entityClass.getSimpleName(), processDuration.getSeconds(), THRESHOLD_PROCESS_DURATION.getSeconds());
    }
    return true;
  }

  @VisibleForTesting
  T extractEntity(@NonNull Message message) {
    try {
      return (T) entityClass.getMethod("parseFrom", ByteString.class).invoke(null, message.getMessage().getData());
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new InvalidRequestException(
          String.format("Exception in unpacking %s for key %s", entityClass.getSimpleName(), message.getId()), e);
    }
  }

  public boolean isProcessable(Message message) {
    if (message != null && message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      return metadataMap != null && metadataMap.get(SERVICE_NAME) != null
          && serviceName.equals(metadataMap.get(SERVICE_NAME));
    }
    return false;
  }

  /**
   * The boolean that we are returning here is just for logging purposes we should infer nothing from the responses
   */
  public void processMessage(T event, Map<String, String> metadataMap, Long timestamp) {
    handler.handleEvent(event, metadataMap, timestamp);
  }
}
