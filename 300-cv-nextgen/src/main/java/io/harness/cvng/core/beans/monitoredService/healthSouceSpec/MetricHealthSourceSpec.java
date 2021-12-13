package io.harness.cvng.core.beans.monitoredService.healthSouceSpec;

import io.harness.cvng.core.beans.HealthSourceMetricDefinition;

import java.util.List;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
public abstract class MetricHealthSourceSpec extends HealthSourceSpec {
  public abstract List<? extends HealthSourceMetricDefinition> getMetricDefinitions();
}
