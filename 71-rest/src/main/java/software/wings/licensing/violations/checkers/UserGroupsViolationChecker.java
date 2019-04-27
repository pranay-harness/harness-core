package software.wings.licensing.violations.checkers;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;

import software.wings.beans.FeatureEnabledViolation;
import software.wings.beans.FeatureViolation;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.UserGroupService;

import java.util.Collections;
import java.util.List;

public class UserGroupsViolationChecker implements FeatureViolationChecker {
  @Inject private UserGroupService userGroupService;

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    int userGroupsCount = getUserGroupsCount(accountId);
    if (userGroupsCount > 1) {
      return Collections.singletonList(FeatureEnabledViolation.builder()
                                           .restrictedFeature(RestrictedFeature.USER_GROUPS)
                                           .usageCount(userGroupsCount)
                                           .build());
    }

    return Collections.emptyList();
  }

  private int getUserGroupsCount(String accountId) {
    return userGroupService.list(accountId, aPageRequest().build(), false).getResponse().size();
  }
}
