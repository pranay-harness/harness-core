/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.model;

public enum ContainerApiVersions {
  KUBERNETES_V1("v1"),
  KUBERNETES_V1_ALPHA1("v1alpha1"),
  KUBERNETES_V2_BETA1("v2beta1");

  private String versionName;

  ContainerApiVersions(String versionName) {
    this.versionName = versionName;
  }

  /**
   * Gets version name.
   *
   * @return the version name
   */
  public String getVersionName() {
    return versionName;
  }
}
