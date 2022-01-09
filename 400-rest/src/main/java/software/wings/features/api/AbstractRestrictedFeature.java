/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features.api;

import software.wings.beans.AccountType;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import java.util.Map;

public abstract class AbstractRestrictedFeature implements RestrictedFeature {
  protected final Map<String, Restrictions> restrictionsByAccountType;
  protected AccountService accountService;

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
