package software.wings.features.api;

import com.google.inject.Inject;

import software.wings.beans.AccountType;
import software.wings.service.intfc.AccountService;

import java.util.Map;

public abstract class AbstractRestrictedFeature implements RestrictedFeature {
  protected final Map<String, Restrictions> restrictionsByAccountType;
  protected final AccountService accountService;

  @Inject
  public AbstractRestrictedFeature(AccountService accountService, FeatureRestrictions featureRestrictions) {
    this.accountService = accountService;
    this.restrictionsByAccountType = featureRestrictions.getRestrictionsByAccountType(getFeatureName());
  }

  protected String getAccountType(String accountId) {
    return accountService.getAccountType(accountId).orElse(AccountType.PAID);
  }

  @Override
  public Restrictions getRestrictionsForAccount(String accountId) {
    return getRestrictions(getAccountType(accountId));
  }

  @Override
  public Restrictions getRestrictions(String accountType) {
    return restrictionsByAccountType.getOrDefault(accountType, new Restrictions());
  }

  @Override
  public String toString() {
    return String.format(
        "{featureName = %s, restrictionsByAccountType = %s}", getFeatureName(), restrictionsByAccountType);
  }
}
