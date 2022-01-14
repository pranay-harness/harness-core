package io.harness.ccm.graphql.dto.common;

import io.leangen.graphql.annotations.GraphQLNonNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StatsInfo {
  @GraphQLNonNull String statsLabel;
  @GraphQLNonNull String statsDescription;
  @GraphQLNonNull String statsValue;
  Number statsTrend;
  Number value;
}
