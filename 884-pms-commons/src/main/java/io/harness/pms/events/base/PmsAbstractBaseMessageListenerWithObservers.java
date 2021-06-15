package io.harness.pms.events.base;

import io.harness.observer.Subject;
import io.harness.queue.EventListenerObserver;

import java.util.Map;
import lombok.Getter;

public abstract class PmsAbstractBaseMessageListenerWithObservers<T extends com.google.protobuf.Message>
    extends PmsAbstractMessageListener<T> {
  @Getter private final Subject<EventListenerObserver> eventListenerObserverSubject = new Subject<>();

  public PmsAbstractBaseMessageListenerWithObservers(String serviceName, Class<T> entityClass) {
    super(serviceName, entityClass);
  }

  @Override
  public boolean processMessage(T event, Map<String, String> metadataMap) {
    eventListenerObserverSubject.fireInform(
        (eventListenerObserver1, message1) -> eventListenerObserver1.onListenerStart(message1, metadataMap), event);
    boolean successful = processMessageInternal(event);
    eventListenerObserverSubject.fireInform(
        (eventListenerObserver, message) -> eventListenerObserver.onListenerEnd(message, metadataMap), event);
    return successful;
  }

  public abstract boolean processMessageInternal(T event);
}
