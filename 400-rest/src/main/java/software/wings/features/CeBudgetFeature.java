/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features;

import io.harness.ccm.budget.dao.BudgetDao;

import software.wings.features.api.AbstractUsageLimitedCeFeature;
import software.wings.features.api.ComplianceByLimitingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import java.util.Map;

public class CeBudgetFeature extends AbstractUsageLimitedCeFeature implements ComplianceByLimitingUsage {
  public static final String FEATURE_NAME = "CE_BUDGETS";
  private final BudgetDao budgetDao;

  @Inject
  public CeBudgetFeature(AccountService accountService, FeatureRestrictions featureRestrictions, BudgetDao budgetDao) {
    super(accountService, featureRestrictions);
    this.budgetDao = budgetDao;
  }

  @Override
  public boolean limitUsageForCompliance(
      String accountId, String targetAccountType, Map<String, Object> requiredInfoToLimitUsage) {
    return false;
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  @Override
  public int getMaxUsageAllowed(String accountType) {
    return (int) getRestrictions(accountType).getOrDefault("maxBudgetsAllowed", Integer.MAX_VALUE);
  }

  @Override
  public int getUsage(String accountId) {
    return budgetDao.list(accountId).size();
  }
}
