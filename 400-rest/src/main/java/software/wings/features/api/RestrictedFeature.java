package software.wings.features.api;

public interface RestrictedFeature extends Feature {
  Restrictions getRestrictionsForAccount(String accountId);

  Restrictions getRestrictions(String accountType);

  boolean isUsageCompliantWithRestrictions(String accountId, String targetAccountType);

  FeatureUsageComplianceReport getUsageComplianceReport(String accountId, String targetAccountType);
}
