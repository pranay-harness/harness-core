package io.harness.batch.processing.pricing.data;

public enum CloudProvider {
  AWS("amazon"),
  GCP("google");

  CloudProvider(String cloudProviderName) {
    this.cloudProviderName = cloudProviderName;
  }

  private final String cloudProviderName;

  public String getCloudProviderName() {
    return cloudProviderName;
  }
}
