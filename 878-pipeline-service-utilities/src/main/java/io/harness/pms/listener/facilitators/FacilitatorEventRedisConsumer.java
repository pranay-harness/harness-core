package io.harness.pms.listener.facilitators;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_FACILITATOR_CONSUMER;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.PT_FACILITATOR_LISTENER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
@Singleton
public class FacilitatorEventRedisConsumer extends PmsAbstractRedisConsumer {
  @Inject
  public FacilitatorEventRedisConsumer(@Named(PT_FACILITATOR_CONSUMER) Consumer redisConsumer,
      @Named(PT_FACILITATOR_LISTENER) MessageListener messageListener) {
    super(redisConsumer, messageListener);
  }
}
