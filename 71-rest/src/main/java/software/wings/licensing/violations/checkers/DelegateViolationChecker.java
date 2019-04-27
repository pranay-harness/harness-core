package software.wings.licensing.violations.checkers;

import com.google.inject.Inject;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.SearchFilter.Operator;
import software.wings.beans.AccountType;
import software.wings.beans.Delegate;
import software.wings.beans.FeatureUsageLimitExceededViolation;
import software.wings.beans.FeatureViolation;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.DelegateService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

public class DelegateViolationChecker implements FeatureViolationChecker {
  private static final int MAX_DELEGATE_ALLOWED_IN_PAID = Integer.MAX_VALUE;
  private static final int MAX_DELEGATE_ALLOWED_IN_TRIAL = Integer.MAX_VALUE;
  private static final int MAX_DELEGATE_ALLOWED_IN_COMMUNITY = 1;

  private static final Map<String, Integer> maxDelegatesAllowed = new HashMap<String, Integer>() {
    {
      put(AccountType.TRIAL, MAX_DELEGATE_ALLOWED_IN_TRIAL);
      put(AccountType.COMMUNITY, MAX_DELEGATE_ALLOWED_IN_COMMUNITY);
      put(AccountType.PAID, MAX_DELEGATE_ALLOWED_IN_PAID);
    }
  };

  private DelegateService delegateService;

  @Inject
  public DelegateViolationChecker(DelegateService delegateService) {
    this.delegateService = delegateService;
  }

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(@NotNull String accountId) {
    int currentDelegateCount =
        delegateService
            .list(PageRequestBuilder.aPageRequest().addFilter(Delegate.ACCOUNT_ID_KEY, Operator.EQ, accountId).build())
            .size();

    int allowedNumberOfDelegates = maxDelegatesAllowed.get(AccountType.COMMUNITY);

    if (currentDelegateCount > allowedNumberOfDelegates) {
      return Collections.singletonList(FeatureUsageLimitExceededViolation.builder()
                                           .restrictedFeature(RestrictedFeature.DELEGATE)
                                           .paidLicenseUsageLimit(MAX_DELEGATE_ALLOWED_IN_PAID)
                                           .usageCount(currentDelegateCount)
                                           .usageLimit(allowedNumberOfDelegates)
                                           .build());
    }

    return Collections.emptyList();
  }
}
