package software.wings.features;

import io.harness.ccm.budget.BudgetService;

import software.wings.features.api.AbstractUsageLimitedCeFeature;
import software.wings.features.api.ComplianceByLimitingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import java.util.Map;

public class CeBudgetFeature extends AbstractUsageLimitedCeFeature implements ComplianceByLimitingUsage {
  public static final String FEATURE_NAME = "CE_BUDGETS";
  private final BudgetService budgetService;

  @Inject
  public CeBudgetFeature(
      AccountService accountService, FeatureRestrictions featureRestrictions, BudgetService budgetService) {
    super(accountService, featureRestrictions);
    this.budgetService = budgetService;
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
    return budgetService.getBudgetCount(accountId);
  }
}
