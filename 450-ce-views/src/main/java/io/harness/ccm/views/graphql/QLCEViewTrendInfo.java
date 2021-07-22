package io.harness.ccm.views.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLCEViewTrendInfo {
  String statsLabel;
  String statsDescription;
  String statsValue;
  Number statsTrend;
  Number value;
}
