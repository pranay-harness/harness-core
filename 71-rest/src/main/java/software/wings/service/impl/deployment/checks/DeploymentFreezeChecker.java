package software.wings.service.impl.deployment.checks;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.service.intfc.deployment.PreDeploymentChecker;

@Singleton
public class DeploymentFreezeChecker implements PreDeploymentChecker {
  private GovernanceConfigService governanceConfigService;

  @Inject
  public DeploymentFreezeChecker(GovernanceConfigService governanceConfigService) {
    this.governanceConfigService = governanceConfigService;
  }

  @Override
  public void check(String accountId, String appId) {
    GovernanceConfig governanceConfig = governanceConfigService.get(accountId);
    if (governanceConfig == null) {
      return;
    }

    if (governanceConfig.isDeploymentFreeze()) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "Deployment Freeze is active. No deployments are allowed.");
    }
  }
}
