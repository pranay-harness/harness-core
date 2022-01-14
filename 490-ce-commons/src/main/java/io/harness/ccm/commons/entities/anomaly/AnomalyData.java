package io.harness.ccm.commons.entities.anomaly;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnomalyData {
  String id;
  EntityInfo entity;
  String comment;
  Double anomalyScore;
  Double expectedAmount;
  Double actualAmount;
  Long time;
  AnomalyFeedback userFeedback;
}
