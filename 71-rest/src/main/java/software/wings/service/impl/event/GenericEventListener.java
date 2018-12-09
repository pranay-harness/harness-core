package software.wings.service.impl.event;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Singleton;

import io.harness.event.handler.EventHandler;
import io.harness.event.listener.EventListener;
import io.harness.event.model.EventType;
import io.harness.event.model.QueableEvent;
import io.harness.queue.QueueListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * @author rktummala on 11/20/2018
 */
@Singleton
public class GenericEventListener extends QueueListener<QueableEvent> implements EventListener {
  private static final Logger logger = LoggerFactory.getLogger(GenericEventListener.class);

  private Multimap<EventType, EventHandler> handlerRegistry;

  //  @Inject private EventHandler eventHandler;

  public GenericEventListener() {
    super(false);
    handlerRegistry = getMultimap();
  }

  private Multimap<EventType, EventHandler> getMultimap() {
    HashMultimap<EventType, EventHandler> hashMultimap = HashMultimap.create();
    return Multimaps.synchronizedSetMultimap(hashMultimap);
    //    return hashMultimap;
  }

  @Override
  public void onMessage(QueableEvent event) {
    Collection<EventHandler> eventHandlers = handlerRegistry.get(event.getEvent().getEventType());

    if (isEmpty(eventHandlers)) {
      return;
    }

    eventHandlers.forEach(eventHandler -> {
      if (eventHandler == null) {
        return;
      }

      try {
        eventHandler.handleEvent(event.getEvent());
      } catch (Exception ex) {
        logger.error("Error while handling event for type {}", event.getEvent().getEventType(), ex);
      }
    });
  }

  @Override
  public void registerEventHandler(EventHandler handler, Set<EventType> eventTypes) {
    eventTypes.forEach(eventType -> handlerRegistry.put(eventType, handler));
  }

  @Override
  public void deregisterEventHandler(EventHandler handler, Set<EventType> eventTypes) {
    eventTypes.forEach(eventType -> handlerRegistry.remove(eventType, handler));
  }
}
