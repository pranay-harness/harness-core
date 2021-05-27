package io.harness.pms.sdk.core.interrupt;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.events.PmsEventFrameworkConstants.INTERRUPT_CONSUMER;
import static io.harness.pms.events.PmsEventFrameworkConstants.INTERRUPT_LISTENER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.sdk.core.execution.events.base.SdkBaseRedisConsumer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class InterruptEventRedisConsumer extends SdkBaseRedisConsumer {
  @Inject
  public InterruptEventRedisConsumer(@Named(INTERRUPT_CONSUMER) Consumer redisConsumer,
      @Named(INTERRUPT_LISTENER) MessageListener interruptEventMessageListener) {
    super(redisConsumer, interruptEventMessageListener);
  }
}
