package io.harness.accesscontrol.resources.resourcegroups.events;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.RESOURCE_GROUP;

import io.harness.accesscontrol.commons.events.EventConsumer;
import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.accesscontrol.commons.events.EventHandler;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class ResourceGroupEventConsumer implements EventConsumer {
  private final ResourceGroupEventFilter resourceGroupEventFilter;
  private final ResourceGroupEventHandler resourceGroupEventHandler;
  public static final String RESOURCE_GROUP_ENTITY_TYPE = RESOURCE_GROUP;

  @Inject
  public ResourceGroupEventConsumer(
      ResourceGroupEventFilter resourceGroupEventFilter, ResourceGroupEventHandler resourceGroupEventHandler) {
    this.resourceGroupEventFilter = resourceGroupEventFilter;
    this.resourceGroupEventHandler = resourceGroupEventHandler;
  }

  @Override
  public EventFilter getEventFilter() {
    return resourceGroupEventFilter;
  }

  @Override
  public EventHandler getEventHandler() {
    return resourceGroupEventHandler;
  }
}
