package io.harness.queue;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.observer.Subject;

import lombok.Getter;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class QueueListenerWithObservers<T extends Queuable> extends QueueListener<T> {
  @Getter private final Subject<QueueListenerObserver> queueListenerObserverSubject = new Subject<>();

  public QueueListenerWithObservers(QueueConsumer<T> queueConsumer, boolean primaryOnly) {
    super(queueConsumer, primaryOnly);
  }

  @Override
  public void onMessage(T message) {
    try {
      onMessageInternal(message);
    } finally {
      queueListenerObserverSubject.fireInform(QueueListenerObxxxxxxxx:onListenerEnd, message);
    }
  }

  public abstract void onMessageInternal(T message);
}
