/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queue;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.observer.Subject;

import java.util.HashMap;
import lombok.Getter;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class QueueListenerWithObservers<T extends Queuable> extends QueueListener<T> {
  @Getter private final Subject<EventListenerObserver> eventListenerObserverSubject = new Subject<>();

  public QueueListenerWithObservers(QueueConsumer<T> queueConsumer, boolean primaryOnly) {
    super(queueConsumer, primaryOnly);
  }

  @Override
  public void onMessage(T message) {
    eventListenerObserverSubject.fireInform((eventListenerObserver1, message2)
                                                -> eventListenerObserver1.onListenerStart(message2, new HashMap<>()),
        message);
    try {
      onMessageInternal(message);
    } finally {
      eventListenerObserverSubject.fireInform(
          (eventListenerObserver, message1) -> eventListenerObserver.onListenerEnd(message1, new HashMap<>()), message);
    }
  }

  public abstract void onMessageInternal(T message);
}
