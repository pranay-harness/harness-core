/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.dto.perspectives;

import io.harness.ccm.graphql.dto.common.StatsInfo;
import io.harness.ccm.views.graphql.EfficiencyScoreStats;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerspectiveTrendStats {
  StatsInfo cost;
  StatsInfo idleCost;
  StatsInfo unallocatedCost;
  StatsInfo systemCost;
  StatsInfo utilizedCost;
  EfficiencyScoreStats efficiencyScoreStats;
}
