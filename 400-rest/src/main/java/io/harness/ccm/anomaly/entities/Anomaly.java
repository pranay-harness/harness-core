package io.harness.ccm.anomaly.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class Anomaly extends AnomalyEntity {
  boolean isAnomaly;
  AnomalyType anomalyType;

  boolean relativeThreshold;
  boolean absoluteThreshold;
  boolean probabilisticThreshold;
}
