/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.aggregator;

import java.util.Optional;

public interface AggregatorMetricsService {
  Optional<SnapshotMetrics> getSnapshotMetrics();

  Optional<StreamingMetrics> getStreamingMetrics();
}
