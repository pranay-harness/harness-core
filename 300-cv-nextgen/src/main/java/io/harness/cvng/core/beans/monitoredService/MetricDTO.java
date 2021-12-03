package io.harness.cvng.core.beans.monitoredService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricDTO {
  String identifier;
  String metricName;
}
