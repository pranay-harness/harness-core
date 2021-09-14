/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.metrics;

import java.time.Instant;

public interface CeCloudMetricsService {
  double getTotalCloudCost(String accountId, String cloudProviderType, Instant start, Instant end);
}
