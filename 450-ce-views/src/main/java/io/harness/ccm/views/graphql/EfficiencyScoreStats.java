package io.harness.ccm.views.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EfficiencyScoreStats {
  String statsLabel;
  String statsValue;
  Number statsTrend;
}
