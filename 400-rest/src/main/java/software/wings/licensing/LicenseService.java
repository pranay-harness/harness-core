/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.licensing;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.license.CeLicenseInfo;

import software.wings.beans.Account;
import software.wings.beans.LicenseInfo;

import java.util.List;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.GTM)
@TargetModule(HarnessModule._945_ACCOUNT_MGMT)
public interface LicenseService {
  String LICENSE_INFO = "LICENSE_INFO";

  void checkForLicenseExpiry(Account account);

  boolean updateAccountLicense(@NotEmpty String accountId, LicenseInfo licenseInfo);

  boolean updateCeLicense(@NotEmpty String accountId, CeLicenseInfo ceLicenseInfo);

  boolean startCeLimitedTrial(@NotEmpty String accountId);

  Account updateAccountSalesContacts(@NotEmpty String accountId, List<String> salesContacts);

  void updateAccountLicenseForOnPrem(String encryptedLicenseInfoBase64String);

  boolean isAccountDeleted(String accountId);

  boolean isAccountExpired(String accountId);

  void validateLicense(String accountId, String operation);

  void setLicense(Account account);

  boolean updateLicenseForProduct(String productCode, String accountId, Integer orderQuantity, long expirationTime);
}
