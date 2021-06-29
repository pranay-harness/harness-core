package io.harness.pms.sdk.execution.events.facilitators;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.sdk.execution.events.PmsSdkEventFrameworkConstants.PT_FACILITATOR_CONSUMER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
@Singleton
public class FacilitatorEventRedisConsumer extends PmsAbstractRedisConsumer<FacilitatorEventMessageListener> {
  @Inject
  public FacilitatorEventRedisConsumer(@Named(PT_FACILITATOR_CONSUMER) Consumer redisConsumer,
      FacilitatorEventMessageListener messageListener, @Named("sdkEventsCache") Cache<String, Integer> eventsCache) {
    super(redisConsumer, messageListener, eventsCache);
  }
}
