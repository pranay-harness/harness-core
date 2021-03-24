package io.harness.accesscontrol.scopes.harness.events;

import static io.harness.accesscontrol.scopes.harness.events.ScopeEventConsumer.SCOPE_EVENT_ENTITY_TYPES;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;

import io.harness.eventsframework.api.EventFilter;
import io.harness.eventsframework.consumer.Message;

import java.util.Map;

public class ScopeEventFilter implements EventFilter {
  @Override
  public boolean filter(Message message) {
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap == null || !metadataMap.containsKey(ACTION) || !metadataMap.containsKey(ENTITY_TYPE)) {
      return false;
    }
    String entityType = metadataMap.get(ENTITY_TYPE);
    String action = metadataMap.get(ACTION);
    return entityType != null && SCOPE_EVENT_ENTITY_TYPES.contains(entityType) && DELETE_ACTION.equals(action);
  }
}
