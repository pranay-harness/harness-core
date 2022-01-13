/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kubernetes.client.custom.IntOrString;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class RollingDeploymentStrategyParams {
  private Long intervalSeconds;
  private IntOrString maxSurge;
  private IntOrString maxUnavailable;
  private LifecycleHook post;
  private LifecycleHook pre;
  private Long timeoutSeconds;
  private Long updatePeriodSeconds;
}
