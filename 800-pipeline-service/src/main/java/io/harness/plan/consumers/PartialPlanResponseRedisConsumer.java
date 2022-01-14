package io.harness.plan.consumers;

import static io.harness.OrchestrationEventsFrameworkConstants.PARTIAL_PLAN_EVENT_CONSUMER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;
import io.harness.queue.QueueController;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import javax.cache.Cache;

@OwnedBy(HarnessTeam.PIPELINE)
public class PartialPlanResponseRedisConsumer
    extends PmsAbstractRedisConsumer<PartialPlanResponseEventMessageListener> {
  @Inject
  public PartialPlanResponseRedisConsumer(@Named(PARTIAL_PLAN_EVENT_CONSUMER) Consumer redisConsumer,
      PartialPlanResponseEventMessageListener sdkResponseMessageListener,
      @Named("pmsEventsCache") Cache<String, Integer> eventsCache, QueueController queueController) {
    super(redisConsumer, sdkResponseMessageListener, eventsCache, queueController);
  }
}
