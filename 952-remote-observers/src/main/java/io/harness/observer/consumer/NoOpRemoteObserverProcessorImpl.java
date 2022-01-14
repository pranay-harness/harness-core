package io.harness.observer.consumer;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.observer.RemoteObserver;

import java.util.Set;

@OwnedBy(HarnessTeam.DEL)
public class NoOpRemoteObserverProcessorImpl implements RemoteObserverProcessor {
  @Override
  public boolean process(Message message, Set<RemoteObserver> remoteObserverMap) {
    return true;
  }
}
