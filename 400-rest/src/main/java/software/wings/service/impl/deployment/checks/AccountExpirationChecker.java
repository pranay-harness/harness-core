/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.deployment.checks;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;

import software.wings.licensing.LicenseService;
import software.wings.service.intfc.deployment.PreDeploymentChecker;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.annotation.ParametersAreNonnullByDefault;

@OwnedBy(PL)
@Singleton
@ParametersAreNonnullByDefault
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
  public void check(String accountId) {
    boolean isAccountExpired = licenseService.isAccountExpired(accountId);
    if (isAccountExpired) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "Your license has expired! Please contact Harness Support.");
    }
  }
}
