/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.apm;

import software.wings.APMFetchConfig;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.states.APMVerificationState.MetricCollectionInfo;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class APMSetupTestNodeData extends SetupTestNodeData {
  APMFetchConfig fetchConfig;
  MetricCollectionInfo apmMetricCollectionInfo;
  String host;
}
