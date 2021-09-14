/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
