package io.harness.ccm.graphql.dto.perspectives;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerspectiveOverviewStatsData {
  Boolean unifiedTableDataPresent;
  Boolean isAwsOrGcpOrClusterConfigured;
}
