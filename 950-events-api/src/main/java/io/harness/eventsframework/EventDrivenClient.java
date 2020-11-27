package io.harness.eventsframework;

import java.util.List;
import java.util.Map;

public interface EventDrivenClient {
  boolean createConsumerGroup(String channel, String groupName);
  void publishEvent(String channel, Event event);
  Map<String, Event> readEvent(String channel);
  Map<String, Event> readEvent(String channel, String lastId);
  Map<String, Event> readEvent(String channel, String groupName, String consumerName);
  Map<String, Event> readEvent(String channel, String groupName, String consumerName, String lastId);
  void acknowledge(String channel, String groupName, String messageId);
  long deleteMessages(String channel, List<String> messageIds);
}
