package software.wings.features.api;

import com.google.inject.Inject;

import software.wings.service.intfc.AccountService;

import java.util.Collection;

public abstract class AbstractPremiumFeature extends AbstractRestrictedFeature implements PremiumFeature {
  @Inject
  public AbstractPremiumFeature(AccountService accountService, FeatureRestrictions featureRestrictions) {
    super(accountService, featureRestrictions);
  }

  @Override
  public boolean isAvailableForAccount(String accountId) {
    return isAvailable(getAccountType(accountId));
  }

  @Override
  public boolean isUsageCompliantWithRestrictions(String accountId, String targetAccountType) {
    return isAvailable(targetAccountType) || !isBeingUsed(accountId);
  }

  @Override
  public FeatureUsageComplianceReport getUsageComplianceReport(String accountId, String targetAccountType) {
    Collection<Usage> disallowedUsages = getDisallowedUsages(accountId, targetAccountType);

    return FeatureUsageComplianceReport.builder()
        .featureName(getFeatureName())
        .property("isUsageCompliantWithRestrictions", isUsageCompliantWithRestrictions(accountId, targetAccountType))
        .property("isAvailable", isAvailable(targetAccountType))
        .property("isBeingUsed", isBeingUsed(accountId))
        .property("disallowedUsages", disallowedUsages)
        .property("disallowedUsagesCount", disallowedUsages.size())
        .build();
  }

  @Override
  public boolean isAvailable(String accountType) {
    return (boolean) getRestrictions(accountType).getOrDefault("available", true);
  }
}
