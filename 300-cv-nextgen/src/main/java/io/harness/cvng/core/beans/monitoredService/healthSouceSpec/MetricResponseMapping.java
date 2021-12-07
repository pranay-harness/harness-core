package io.harness.cvng.core.beans.monitoredService.healthSouceSpec;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MetricResponseMapping {
  String metricValueJsonPath;
  String timestampJsonPath;
  String serviceInstanceJsonPath;
  String timestampFormat;
}
