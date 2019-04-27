package software.wings.licensing.violations;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import software.wings.licensing.violations.checkers.APIKeyViolationChecker;
import software.wings.licensing.violations.checkers.DelegateViolationChecker;
import software.wings.licensing.violations.checkers.FlowControlViolationChecker;
import software.wings.licensing.violations.checkers.SSOViolationChecker;
import software.wings.licensing.violations.checkers.UsersViolationChecker;

@Getter
@ToString
public enum RestrictedFeature {
  USERS(UsersViolationChecker.class),
  FLOW_CONTROL(FlowControlViolationChecker.class),
  API_KEYS(APIKeyViolationChecker.class),
  SSO(SSOViolationChecker.class),
  DELEGATE(DelegateViolationChecker.class);

  private final Class<? extends FeatureViolationChecker> violationsCheckerClass;

  RestrictedFeature(@NonNull Class<? extends FeatureViolationChecker> violationsCheckerClass) {
    this.violationsCheckerClass = violationsCheckerClass;
  }
}
