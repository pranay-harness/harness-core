package io.harness.accesscontrol.commons.events;

import static io.harness.AuthorizationServiceHeader.ACCESS_CONTROL_SERVICE;
import static io.harness.eventsframework.EventsFrameworkConstants.USERMEMBERSHIP;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.lock.redis.RedisPersistentLocker;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Set;

@OwnedBy(HarnessTeam.PL)
public class UserMembershipEventListener extends EventListener {
  @Inject
  public UserMembershipEventListener(@Named(USERMEMBERSHIP) Consumer redisConsumer,
      @Named(USERMEMBERSHIP) Set<EventConsumer> eventConsumers, RedisPersistentLocker redisPersistentLocker) {
    super(redisConsumer, eventConsumers, redisPersistentLocker,
        ACCESS_CONTROL_SERVICE.getServiceId().concat(USERMEMBERSHIP));
  }

  @Override
  public String getListenerName() {
    return USERMEMBERSHIP;
  }
}
