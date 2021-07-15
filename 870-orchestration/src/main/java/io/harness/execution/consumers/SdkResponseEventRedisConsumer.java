package io.harness.execution.consumers;

import static io.harness.OrchestrationEventsFrameworkConstants.SDK_RESPONSE_EVENT_CONSUMER;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

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
public class SdkResponseEventRedisConsumer extends PmsAbstractRedisConsumer<SdkResponseEventMessageListener> {
  @Inject
  public SdkResponseEventRedisConsumer(@Named(SDK_RESPONSE_EVENT_CONSUMER) Consumer redisConsumer,
      SdkResponseEventMessageListener sdkResponseMessageListener,
      @Named("pmsEventsCache") Cache<String, Integer> eventsCache, QueueController queueController) {
    super(redisConsumer, sdkResponseMessageListener, eventsCache, queueController);
  }
}
