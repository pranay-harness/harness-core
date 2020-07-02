package io.harness.cvng.core.entities;

import io.harness.cvng.models.VerificationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class MetricCVConfig extends CVConfig {
  private MetricPack metricPack;

  @Override
  public VerificationType getVerificationType() {
    return VerificationType.TIME_SERIES;
  }
}
