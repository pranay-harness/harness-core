package io.harness.cvng.core.services.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class MetricCVConfig extends CVConfig {
  private List<MetricPack> metricPacks;
}
