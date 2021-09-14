/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.ce.recommendation.entity;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerRecommendation {
  ResourceRequirement current;
  @Deprecated ResourceRequirement burstable;
  @Deprecated ResourceRequirement guaranteed;
  @Deprecated ResourceRequirement recommended;
  Map<String, ResourceRequirement> percentileBased;
  Cost lastDayCost;
  int numDays;
  int totalSamplesCount;
}
