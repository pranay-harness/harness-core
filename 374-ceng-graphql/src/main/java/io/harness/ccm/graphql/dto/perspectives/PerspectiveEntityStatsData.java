package io.harness.ccm.graphql.dto.perspectives;

import io.harness.ccm.views.graphql.QLCEViewEntityStatsDataPoint;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerspectiveEntityStatsData {
  List<QLCEViewEntityStatsDataPoint> data;
}
