/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.event;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.event.handler.EventHandler;
import io.harness.event.listener.EventListener;
import io.harness.event.model.EventType;
import io.harness.event.model.GenericEvent;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author rktummala on 11/20/2018
 */
@Singleton
@Slf4j
public class GenericEventListener extends QueueListener<GenericEvent> implements EventListener {
  private Multimap<EventType, EventHandler> handlerRegistry;

  @Inject
  public GenericEventListener(QueueConsumer<GenericEvent> queueConsumer) {
    super(queueConsumer, false);
    handlerRegistry = getMultimap();
  }

  private Multimap<EventType, EventHandler> getMultimap() {
    HashMultimap<EventType, EventHandler> hashMultimap = HashMultimap.create();
    return Multimaps.synchronizedSetMultimap(hashMultimap);
  }

  @Override
  public void onMessage(GenericEvent event) {
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
        log.error("Error while handling event for type {}", event.getEvent().getEventType(), ex);
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
