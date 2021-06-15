package io.harness.ccm.graphql.dto.common;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StatsInfo {
  String statsLabel;
  String statsDescription;
  String statsValue;
  Number statsTrend;
  Number value;
}
