package io.harness.eventframework.manager;

import static io.harness.eventsframework.EventsFrameworkConstants.OBSERVER_EVENT_CHANNEL;

import io.harness.AuthorizationServiceHeader;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.observer.RemoteObserver;
import io.harness.observer.consumer.AbstractRemoteObserverEventConsumer;
import io.harness.observer.consumer.RemoteObserverProcessor;
import io.harness.queue.QueueController;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Set;

@OwnedBy(HarnessTeam.DEL)
public class ManagerRemoteObserverEventConsumer extends AbstractRemoteObserverEventConsumer {
  @Inject
  public ManagerRemoteObserverEventConsumer(@Named(OBSERVER_EVENT_CHANNEL) Consumer redisConsumer,
      Set<RemoteObserver> remoteObservers, QueueController queueController,
      RemoteObserverProcessor remoteObserverProcessor) {
    super(redisConsumer, remoteObservers, queueController, remoteObserverProcessor);
  }

  @Override
  public String getServicePrincipal() {
    return AuthorizationServiceHeader.DMS.getServiceId();
  }
}
