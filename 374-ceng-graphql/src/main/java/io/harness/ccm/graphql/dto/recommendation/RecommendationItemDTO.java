package io.harness.ccm.graphql.dto.recommendation;

import io.harness.ccm.commons.beans.recommendation.ResourceType;

import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.types.GraphQLType;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@GraphQLType(name = "RecommendationItem")
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