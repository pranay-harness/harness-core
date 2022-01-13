/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.configuration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

/**
 * Enum to identify the deploymode. Hazelcast , delegate may use this enum for their custom behaviors in respective
 * modes
 */
public enum DeployMode {
  AWS("SAAS"),
  ONPREM("ONPREM"),
  KUBERNETES("SAAS"),
  KUBERNETES_ONPREM("ONPREM");

  private String deployedAs;

  public static final String DEPLOY_MODE = "DEPLOY_MODE";

  DeployMode(String deployedAs) {
    this.deployedAs = deployedAs;
  }

  public String getDeployedAs() {
    return deployedAs;
  }

  public static boolean isOnPrem(String deployMode) {
    if (isEmpty(deployMode)) {
      return false;
    }

    return ONPREM.name().equals(deployMode) || KUBERNETES_ONPREM.name().equals(deployMode);
  }
}
