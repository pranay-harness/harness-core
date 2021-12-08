package io.harness.ccm.commons.beans.recommendation;

import io.leangen.graphql.annotations.types.GraphQLType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@GraphQLType(name = "RecommendationOverviewStats")
public class RecommendationOverviewStatsDTO {
  double totalMonthlyCost;
  double totalMonthlySaving;
}
