package io.harness.ccm.anomaly.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AnomalyKeys")
@SuperBuilder
public class Anomaly extends AnomalyEntity {
  public Double getAnomalousCost() {
    return actualCost - expectedCost;
  }
}
