package io.harness;

import io.harness.monitoring.MonitoringRedisEventObserver;
import io.harness.pms.listener.orchestrationevent.OrchestrationEventMessageListener;

import com.google.inject.Injector;
import com.google.inject.Key;

public class EventObserverUtils {
  public static void registerObservers(Injector injector) {
    OrchestrationEventMessageListener orchestrationEventMessageListener =
        injector.getInstance(OrchestrationEventMessageListener.class);
    orchestrationEventMessageListener.getEventListenerObserverSubject().register(
        injector.getInstance(Key.get(MonitoringRedisEventObserver.class)));
  }
}
