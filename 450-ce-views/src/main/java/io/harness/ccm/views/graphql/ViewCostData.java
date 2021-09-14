/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.views.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ViewCostData {
  double cost;
  Double idleCost;
  Double unallocatedCost;
  Double systemCost;
  Double utilizedCost;
  long minStartTime;
  long maxStartTime;
}
