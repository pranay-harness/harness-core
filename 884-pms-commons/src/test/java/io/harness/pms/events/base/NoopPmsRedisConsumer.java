package io.harness.pms.events.base;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cache.NoOpCache;
import io.harness.eventsframework.api.Consumer;

@OwnedBy(HarnessTeam.PIPELINE)
public class NoopPmsRedisConsumer extends PmsAbstractRedisConsumer<NoopPmsMessageListener> {
  public NoopPmsRedisConsumer(Consumer redisConsumer, NoopPmsMessageListener messageListener) {
    super(redisConsumer, messageListener, new NoOpCache<>(), new NoopQueueController());
  }
}
