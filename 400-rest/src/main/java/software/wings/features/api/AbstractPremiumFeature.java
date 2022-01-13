/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features.api;

import static software.wings.beans.AccountType.allAccountTypes;

import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractPremiumFeature extends AbstractRestrictedFeature implements PremiumFeature {
  @Inject
  public AbstractPremiumFeature(AccountService accountService, FeatureRestrictions featureRestrictions) {
    super(accountService, featureRestrictions);
  }

  @Override
  public Set<String> getRestrictedAccountTypes() {
    return allAccountTypes.stream().filter(accountType -> !isAvailable(accountType)).collect(Collectors.toSet());
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
