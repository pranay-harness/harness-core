/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.watch;

import io.harness.perpetualtask.PerpetualTaskClientParams;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class K8WatchPerpetualTaskClientParams implements PerpetualTaskClientParams {
  private String cloudProviderId;
  private String clusterId;
  private String clusterName;
}
