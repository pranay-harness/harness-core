/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
