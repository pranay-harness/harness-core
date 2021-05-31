package io.harness.ccm.graphql.dto.perspectives;

import io.harness.ccm.graphql.dto.common.StatsInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerspectiveTrendStats {
  StatsInfo cost;
}
