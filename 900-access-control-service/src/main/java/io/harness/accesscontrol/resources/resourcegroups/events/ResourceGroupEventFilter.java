package io.harness.accesscontrol.resources.resourcegroups.events;

import static io.harness.accesscontrol.resources.resourcegroups.events.ResourceGroupEventConsumer.RESOURCE_GROUP_ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.UPDATE_ACTION;

import io.harness.eventsframework.api.EventFilter;
import io.harness.eventsframework.consumer.Message;

import com.google.inject.Singleton;
import java.util.Map;

@Singleton
public class ResourceGroupEventFilter implements EventFilter {
  @Override
  public boolean filter(Message message) {
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap == null || !metadataMap.containsKey(ACTION) || !metadataMap.containsKey(ENTITY_TYPE)) {
      return false;
    }
    String entityType = metadataMap.get(ENTITY_TYPE);
    String action = metadataMap.get(ACTION);
    return RESOURCE_GROUP_ENTITY_TYPE.equals(entityType)
        && (UPDATE_ACTION.equals(action) || DELETE_ACTION.equals(action));
  }
}
