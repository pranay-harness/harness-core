package io.harness.ccm.views.entities;

public enum ViewFieldIdentifier {
  CLUSTER("Cluster"),
  AWS("AWS"),
  GCP("GCP"),
  AZURE("Azure"),
  COMMON("Common"),
  CUSTOM("Custom"),
  BUSINESS_MAPPING("Business Mapping"),
  LABEL("Label");

  public String getDisplayName() {
    return displayName;
  }

  private String displayName;

  ViewFieldIdentifier(String displayName) {
    this.displayName = displayName;
  }
}
