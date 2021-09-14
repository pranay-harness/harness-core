/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.dto.recommendation;

import io.harness.ccm.commons.beans.recommendation.ResourceType;

import io.leangen.graphql.annotations.GraphQLNonNull;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RecommendationItemDTO {
  @GraphQLNonNull @NotNull String id;
  String clusterName;
  String namespace;
  String resourceName;
  Double monthlySaving;
  Double monthlyCost;
  @GraphQLNonNull @NotNull ResourceType resourceType;
  RecommendationDetailsDTO recommendationDetails;
}
