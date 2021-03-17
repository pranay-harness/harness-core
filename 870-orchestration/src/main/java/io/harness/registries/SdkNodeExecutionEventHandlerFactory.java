package io.harness.registries;

import io.harness.event.handlers.QueueNodeExecutionEventHandler;
import io.harness.event.handlers.SdkResponseEventHandler;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

@Singleton
public class SdkNodeExecutionEventHandlerFactory {
  @Inject Injector injector;

  public SdkResponseEventHandler getHandler(SdkResponseEventType eventType) {
    if (eventType == SdkResponseEventType.QUEUE_NODE) {
      return injector.getInstance(QueueNodeExecutionEventHandler.class);
    }
    throw new InvalidRequestException("Unknown sdkResponseEventType.");
  }
}
