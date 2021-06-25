package io.harness.engine.interrupts.consumers;

import static io.harness.OrchestrationEventsFrameworkConstants.SDK_INTERRUPT_EVENT_NOTIFY_CONSUMER;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class SdkInterruptEventNotifyRedisConsumer
    extends PmsAbstractRedisConsumer<SdkInterruptEventNotifyMessageListener> {
  @Inject
  public SdkInterruptEventNotifyRedisConsumer(@Named(SDK_INTERRUPT_EVENT_NOTIFY_CONSUMER) Consumer redisConsumer,
      SdkInterruptEventNotifyMessageListener messageListener) {
    super(redisConsumer, messageListener);
  }
}
