/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.k8s.exception;

import static io.harness.eraro.ErrorCode.KUBERNETES_CLUSTER_ERROR;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class K8sClusterException extends WingsException {
  private static final String REASON_ARG = "reason";

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public K8sClusterException(String reason) {
    super(null, null, KUBERNETES_CLUSTER_ERROR, Level.ERROR, null, null);
    super.param(REASON_ARG, reason);
  }
}
