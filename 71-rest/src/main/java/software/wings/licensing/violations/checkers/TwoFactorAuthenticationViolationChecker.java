package software.wings.licensing.violations.checkers;

import com.google.inject.Inject;

import software.wings.beans.FeatureEnabledViolation;
import software.wings.beans.FeatureViolation;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.security.authentication.TwoFactorAuthenticationManager;
import software.wings.service.intfc.UserService;

import java.util.Collections;
import java.util.List;

public class TwoFactorAuthenticationViolationChecker implements FeatureViolationChecker {
  @Inject private TwoFactorAuthenticationManager twoFactorAuthenticationManager;
  @Inject private UserService userService;

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    int numberOfUsersWith2FAEnabled = (int) userService.getUsersWithThisAsPrimaryAccount(accountId)
                                          .stream()
                                          .filter(u -> twoFactorAuthenticationManager.isTwoFactorEnabled(accountId, u))
                                          .count();

    if (twoFactorAuthenticationManager.getTwoFactorAuthAdminEnforceInfo(accountId) || numberOfUsersWith2FAEnabled > 0) {
      return Collections.singletonList(FeatureEnabledViolation.builder()
                                           .restrictedFeature(RestrictedFeature.TWO_FACTOR_AUTHENTICATION)
                                           .usageCount(numberOfUsersWith2FAEnabled)
                                           .build());
    }

    return Collections.emptyList();
  }
}
