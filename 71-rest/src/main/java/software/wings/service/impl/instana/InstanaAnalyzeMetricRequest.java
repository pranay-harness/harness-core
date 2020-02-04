package software.wings.service.impl.instana;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// this request class can be used for both trace and call metrics.
// https://instana.github.io/openapi/#operation/getCallGroup
// https://instana.github.io/openapi/#operation/getTraceGroups
@Data
@Builder
public class InstanaAnalyzeMetricRequest {
  private InstanaTimeFrame timeFrame;
  private Group group;
  private List<InstanaTagFilter> tagFilters;
  private List<Metric> metrics;
  @Data
  @Builder
  public static class Group {
    private String groupByTag;
    private String groupbyTagSecondLevelKey;
  }
  @Data
  @Builder
  public static class Metric {
    private String metric;
    private String aggregation;
    private long granularity;
  }
}
