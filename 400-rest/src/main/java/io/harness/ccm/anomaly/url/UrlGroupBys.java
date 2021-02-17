package io.harness.ccm.anomaly.url;

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
