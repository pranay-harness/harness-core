/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.constants;

import lombok.Getter;

public enum CloudProvider {
  AWS("amazon", "eks"),
  AZURE("azure", "aks"),
  GCP("google", "gke"),
  IBM("ibm", null),
  ON_PREM("onprem", null),
  UNKNOWN("unknown", null);

  @Getter private final String cloudProviderName;
  @Getter private final String k8sService;

  CloudProvider(String cloudProviderName, String k8sService) {
    this.cloudProviderName = cloudProviderName;
    this.k8sService = k8sService;
  }

  public static CloudProvider fromCloudProviderName(String cloudProviderName) {
    if (cloudProviderName == null) {
      return CloudProvider.UNKNOWN;
    }

    try {
      return CloudProvider.valueOf(cloudProviderName);
    } catch (IllegalArgumentException e) {
      return CloudProvider.UNKNOWN;
    }
  }
}
