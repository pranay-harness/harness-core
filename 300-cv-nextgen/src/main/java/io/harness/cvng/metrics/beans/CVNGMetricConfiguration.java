package io.harness.cvng.metrics.beans;

import io.opencensus.stats.Measure.MeasureDouble;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
// TODO: Give a better name
public class CVNGMetricConfiguration {
  String name;
  String identifier;
  String metricGroup;
  List<CVNGMetric> metrics;

  @Data
  @Builder
  public static class CVNGMetric {
    String metricName;
    String metricDefinition;
    String unit;
    MeasureDouble measure;
  }
}
