package io.harness.ccm.graphql.dto.common;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TimeSeriesDataPoints {
  List<DataPoint> values;
  Long time;
}
