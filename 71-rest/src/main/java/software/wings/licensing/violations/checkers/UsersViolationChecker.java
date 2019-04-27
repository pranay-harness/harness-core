package software.wings.licensing.violations.checkers;

import com.google.inject.Inject;

import io.harness.data.structure.CollectionUtils;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureUsageLimitExceededViolation;
import software.wings.beans.FeatureViolation;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.UserService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersViolationChecker implements FeatureViolationChecker {
  private static final int MAX_USERS_ALLOWED_IN_PAID = Integer.MAX_VALUE;
  private static final int MAX_USERS_ALLOWED_IN_TRIAL = Integer.MAX_VALUE;
  private static final int MAX_USERS_ALLOWED_IN_COMMUNITY = 5;

  private static final Map<String, Integer> maxUsersAllowedByAccountType = getMaxUsersAllowedByAccountType();

  @Inject private UserService userService;

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    int currentNumberOfUsers = userService.getUsersOfAccount(accountId).size();
    int allowedNumberOfUsers = maxUsersAllowedByAccountType.get(AccountType.COMMUNITY);

    List<FeatureViolation> featureViolationList = null;
    if (currentNumberOfUsers > allowedNumberOfUsers) {
      featureViolationList = Collections.singletonList(FeatureUsageLimitExceededViolation.builder()
                                                           .restrictedFeature(RestrictedFeature.USERS)
                                                           .paidLicenseUsageLimit(MAX_USERS_ALLOWED_IN_PAID)
                                                           .usageCount(currentNumberOfUsers)
                                                           .usageLimit(allowedNumberOfUsers)
                                                           .build());
    }

    return CollectionUtils.emptyIfNull(featureViolationList);
  }

  private static Map<String, Integer> getMaxUsersAllowedByAccountType() {
    Map<String, Integer> maxUsersAllowedByAccountType = new HashMap<>();

    maxUsersAllowedByAccountType.put(AccountType.TRIAL, MAX_USERS_ALLOWED_IN_TRIAL);
    maxUsersAllowedByAccountType.put(AccountType.COMMUNITY, MAX_USERS_ALLOWED_IN_COMMUNITY);
    maxUsersAllowedByAccountType.put(AccountType.PAID, MAX_USERS_ALLOWED_IN_PAID);

    return maxUsersAllowedByAccountType;
  }
}
