package io.harness.ccm.anomaly.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@OwnedBy(CE)
public class Anomaly extends AnomalyEntity {
  boolean isAnomaly;
  AnomalyType anomalyType;

  boolean relativeThreshold;
  boolean absoluteThreshold;
  boolean probabilisticThreshold;
}
