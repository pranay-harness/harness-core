package io.harness.licensing;

import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.transactions.CILicenseTransactionDTO;
import io.harness.licensing.beans.transactions.LicenseTransactionDTO;
import io.harness.licensing.entities.account.AccountLicense;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.transactions.LicenseTransaction;
import io.harness.licensing.entities.transactions.modules.CILicenseTransaction;

import com.google.common.collect.Lists;
import java.util.Collections;

public class LicenseTestConstant {
  public static final String DEFAULT_ACCOUNT_UUID = "1";
  public static final String DEFAULT_LICENSE_TRANSACTION_UUID = "2";
  public static final String ACCOUNT_IDENTIFIER = "account";
  public static final ModuleType DEFAULT_MODULE_TYPE = ModuleType.CI;
  public static final int TOTAL_DEVELOPER = 10;
  public static final int MAX_DEVELOPER = 12;
  public static final LicenseTransactionDTO DEFAULT_CI_LICENSE_TRANSACTION_DTO =
      CILicenseTransactionDTO.builder()
          .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
          .developers(TOTAL_DEVELOPER)
          .accountIdentifier(ACCOUNT_IDENTIFIER)
          .licenseType(LicenseType.TRIAL)
          .moduleType(ModuleType.CI)
          .status(LicenseStatus.ACTIVE)
          .edition(Edition.ENTERPRISE)
          .startTime(0)
          .expiryTime(0)
          .createdAt(0L)
          .lastModifiedAt(0L)
          .build();
  public static final ModuleLicenseDTO DEFAULT_CI_MODULE_LICENSE_DTO =
      CIModuleLicenseDTO.builder()
          .totalDevelopers(TOTAL_DEVELOPER)
          .maxDevelopers(MAX_DEVELOPER)
          .accountIdentifier(ACCOUNT_IDENTIFIER)
          .licenseType(LicenseType.TRIAL)
          .moduleType(DEFAULT_MODULE_TYPE)
          .edition(Edition.ENTERPRISE)
          .status(LicenseStatus.ACTIVE)
          .createdAt(0L)
          .lastModifiedAt(0L)
          .transactions(Lists.newArrayList(DEFAULT_CI_LICENSE_TRANSACTION_DTO))
          .build();

  public static final AccountLicenseDTO DEFAULT_ACCOUNT_LICENSE_DTO =
      AccountLicenseDTO.builder()
          .uuid(DEFAULT_ACCOUNT_UUID)
          .accountId(ACCOUNT_IDENTIFIER)
          .moduleLicenses(Collections.singletonMap(DEFAULT_MODULE_TYPE, DEFAULT_CI_MODULE_LICENSE_DTO))
          .allInactive(false)
          .build();

  public static final LicenseTransaction DEFAULT_CI_LICENSE_TRANSACTION = CILicenseTransaction.builder()
                                                                              .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
                                                                              .developers(TOTAL_DEVELOPER)
                                                                              .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                                              .licenseType(LicenseType.TRIAL)
                                                                              .moduleType(ModuleType.CI)
                                                                              .status(LicenseStatus.ACTIVE)
                                                                              .edition(Edition.ENTERPRISE)
                                                                              .startTime(0)
                                                                              .expiryTime(0)
                                                                              .createdAt(0L)
                                                                              .lastUpdatedAt(0L)
                                                                              .build();

  public static final CIModuleLicense DEFAULT_CI_MODULE_LICENSE =
      CIModuleLicense.builder().totalDevelopers(10).maxDevelopers(10).build();

  public static final AccountLicense DEFAULT_ACCOUNT_LICENSE =
      AccountLicense.builder()
          .uuid(DEFAULT_ACCOUNT_UUID)
          .accountIdentifier(ACCOUNT_IDENTIFIER)
          .moduleLicenses(Collections.singletonMap(DEFAULT_MODULE_TYPE, DEFAULT_CI_MODULE_LICENSE))
          .allInactive(false)
          .build();
  static {
    DEFAULT_CI_MODULE_LICENSE.setId("id");
    DEFAULT_CI_MODULE_LICENSE.setAccountIdentifier(ACCOUNT_IDENTIFIER);
    DEFAULT_CI_MODULE_LICENSE.setModuleType(DEFAULT_MODULE_TYPE);
    DEFAULT_CI_MODULE_LICENSE.setEdition(Edition.ENTERPRISE);
    DEFAULT_CI_MODULE_LICENSE.setStatus(LicenseStatus.ACTIVE);
    DEFAULT_CI_MODULE_LICENSE.setLicenseType(LicenseType.TRIAL);
    DEFAULT_CI_MODULE_LICENSE.setTransactions(Lists.newArrayList(DEFAULT_CI_LICENSE_TRANSACTION));
  }
}
