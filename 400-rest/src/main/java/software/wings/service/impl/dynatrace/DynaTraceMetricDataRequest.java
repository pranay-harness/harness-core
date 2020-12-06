package software.wings.service.impl.dynatrace;

import software.wings.service.impl.dynatrace.DynaTraceTimeSeries.DynaTraceAggregationType;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 2/6/18.
 */
@Data
@Builder
public class DynaTraceMetricDataRequest {
  private String timeseriesId;
  private Set<String> entities;
  private DynaTraceAggregationType aggregationType;
  private Integer percentile;
  private long startTimestamp;
  private long endTimestamp;
}
