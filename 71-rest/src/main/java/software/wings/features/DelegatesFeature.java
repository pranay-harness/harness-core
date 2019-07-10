package software.wings.features;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.scheduler.PersistentScheduler;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.features.api.AbstractUsageLimitedFeature;
import software.wings.features.api.ComplianceByLimitingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.scheduler.DelegateDeletionJob;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;

import java.util.List;
import java.util.Map;

@Singleton
public class DelegatesFeature extends AbstractUsageLimitedFeature implements ComplianceByLimitingUsage {
  public static final String FEATURE_NAME = "DELEGATES";

  private final PersistentScheduler jobScheduler;
  private final DelegateService delegateService;

  @Inject
  public DelegatesFeature(AccountService accountService, FeatureRestrictions featureRestrictions,
      @Named("BackgroundJobScheduler") PersistentScheduler jobScheduler, DelegateService delegateService) {
    super(accountService, featureRestrictions);
    this.jobScheduler = jobScheduler;
    this.delegateService = delegateService;
  }

  @Override
  public int getMaxUsageAllowed(String accountType) {
    return (int) getRestrictions(accountType).getOrDefault("maxDelegatesAllowed", Integer.MAX_VALUE);
  }

  @Override
  public int getUsage(String accountId) {
    return getCurrentDelegateCount(accountId);
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  @Override
  public boolean limitUsageForCompliance(
      String accountId, String targetAccountType, Map<String, Object> requiredInfoToLimitUsage) {
    if (isUsageCompliantWithRestrictions(accountId, targetAccountType)) {
      return true;
    }

    if (!getAccountType(accountId).equals(targetAccountType)) {
      return true;
    }

    @SuppressWarnings("unchecked")
    List<String> delegatesToRetain = (List<String>) requiredInfoToLimitUsage.get("delegatesToRetain");

    if (!isEmpty(delegatesToRetain)) {
      DelegateDeletionJob.addWithDelay(jobScheduler, accountId, delegatesToRetain.get(0), 30);
    }

    return true;
  }

  private int getCurrentDelegateCount(String accountId) {
    return delegateService
        .list(PageRequestBuilder.aPageRequest().addFilter(DelegateKeys.accountId, EQ, accountId).build())
        .size();
  }
}
