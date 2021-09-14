/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.event;

import io.harness.event.model.Event;
import io.harness.event.model.GenericEvent;
import io.harness.event.publisher.EventPublishException;
import io.harness.event.publisher.EventPublisher;
import io.harness.queue.QueuePublisher;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author rktummala on 11/26/18
 */
@Singleton
public class GenericEventPublisher implements EventPublisher {
  @Inject private QueuePublisher<GenericEvent> eventQueue;

  @Override
  public void publishEvent(Event event) throws EventPublishException {
    if (null == event.getEventType()) {
      throw new IllegalArgumentException("eventType can not be null. Event will not be queued. Event: " + event);
    }
    GenericEvent genericEvent = GenericEvent.builder().event(event).build();
    eventQueue.send(genericEvent);
  }
}
