package io.harness.pms.sdk.execution.events.plan;

import static io.harness.pms.sdk.execution.events.PmsSdkEventFrameworkConstants.PT_START_PLAN_CREATION_EVENT_CONSUMER;

import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;
import io.harness.queue.QueueController;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import javax.cache.Cache;

public class CreatePartialPlanRedisConsumer extends PmsAbstractRedisConsumer<CreatePartialPlanMessageListener> {
  @Inject
  public CreatePartialPlanRedisConsumer(@Named(PT_START_PLAN_CREATION_EVENT_CONSUMER) Consumer redisConsumer,
      CreatePartialPlanMessageListener messageListener, @Named("sdkEventsCache") Cache<String, Integer> eventsCache,
      QueueController queueController) {
    super(redisConsumer, messageListener, eventsCache, queueController);
  }
}
