/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.k8s.data;

import io.harness.exception.DataException;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class K8sCanaryDataException extends DataException {
  String canaryWorkload;
  boolean canaryWorkloadDeployed;

  @Builder(builderMethodName = "dataBuilder")
  public K8sCanaryDataException(String canaryWorkload, boolean canaryWorkloadDeployed, Throwable cause) {
    super(cause);
    this.canaryWorkload = canaryWorkload;
    this.canaryWorkloadDeployed = canaryWorkloadDeployed;
  }
}
