package io.harness.pms.events.base;

import io.harness.observer.Subject;
import io.harness.queue.EventListenerObserver;

import lombok.Getter;

public abstract class PmsAbstractBaseMessageListenerWithObservers<T extends com.google.protobuf.Message>
    extends PmsAbstractMessageListener<T> {
  @Getter private final Subject<EventListenerObserver> eventListenerObserverSubject = new Subject<>();

  public PmsAbstractBaseMessageListenerWithObservers(String serviceName, Class<T> entityClass) {
    super(serviceName, entityClass);
  }

  @Override
  public boolean processMessage(T event) {
    eventListenerObserverSubject.fireInform(EventListenerObxxxxxxxx:onListenerStart, event);
    boolean successful = processMessageInternal(event);
    eventListenerObserverSubject.fireInform(EventListenerObxxxxxxxx:onListenerEnd, event);
    return successful;
  }

  public abstract boolean processMessageInternal(T event);
}
