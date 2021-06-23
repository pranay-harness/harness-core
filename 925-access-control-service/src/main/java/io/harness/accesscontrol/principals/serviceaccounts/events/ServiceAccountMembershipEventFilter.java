package io.harness.accesscontrol.principals.serviceaccounts.events;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.SERVICE_ACCOUNT_ENTITY;

import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.eventsframework.consumer.Message;

import com.google.inject.Singleton;
import java.util.Map;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class ServiceAccountMembershipEventFilter implements EventFilter {
  @Override
  public boolean filter(Message message) {
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap == null || !metadataMap.containsKey(ACTION) || !metadataMap.containsKey(ENTITY_TYPE)) {
      return false;
    }
    String entityType = metadataMap.get(ENTITY_TYPE);
    String action = metadataMap.get(ACTION);
    return SERVICE_ACCOUNT_ENTITY.equals(entityType) && (Action.UPDATE.equals(action) || Action.DELETE.equals(action));
  }
}
