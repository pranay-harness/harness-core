package io.harness.accesscontrol.principals.serviceaccounts.events;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.SERVICE_ACCOUNT_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.UPDATE_ACTION;

import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;

import com.google.inject.Singleton;
import java.util.Map;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class ServiceAccountEventFilter implements EventFilter {
  @Override
  public boolean filter(Message message) {
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap == null || !metadataMap.containsKey(ACTION) || !metadataMap.containsKey(ENTITY_TYPE)) {
      return false;
    }
    String entityType = metadataMap.get(ENTITY_TYPE);
    String action = metadataMap.get(ACTION);
    return SERVICE_ACCOUNT_ENTITY.equals(entityType) && (UPDATE_ACTION.equals(action) || DELETE_ACTION.equals(action));
  }
}
