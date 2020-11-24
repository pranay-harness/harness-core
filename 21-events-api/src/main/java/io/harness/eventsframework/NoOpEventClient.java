package io.harness.eventsframework;

import java.util.Map;

public class NoOpEventClient implements EventDrivenClient {
  @Override
  public boolean createConsumerGroup(StreamChannel channel, String groupName) {
    return false;
  }

  @Override
  public void publishEvent(StreamChannel channel, Event event) {}

  @Override
  public Map<String, Event> readEvent(StreamChannel channel) {
    return null;
  }

  @Override
  public Map<String, Event> readEvent(StreamChannel channel, String lastId) {
    return null;
  }

  @Override
  public Map<String, Event> readEvent(StreamChannel channel, String groupName, String consumerName) {
    return null;
  }

  @Override
  public Map<String, Event> readEvent(StreamChannel channel, String groupName, String consumerName, String lastId) {
    return null;
  }

  @Override
  public void acknowledge(StreamChannel channel, String groupName, String messageId) {}
}
