package io.harness.pms.sdk.execution.events.interrupts;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.sdk.execution.events.PmsUtilityConsumerConstants.PT_INTERRUPT_CONSUMER;
import static io.harness.pms.sdk.execution.events.PmsUtilityConsumerConstants.PT_INTERRUPT_LISTENER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class InterruptEventRedisConsumer extends PmsAbstractRedisConsumer {
  @Inject
  public InterruptEventRedisConsumer(@Named(PT_INTERRUPT_CONSUMER) Consumer redisConsumer,
      @Named(PT_INTERRUPT_LISTENER) MessageListener interruptEventMessageListener) {
    super(redisConsumer, interruptEventMessageListener);
  }
}
