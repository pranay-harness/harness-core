package software.wings.service.impl.deployment.checks;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.sun.istack.Nullable;
import io.harness.exception.WingsException;
import software.wings.licensing.LicenseService;
import software.wings.service.intfc.deployment.PreDeploymentChecker;

@Singleton
public class AccountExpirationChecker implements PreDeploymentChecker {
  private final LicenseService licenseService;

  @Inject
  public AccountExpirationChecker(LicenseService licenseService) {
    this.licenseService = licenseService;
  }

  /**
   * CHecks if an account's license is expired.
   */
  @Override
  public void check(String accountId, @Nullable String appId) {
    boolean isAccountExpired = licenseService.isAccountExpired(accountId);
    if (isAccountExpired) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "License expired!!! Please contact Harness Support.");
    }
  }
}
