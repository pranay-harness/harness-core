package io.harness.pms.sdk.execution.events.orchestrationevent;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.sdk.execution.events.PmsSdkEventFrameworkConstants.PT_ORCHESTRATION_EVENT_CONSUMER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class OrchestrationEventRedisConsumer extends PmsAbstractRedisConsumer<OrchestrationEventMessageListener> {
  @Inject
  public OrchestrationEventRedisConsumer(@Named(PT_ORCHESTRATION_EVENT_CONSUMER) Consumer redisConsumer,
      OrchestrationEventMessageListener sdkOrchestrationEventMessageListener,
      @Named("sdkEventsCache") Cache<String, Integer> eventsCache) {
    super(redisConsumer, sdkOrchestrationEventMessageListener, eventsCache);
  }
}
