/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.dto.recommendation;

import io.harness.ccm.commons.beans.recommendation.ResourceType;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sRecommendationFilterDTO {
  List<String> ids;
  List<String> names;
  List<String> namespaces;
  List<String> clusterNames;
  List<ResourceType> resourceTypes;

  Double minSaving;
  Double minCost;

  Long offset;
  Long limit;
}
