/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.anomaly.url;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public enum UrlGroupBys {
  CLUSTER("Cluster"),
  NAMESPACE("Namespace"),
  WORKLOAD("WorkloadName"),
  GCP_PROJECT("projectId"),
  GCP_PRODUCT("product"),
  GCP_SKU_ID("skuId"),
  AWS_ACCOUNT("awsLinkedAccount"),
  AWS_SERVICE("awsService");

  private String value;
  UrlGroupBys(String param) {
    this.value = param;
  }

  public String getValue() {
    return value;
  }
}
