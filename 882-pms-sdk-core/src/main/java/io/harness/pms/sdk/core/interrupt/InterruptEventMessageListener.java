package io.harness.pms.sdk.core.interrupt;

import static io.harness.govern.Switch.noop;
import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.eventsframework.consumer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AutoLogContext;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.execution.utils.InterruptEventUtils;
import io.harness.pms.sdk.PmsSdkModuleUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterruptEventMessageListener implements MessageListener {
  @Inject @Named(PmsSdkModuleUtils.SDK_SERVICE_NAME) String serviceName;
  @Inject InterruptEventListenerHelper interruptEventListenerHelper;

  @Override
  public boolean handleMessage(Message message) {
    if (message != null && message.hasMessage()) {
      Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      if (metadataMap != null && metadataMap.get(SERVICE_NAME) != null
          && serviceName.equals(metadataMap.get(SERVICE_NAME))) {
        InterruptEvent interruptEvent;
        try {
          interruptEvent = InterruptEvent.parseFrom(message.getMessage().getData());
        } catch (InvalidProtocolBufferException e) {
          throw new InvalidRequestException(
              String.format("Exception in unpacking InterruptEvent for key %s", message.getId()), e);
        }
        processMessage(interruptEvent);
      }
    }
    return true;
  }

  private void processMessage(InterruptEvent event) {
    try (AutoLogContext ignore = InterruptEventUtils.obtainLogContext(event)) {
      InterruptType interruptType = event.getType();
      switch (interruptType) {
        case ABORT:
          interruptEventListenerHelper.handleAbort(event.getNodeExecution(), event.getNotifyId());
          break;
        case CUSTOM_FAILURE:
          interruptEventListenerHelper.handleFailure(
              event.getNodeExecution(), event.getMetadata(), event.getInterruptUuid(), event.getNotifyId());
          break;
        default:
          log.warn("No Handling present for Interrupt Event of type : {}", interruptType);
          noop();
      }
    }
  }
}
