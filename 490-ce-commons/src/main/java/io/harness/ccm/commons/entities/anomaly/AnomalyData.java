/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
