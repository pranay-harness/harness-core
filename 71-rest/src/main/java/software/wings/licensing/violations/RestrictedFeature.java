package software.wings.licensing.violations;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import software.wings.licensing.violations.checkers.APIKeyViolationChecker;
import software.wings.licensing.violations.checkers.ApprovalStepViolationChecker;
import software.wings.licensing.violations.checkers.DelegateViolationChecker;
import software.wings.licensing.violations.checkers.FlowControlViolationChecker;
import software.wings.licensing.violations.checkers.GovernanceViolationChecker;
import software.wings.licensing.violations.checkers.IpWhitelistViolationChecker;
import software.wings.licensing.violations.checkers.SSOViolationChecker;
import software.wings.licensing.violations.checkers.TemplateLibraryViolationChecker;
import software.wings.licensing.violations.checkers.TwoFactorAuthenticationViolationChecker;
import software.wings.licensing.violations.checkers.UserGroupsViolationChecker;
import software.wings.licensing.violations.checkers.UsersViolationChecker;

@Getter
@ToString
public enum RestrictedFeature {
  USERS(UsersViolationChecker.class),
  FLOW_CONTROL(FlowControlViolationChecker.class),
  TWO_FACTOR_AUTHENTICATION(TwoFactorAuthenticationViolationChecker.class),
  USER_GROUPS(UserGroupsViolationChecker.class),
  API_KEYS(APIKeyViolationChecker.class),
  SSO(SSOViolationChecker.class),
  TEMPLATE_LIBRARY(TemplateLibraryViolationChecker.class),
  IP_WHITELIST(IpWhitelistViolationChecker.class),
  GOVERNANCE(GovernanceViolationChecker.class),
  DELEGATE(DelegateViolationChecker.class),
  APPROVAL_STEP(ApprovalStepViolationChecker.class);

  private final Class<? extends FeatureViolationChecker> violationsCheckerClass;

  RestrictedFeature(@NonNull Class<? extends FeatureViolationChecker> violationsCheckerClass) {
    this.violationsCheckerClass = violationsCheckerClass;
  }
}
