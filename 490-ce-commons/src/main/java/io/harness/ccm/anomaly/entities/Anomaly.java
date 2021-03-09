package io.harness.ccm.anomaly.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Anomaly extends AnomalyEntity {
  boolean isAnomaly;
  AnomalyType anomalyType;

  boolean relativeThreshold;
  boolean absoluteThreshold;
  boolean probabilisticThreshold;
}
