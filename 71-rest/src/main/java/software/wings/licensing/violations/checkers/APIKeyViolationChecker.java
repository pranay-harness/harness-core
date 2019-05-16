package software.wings.licensing.violations.checkers;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import software.wings.beans.AccountType;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.FeatureEnabledViolation;
import software.wings.beans.FeatureViolation;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.ApiKeyService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class APIKeyViolationChecker implements FeatureViolationChecker {
  private static final int MAX_COUNT_ALLOWED_IN_PAID = Integer.MAX_VALUE;
  private static final int MAX_COUNT_ALLOWED_IN_TRIAL = Integer.MAX_VALUE;
  private static final int MAX_COUNT_ALLOWED_IN_COMMUNITY = 0;

  private static final Map<String, Integer> allowedUsageByAccountType = allowedUsageByAccountType();

  @Inject private ApiKeyService apiKeyService;

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    final PageRequest<ApiKeyEntry> request = aPageRequest().addFilter(ACCOUNT_ID_KEY, EQ, accountId).build();

    final int currentCount = apiKeyService.list(request, accountId).size();
    final int allowedCount = allowedUsageByAccountType.get(AccountType.COMMUNITY);

    if (currentCount > allowedCount) {
      return Collections.singletonList(FeatureEnabledViolation.builder()
                                           .restrictedFeature(RestrictedFeature.API_KEYS)
                                           .usageCount(currentCount)
                                           .build());
    }

    return Collections.emptyList();
  }

  private static Map<String, Integer> allowedUsageByAccountType() {
    final Map<String, Integer> allowedUsageByAccountType = new HashMap<>();

    allowedUsageByAccountType.put(AccountType.TRIAL, MAX_COUNT_ALLOWED_IN_TRIAL);
    allowedUsageByAccountType.put(AccountType.COMMUNITY, MAX_COUNT_ALLOWED_IN_COMMUNITY);
    allowedUsageByAccountType.put(AccountType.PAID, MAX_COUNT_ALLOWED_IN_PAID);

    return allowedUsageByAccountType;
  }
}
