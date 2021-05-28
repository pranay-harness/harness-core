package io.harness.pms.sdk.core.execution.events.base;

import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.eventsframework.consumer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.sdk.PmsSdkModuleUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SdkBaseEventMessageListener<T extends com.google.protobuf.Message> implements MessageListener {
  @Inject @Named(PmsSdkModuleUtils.SDK_SERVICE_NAME) String serviceName;

  private final Class<T> entityClass;

  public SdkBaseEventMessageListener(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  @Override
  public boolean handleMessage(Message message) {
    if (isProcessable(message)) {
      log.info("[PMS_SDK] Starting to process message from {} messageId: {}", this.getClass().getSimpleName(),
          message.getId());
      boolean processed = processMessage(extractEntity(message));
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
          String.format("Exception in unpacking InterruptEvent for key %s", message.getId()), e);
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

  public abstract boolean processMessage(T event);
}
