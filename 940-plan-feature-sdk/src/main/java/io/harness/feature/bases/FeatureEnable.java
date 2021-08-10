package io.harness.feature.bases;

public class FeatureEnable extends FeatureBase<String> {
  @Override
  public String getRestriction(String accountIdentifier) {
    // find license edition
    String edition = "TEAM";
    return restrictions.getOrDefault(edition, "false");
  }

  @Override
  public boolean isAvailable(String accountIdentifier) {
    return Boolean.getBoolean(getRestriction(accountIdentifier));
  }
}
