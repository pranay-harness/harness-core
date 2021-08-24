package io.harness.waiter;

import static io.harness.eventsframework.EventsFrameworkConstants.PMS_ORCHESTRATION_NOTIFY_EVENT;

import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;
import io.harness.queue.QueueController;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import javax.cache.Cache;

public class PmsNotifyEventConsumerRedis extends PmsAbstractRedisConsumer<PmsNotifyEventMessageListener> {
  @Inject
  public PmsNotifyEventConsumerRedis(@Named(PMS_ORCHESTRATION_NOTIFY_EVENT) Consumer redisConsumer,
      PmsNotifyEventMessageListener messageListener, @Named("pmsEventsCache") Cache<String, Integer> eventsCache,
      QueueController queueController) {
    super(redisConsumer, messageListener, eventsCache, queueController);
  }
}
