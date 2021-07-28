package io.harness.pms.sdk.execution.events.interrupts;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.sdk.execution.events.PmsSdkEventFrameworkConstants.PT_INTERRUPT_CONSUMER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;
import io.harness.queue.QueueController;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class InterruptEventRedisConsumer extends PmsAbstractRedisConsumer<InterruptEventMessageListener> {
  @Inject
  public InterruptEventRedisConsumer(@Named(PT_INTERRUPT_CONSUMER) Consumer redisConsumer,
      InterruptEventMessageListener interruptEventMessageListener,
      @Named("sdkEventsCache") Cache<String, Integer> eventsCache, QueueController queueController) {
    super(redisConsumer, interruptEventMessageListener, eventsCache, queueController);
  }
}
