package io.harness.ccm.dto.graphql.recommendation;

import io.leangen.graphql.annotations.GraphQLNonNull;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RecommendationItemDTO {
  @GraphQLNonNull @NotNull String id;
  String clusterName;
  String resourceName;
  double monthlySaving;
  double monthlyCost;
  @GraphQLNonNull @NotNull ResourceType resourceType;
  RecommendationDetailsDTO recommendationDetails;
}