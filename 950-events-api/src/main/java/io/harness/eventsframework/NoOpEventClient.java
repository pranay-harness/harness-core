package io.harness.eventsframework;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NoOpEventClient implements EventDrivenClient {
  @Override
  public boolean createConsumerGroup(String channel, String groupName) {
    return false;
  }

  @Override
  public void publishEvent(String channel, Event event) {}

  @Override
  public Map<String, Event> readEvent(String channel) {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, Event> readEvent(String channel, String lastId) {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, Event> readEvent(String channel, String groupName, String consumerName) {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, Event> readEvent(String channel, String groupName, String consumerName, String lastId) {
    return Collections.emptyMap();
  }

  @Override
  public void acknowledge(String channel, String groupName, String messageId) {}

  @Override
  public long deleteMessages(String channel, List<String> messageIds) {
    return 0;
  }
}
