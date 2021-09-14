/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.dto.recommendation;

import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.Cost;

import io.leangen.graphql.annotations.GraphQLQuery;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkloadRecommendationDTO implements RecommendationDetailsDTO {
  @GraphQLQuery(description = "use items.containerRecommendation", deprecationReason = "")
  @Deprecated
  Map<String, ContainerRecommendation> containerRecommendations;
  List<ContainerHistogramDTO> items;
  Cost lastDayCost;
}
