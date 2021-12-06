package io.harness.observer.consumer;

import io.harness.eventsframework.consumer.Message;
import io.harness.observer.RemoteObserver;

import java.util.Set;

public interface RemoteObserverProcessor {
  boolean process(Message message, Set<RemoteObserver> remoteObserverMap);
}
