package io.harness.ccm.graphql.dto.perspectives;

import io.harness.ccm.graphql.dto.common.TimeSeriesDataPoints;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerspectiveTimeSeriesData {
  List<TimeSeriesDataPoints> stats;
}
