package io.harness.feature.bases;

import io.harness.feature.interfaces.FeatureUsage;

public abstract class FeatureLimit extends FeatureBase<String> implements FeatureUsage<Long> {
  @Override
  public String getRestriction(String accountIdentifier) {
    // get license edition
    String edition = "TEAM";
    return restrictions.getOrDefault(edition, "unlimited");
  }

  @Override
  public boolean isAvailable(String accountIdentifier) {
    String restriction = getRestriction(accountIdentifier);
    if ("unlimited".equalsIgnoreCase(restriction)) {
      return true;
    }

    if ("disabled".equalsIgnoreCase(restriction)) {
      return false;
    }

    Long longRestriction = Long.valueOf(restriction);
    return getUsage(accountIdentifier).compareTo(longRestriction) < 0;
  }
}
