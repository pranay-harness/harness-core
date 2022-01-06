/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
