package io.harness.accesscontrol.commons.events;

import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PL)
public class EntityCrudEventListenerService extends EventListenerService {
  @Inject
  public EntityCrudEventListenerService(EntityCrudEventListener entityCrudEventListener) {
    super(entityCrudEventListener);
  }

  @Override
  public String getServiceName() {
    return ENTITY_CRUD;
  }
}
