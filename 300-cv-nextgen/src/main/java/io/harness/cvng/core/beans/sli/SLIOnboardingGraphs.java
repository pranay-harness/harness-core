package io.harness.cvng.core.beans.sli;

import io.harness.cvng.core.beans.TimeGraphResponse;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Value
@Builder
public class SLIOnboardingGraphs {
  TimeGraphResponse sliGraph;
  Map<String, MetricGraph> metricGraphs;

  @Value
  @SuperBuilder
  public static class MetricGraph extends TimeGraphResponse {
    private String metricName;
    private String metricIdentifier;
  }
}
