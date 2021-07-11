package io.harness.accesscontrol.commons.events;

import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.queue.QueueController;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class EntityCrudEventListener extends EventListener {
  @Inject
  public EntityCrudEventListener(@Named(ENTITY_CRUD) Consumer redisConsumer,
      @Named(ENTITY_CRUD) Set<EventConsumer> eventConsumers, QueueController queueController) {
    super(redisConsumer, eventConsumers, queueController);
  }

  @Override
  public String getListenerName() {
    return ENTITY_CRUD;
  }
}
