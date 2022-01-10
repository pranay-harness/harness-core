/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
