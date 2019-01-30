package software.wings.service.impl.event;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.event.model.Event;
import io.harness.event.model.QueableEvent;
import io.harness.event.publisher.EventPublishException;
import io.harness.event.publisher.EventPublisher;
import io.harness.queue.Queue;

/**
 * @author rktummala on 11/26/18
 */
@Singleton
public class GenericEventPublisher implements EventPublisher {
  @Inject private Queue<QueableEvent> eventQueue;

  @Override
  public void publishEvent(Event event) throws EventPublishException {
    if (null == event.getEventType()) {
      throw new IllegalArgumentException("eventType can not ne bull. Event will not be queued. Event: " + event);
    }
    QueableEvent queableEvent = QueableEvent.builder().event(event).build();
    eventQueue.send(queableEvent);
  }
}
