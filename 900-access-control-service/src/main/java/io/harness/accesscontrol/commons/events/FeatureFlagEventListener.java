package io.harness.accesscontrol.commons.events;

import static io.harness.AuthorizationServiceHeader.ACCESS_CONTROL_SERVICE;
import static io.harness.eventsframework.EventsFrameworkConstants.FEATURE_FLAG_STREAM;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.lock.redis.RedisPersistentLocker;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Set;

@OwnedBy(HarnessTeam.PL)
public class FeatureFlagEventListener extends EventListener {
  @Inject
  public FeatureFlagEventListener(@Named(FEATURE_FLAG_STREAM) Consumer redisConsumer,
      @Named(FEATURE_FLAG_STREAM) Set<EventConsumer> eventConsumers, RedisPersistentLocker redisPersistentLocker) {
    super(redisConsumer, eventConsumers, redisPersistentLocker,
        ACCESS_CONTROL_SERVICE.getServiceId().concat(FEATURE_FLAG_STREAM));
  }

  @Override
  public String getListenerName() {
    return FEATURE_FLAG_STREAM;
  }
}
