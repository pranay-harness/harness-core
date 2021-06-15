package io.harness.pms.events.base;

import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.eventsframework.consumer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.event.MessageListener;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class PmsAbstractMessageListener<T extends com.google.protobuf.Message> implements MessageListener {
  public final String serviceName;
  public final Class<T> entityClass;

  public PmsAbstractMessageListener(String serviceName, Class<T> entityClass) {
    this.serviceName = serviceName;
    this.entityClass = entityClass;
  }

  @Override
  public boolean handleMessage(Message message) {
    if (isProcessable(message)) {
      log.info("[PMS_SDK] Starting to process message from {} messageId: {}", this.getClass().getSimpleName(),
          message.getId());
      boolean processed = processMessage(extractEntity(message), message.getMessage().getMetadataMap());
      log.info("[PMS_SDK] Processing Finished from {} for messageId: {} returning {}", this.getClass().getSimpleName(),
          message.getId(), processed);
      return processed;
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

  public abstract boolean processMessage(T event, Map<String, String> metadataMap);
}
