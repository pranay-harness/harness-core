package io.harness.accesscontrol.resources.resourcegroups.events;

import com.google.inject.Singleton;
import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.eventsframework.consumer.Message;

import java.util.Map;

import static io.harness.accesscontrol.resources.resourcegroups.events.ResourceGroupEventConsumer.RESOURCE_GROUP_ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.UPDATE_ACTION;

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
        && (UPDATE_ACTION.equals(action) || DELETE_ACTION.equals(action) || CREATE_ACTION.equals(action));
  }
}
