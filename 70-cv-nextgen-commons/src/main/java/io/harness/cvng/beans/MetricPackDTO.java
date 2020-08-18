package io.harness.cvng.beans;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class MetricPackDTO {
  String accountId;
  String projectIdentifier;
  DataSourceType dataSourceType;
  String identifier;
  Set<MetricDefinitionDTO> metrics;

  @Value
  @Builder
  public static class MetricDefinitionDTO {
    String name;
    TimeSeriesMetricType type;
    String path;
    String validationPath;
    boolean included;
  }
}
