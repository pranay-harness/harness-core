package io.harness.cvng.core.beans.monitoredService.healthSouceSpec;

import io.harness.cvng.beans.MetricResponseMappingDTO;

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

  public MetricResponseMappingDTO toDto() {
    return MetricResponseMappingDTO.builder()
        .metricValueJsonPath(metricValueJsonPath)
        .serviceInstanceJsonPath(serviceInstanceJsonPath)
        .timestampJsonPath(timestampJsonPath)
        .timestampFormat(timestampFormat)
        .build();
  }
}
