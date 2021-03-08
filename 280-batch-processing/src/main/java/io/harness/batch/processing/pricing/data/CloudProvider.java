package io.harness.batch.processing.pricing.data;

public enum CloudProvider {
  AWS("amazon"),
  AZURE("azure"),
  GCP("google"),
  IBM("ibm"),
  ON_PREM("onprem"),
  UNKNOWN("unknown");

  CloudProvider(String cloudProviderName) {
    this.cloudProviderName = cloudProviderName;
  }

  private final String cloudProviderName;

  public String getCloudProviderName() {
    return cloudProviderName;
  }

  public static CloudProvider fromCloudProviderName(String cloudProviderName) {
    try {
      return CloudProvider.valueOf(cloudProviderName);
    } catch (IllegalArgumentException e) {
      return CloudProvider.UNKNOWN;
    }
  }
}
