package io.harness.batch.processing.anomalydetection;

import io.harness.batch.processing.anomalydetection.types.AnomalyDetectionModel;
import io.harness.batch.processing.anomalydetection.types.AnomalyType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Anomaly extends AnomalyDetectionInfo {
  private String id;
  private Double actualCost;
  private Double expectedCost;
  private Instant time;
  private boolean isAnomaly;
  private double anomalyScore;
  private AnomalyType anomalyType;
  private AnomalyDetectionModel reportedBy;

  private boolean relativeThreshold;
  private boolean absoluteThreshold;
  private boolean probabilisticThreshold;
}
