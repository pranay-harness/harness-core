/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
