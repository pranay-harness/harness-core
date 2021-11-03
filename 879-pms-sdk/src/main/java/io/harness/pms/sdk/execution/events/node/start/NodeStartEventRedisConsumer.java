package io.harness.pms.sdk.execution.events.node.start;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;
import io.harness.queue.QueueController;

import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class NodeStartEventRedisConsumer extends PmsAbstractRedisConsumer<NodeStartEventMessageListener> {
  public static NodeStartEventRedisConsumer getInstance(Consumer redisConsumer,
      NodeStartEventMessageListener messageListener, Cache<String, Integer> eventsCache,
      QueueController queueController) {
    return new NodeStartEventRedisConsumer(redisConsumer, messageListener, eventsCache, queueController);
  }

  private NodeStartEventRedisConsumer(Consumer redisConsumer, NodeStartEventMessageListener messageListener,
      Cache<String, Integer> eventsCache, QueueController queueController) {
    super(redisConsumer, messageListener, eventsCache, queueController);
  }
}
