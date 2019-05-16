package software.wings.licensing.violations.checkers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.FeatureEnabledViolation;
import software.wings.beans.FeatureViolation;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.compliance.GovernanceConfigService;

import java.util.Collections;
import java.util.List;

@Singleton
public class GovernanceViolationChecker implements FeatureViolationChecker {
  @Inject private GovernanceConfigService governanceConfigService;

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    GovernanceConfig config = governanceConfigService.get(accountId);
    boolean isViolation = config != null && config.isDeploymentFreeze();

    if (isViolation) {
      return Collections.singletonList(new FeatureEnabledViolation(RestrictedFeature.GOVERNANCE, 1));
    } else {
      return Collections.emptyList();
    }
  }
}
