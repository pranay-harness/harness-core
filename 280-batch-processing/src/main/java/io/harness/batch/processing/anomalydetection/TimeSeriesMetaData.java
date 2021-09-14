/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.anomalydetection;

import io.harness.ccm.anomaly.entities.EntityType;
import io.harness.ccm.anomaly.entities.TimeGranularity;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class TimeSeriesMetaData {
  String accountId;
  Instant trainStart;
  Instant trainEnd;
  Instant testStart;
  Instant testEnd;
  TimeGranularity timeGranularity;
  EntityType entityType;
  String entityIdentifier;

  K8sQueryMetaData k8sQueryMetaData;
  CloudQueryMetaData cloudQueryMetaData;
}
