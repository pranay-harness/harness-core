package software.wings.features.api;

import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;

public abstract class AbstractUsageLimitedFeature extends AbstractRestrictedFeature implements UsageLimitedFeature {
  @Inject
  public AbstractUsageLimitedFeature(AccountService accountService, FeatureRestrictions featureRestrictions) {
    super(accountService, featureRestrictions);
  }

  @Override
  public boolean isUsageCompliantWithRestrictions(String accountId, String targetAccountType) {
    return getUsage(accountId) <= getMaxUsageAllowed(targetAccountType);
  }

  @Override
  public FeatureUsageComplianceReport getUsageComplianceReport(String accountId, String targetAccountType) {
    return FeatureUsageComplianceReport.builder()
        .featureName(getFeatureName())
        .property("isUsageCompliantWithRestrictions", isUsageCompliantWithRestrictions(accountId, targetAccountType))
        .property("isUsageLimited", true)
        .property("maxUsageAllowed", getMaxUsageAllowed(targetAccountType))
        .property("currentUsage", getUsage(accountId))
        .build();
  }

  @Override
  public int getMaxUsageAllowedForAccount(String accountId) {
    return getMaxUsageAllowed(getAccountType(accountId));
  }
}
