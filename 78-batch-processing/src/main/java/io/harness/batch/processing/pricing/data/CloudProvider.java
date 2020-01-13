package io.harness.batch.processing.pricing.data;

public enum CloudProvider {
  AWS("amazon"),
  AZURE("azure"),
  GCP("google"),
  UNKNOWN("unknown");

  CloudProvider(String cloudProviderName) {
    this.cloudProviderName = cloudProviderName;
  }

  private final String cloudProviderName;

  public String getCloudProviderName() {
    return cloudProviderName;
  }
}
