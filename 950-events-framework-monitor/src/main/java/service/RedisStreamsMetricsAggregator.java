/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.monitor.dto.AggregateRedisStreamMetricsDTO;

@OwnedBy(HarnessTeam.PL)
public interface RedisStreamsMetricsAggregator {
  AggregateRedisStreamMetricsDTO getStreamStats();
}
