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
public enum UrlParams {
  AGGREGATION_TYPE("aggregationType"),
  CHART_TYPE("chartType"),
  UTILIZATION_AGGREGATION("utilizationAggregationType"),
  FILTER_ON("isFilterOn"),
  GROUP_BY("groupBy"),
  FROM_DATE("from"),
  TO_DATE("to"),

  SHOW_UNALLOCATED("showUnallocated"),
  SHOW_OTHERS("showOthers"),
  CURRENT_VIEW("currentView"),

  CLUSTER_ID("clusterList"),
  NAMESPACE("clusterNamespaceList"),

  GCP_DISCOUNTS("includeGCPDiscounts"),
  GCP_PRODUCT("gcpProductList"),
  GCP_PROJECT("gcpProjectList"),
  GCP_SKU("gcpSKUList"),

  AWS_ACCOUNT("awsAccountList"),
  AWS_SERVICE("awsServiceList"),

  CURRENT_TAB("currentTab"),
  FILTER("filter"),
  SELECTED_VIEW("selectedView");

  private String param;
  UrlParams(String param) {
    this.param = param;
  }

  public String getParam() {
    return param;
  }
}
