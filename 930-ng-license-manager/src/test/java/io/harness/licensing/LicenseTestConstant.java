package io.harness.licensing;

import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.CIModuleLicense;

public class LicenseTestConstant {
  public static final String DEFAULT_ACCOUNT_UUID = "1";
  public static final String ACCOUNT_IDENTIFIER = "account";
  public static final ModuleType DEFAULT_MODULE_TYPE = ModuleType.CI;
  public static final int TOTAL_DEVELOPER = 10;
  public static final int MAX_DEVELOPER = 12;
  public static final ModuleLicenseDTO DEFAULT_CI_MODULE_LICENSE_DTO = CIModuleLicenseDTO.builder()
                                                                           .id("id")
                                                                           .numberOfCommitters(TOTAL_DEVELOPER)
                                                                           .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                                           .licenseType(LicenseType.TRIAL)
                                                                           .moduleType(DEFAULT_MODULE_TYPE)
                                                                           .edition(Edition.ENTERPRISE)
                                                                           .status(LicenseStatus.ACTIVE)
                                                                           .startTime(1)
                                                                           .expiryTime(1)
                                                                           .createdAt(0L)
                                                                           .lastModifiedAt(0L)
                                                                           .build();

  public static final CIModuleLicense DEFAULT_CI_MODULE_LICENSE =
      CIModuleLicense.builder().numberOfCommitters(TOTAL_DEVELOPER).build();
  static {
    DEFAULT_CI_MODULE_LICENSE.setId("id");
    DEFAULT_CI_MODULE_LICENSE.setAccountIdentifier(ACCOUNT_IDENTIFIER);
    DEFAULT_CI_MODULE_LICENSE.setModuleType(DEFAULT_MODULE_TYPE);
    DEFAULT_CI_MODULE_LICENSE.setEdition(Edition.ENTERPRISE);
    DEFAULT_CI_MODULE_LICENSE.setStatus(LicenseStatus.ACTIVE);
    DEFAULT_CI_MODULE_LICENSE.setLicenseType(LicenseType.TRIAL);
    DEFAULT_CI_MODULE_LICENSE.setStartTime(1);
    DEFAULT_CI_MODULE_LICENSE.setExpiryTime(1);
    DEFAULT_CI_MODULE_LICENSE.setCreatedAt(0L);
    DEFAULT_CI_MODULE_LICENSE.setLastUpdatedAt(0L);
  }
}
