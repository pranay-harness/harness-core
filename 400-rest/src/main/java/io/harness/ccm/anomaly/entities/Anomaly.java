package io.harness.ccm.anomaly.entities;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@TargetModule(Module._490_CE_COMMONS)
public class Anomaly extends AnomalyEntity {
  boolean isAnomaly;
  AnomalyType anomalyType;

  boolean relativeThreshold;
  boolean absoluteThreshold;
  boolean probabilisticThreshold;
}
