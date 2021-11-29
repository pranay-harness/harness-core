package io.harness.observer.consumer;

import io.harness.eventsframework.consumer.Message;
import io.harness.observer.RemoteObserver;

import java.util.Map;

public interface RemoteObserverProcessor {
  boolean process(Message message, Map<String, RemoteObserver> remoteObserverMap);
}
