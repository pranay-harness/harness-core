package software.wings.licensing.violations.checkers;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureEnabledViolation;
import software.wings.beans.FeatureViolation;
import software.wings.beans.security.access.Whitelist;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.WhitelistService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IpWhitelistViolationChecker implements FeatureViolationChecker {
  private static final int MAX_USERS_ALLOWED_IN_PAID = Integer.MAX_VALUE;
  private static final int MAX_USERS_ALLOWED_IN_TRIAL = Integer.MAX_VALUE;
  private static final int MAX_USERS_ALLOWED_IN_COMMUNITY = 0;

  private static final Map<String, Integer> allowedUsageByAccountType = allowedUsageByAccountType();

  @Inject private WhitelistService whitelistService;

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    PageRequest<Whitelist> request = aPageRequest().addFilter(ACCOUNT_ID_KEY, EQ, accountId).build();

    int currentCount = whitelistService.list(accountId, request).size();
    int allowedCount = allowedUsageByAccountType.get(AccountType.COMMUNITY);

    if (currentCount > allowedCount) {
      return Collections.singletonList(FeatureEnabledViolation.builder()
                                           .restrictedFeature(RestrictedFeature.IP_WHITELIST)
                                           .usageCount(currentCount)
                                           .build());
    }

    return Collections.emptyList();
  }

  private static Map<String, Integer> allowedUsageByAccountType() {
    Map<String, Integer> allowedUsageByAccountType = new HashMap<>();

    allowedUsageByAccountType.put(AccountType.TRIAL, MAX_USERS_ALLOWED_IN_TRIAL);
    allowedUsageByAccountType.put(AccountType.COMMUNITY, MAX_USERS_ALLOWED_IN_COMMUNITY);
    allowedUsageByAccountType.put(AccountType.PAID, MAX_USERS_ALLOWED_IN_PAID);

    return allowedUsageByAccountType;
  }
}
