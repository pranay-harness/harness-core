package io.harness.eventsframework;

import java.util.Map;

public interface EventDrivenClient {
  boolean createConsumerGroup(StreamChannel channel, String groupName);
  void publishEvent(StreamChannel channel, Event event);
  Map<String, Event> readEvent(StreamChannel channel);
  Map<String, Event> readEvent(StreamChannel channel, String lastId);
  Map<String, Event> readEvent(StreamChannel channel, String groupName, String consumerName);
  Map<String, Event> readEvent(StreamChannel channel, String groupName, String consumerName, String lastId);
  void acknowledge(StreamChannel channel, String groupName, String messageId);
}
