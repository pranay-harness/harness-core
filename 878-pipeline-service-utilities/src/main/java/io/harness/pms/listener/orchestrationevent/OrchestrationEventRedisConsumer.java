package io.harness.pms.listener.orchestrationevent;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.ORCHESTRATION_EVENT_CONSUMER;
import static io.harness.pms.listener.PmsUtilityConsumerConstants.ORCHESTRATION_EVENT_LISTENER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class OrchestrationEventRedisConsumer extends PmsAbstractRedisConsumer {
  @Inject
  public OrchestrationEventRedisConsumer(@Named(ORCHESTRATION_EVENT_CONSUMER) Consumer redisConsumer,
      @Named(ORCHESTRATION_EVENT_LISTENER) MessageListener sdkOrchestrationEventMessageListener) {
    super(redisConsumer, sdkOrchestrationEventMessageListener);
  }
}
