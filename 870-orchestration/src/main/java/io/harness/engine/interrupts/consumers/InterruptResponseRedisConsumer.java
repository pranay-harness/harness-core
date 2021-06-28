package io.harness.engine.interrupts.consumers;

import static io.harness.OrchestrationEventsFrameworkConstants.SDK_INTERRUPT_RESPONSE_CONSUMER;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class InterruptResponseRedisConsumer extends PmsAbstractRedisConsumer<InterruptResponseMessageListener> {
  @Inject
  public InterruptResponseRedisConsumer(@Named(SDK_INTERRUPT_RESPONSE_CONSUMER) Consumer redisConsumer,
      InterruptResponseMessageListener messageListener, @Named("pmsEventsCache") Cache<String, Integer> eventsCache) {
    super(redisConsumer, messageListener, eventsCache);
  }
}
