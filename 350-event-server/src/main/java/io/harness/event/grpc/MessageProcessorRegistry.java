package io.harness.event.grpc;

import io.harness.event.MessageProcessorType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;

@Singleton
public class MessageProcessorRegistry {
  @Inject private Map<MessageProcessorType, MessageProcessor> processorMap;

  public MessageProcessor getProcessor(MessageProcessorType type) {
    return processorMap.get(type);
  }
}
