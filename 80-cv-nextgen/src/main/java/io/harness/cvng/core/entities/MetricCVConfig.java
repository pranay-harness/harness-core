package io.harness.cvng.core.entities;

import io.harness.cvng.core.services.entities.MetricPack;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class MetricCVConfig extends CVConfig {
  private MetricPack metricPack;
}
