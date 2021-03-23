package io.harness.accesscontrol.migrations.events;

import static io.harness.eventsframework.EventsFrameworkConstants.FEATURE_FLAG_STREAM;

import io.harness.accesscontrol.commons.events.EntityCrudEventListener;
import io.harness.accesscontrol.commons.events.EventConsumer;
import io.harness.eventsframework.api.Consumer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeatureFlagEventListener extends EntityCrudEventListener {
  @Inject
  public FeatureFlagEventListener(@Named(FEATURE_FLAG_STREAM) Consumer redisConsumer,
      @Named(FEATURE_FLAG_STREAM) Set<EventConsumer> eventConsumers) {
    super(redisConsumer, eventConsumers);
  }

  @Override
  public String getConsumerName() {
    return FEATURE_FLAG_STREAM;
  }
}
