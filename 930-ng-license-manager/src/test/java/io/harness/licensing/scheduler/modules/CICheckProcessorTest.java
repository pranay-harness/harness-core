package io.harness.licensing.scheduler.modules;

import static io.harness.licensing.LicenseTestConstant.ACCOUNT_IDENTIFIER;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_ACCOUNT_UUID;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_LICENSE_TRANSACTION_UUID;
import static io.harness.licensing.LicenseTestConstant.MAX_DEVELOPER;
import static io.harness.licensing.LicenseTestConstant.TOTAL_DEVELOPER;
import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.category.element.UnitTests;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.transactions.modules.CILicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class CICheckProcessorTest {
  @InjectMocks private CICheckProcessor ciCheckProcessor;

  @Before
  public void setup() {
    initMocks(this);
    Instant.now(Clock.fixed(Instant.parse("2018-08-22T10:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithNoChange() {
    CIModuleLicense moduleLicense =
        CIModuleLicense.builder().totalDevelopers(TOTAL_DEVELOPER).maxDevelopers(MAX_DEVELOPER).build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CI);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);
    moduleLicense.setTransactions(Lists.newArrayList(CILicenseTransaction.builder()
                                                         .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
                                                         .developers(TOTAL_DEVELOPER)
                                                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                         .licenseType(LicenseType.TRIAL)
                                                         .moduleType(ModuleType.CI)
                                                         .status(LicenseStatus.ACTIVE)
                                                         .edition(Edition.ENTERPRISE)
                                                         .startTime(0)
                                                         .expiryTime(Long.MAX_VALUE)
                                                         .createdAt(0L)
                                                         .lastUpdatedAt(0L)
                                                         .build()));

    CheckResult checkResult =
        ciCheckProcessor.checkExpiry(DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense);
    assertThat(checkResult.isAllInactive()).isFalse();
    assertThat(checkResult.isUpdated()).isFalse();
    assertThat(moduleLicense.getTotalDevelopers()).isEqualTo(TOTAL_DEVELOPER);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithOneExpired() {
    CIModuleLicense moduleLicense =
        CIModuleLicense.builder().totalDevelopers(TOTAL_DEVELOPER + 2).maxDevelopers(MAX_DEVELOPER).build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CI);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);

    CILicenseTransaction transaction = CILicenseTransaction.builder()
                                           .uuid("expire")
                                           .developers(2)
                                           .moduleType(ModuleType.CI)
                                           .accountIdentifier(ACCOUNT_IDENTIFIER)
                                           .licenseType(LicenseType.TRIAL)
                                           .status(LicenseStatus.ACTIVE)
                                           .startTime(0)
                                           .expiryTime(0)
                                           .build();
    moduleLicense.setTransactions(Lists.newArrayList(CILicenseTransaction.builder()
                                                         .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
                                                         .developers(TOTAL_DEVELOPER)
                                                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                         .licenseType(LicenseType.TRIAL)
                                                         .moduleType(ModuleType.CI)
                                                         .status(LicenseStatus.ACTIVE)
                                                         .edition(Edition.ENTERPRISE)
                                                         .startTime(0)
                                                         .expiryTime(Long.MAX_VALUE)
                                                         .createdAt(0L)
                                                         .lastUpdatedAt(0L)
                                                         .build(),
        transaction));

    CheckResult checkResult =
        ciCheckProcessor.checkExpiry(DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense);
    assertThat(checkResult.isAllInactive()).isFalse();
    assertThat(checkResult.isUpdated()).isTrue();
    assertThat(moduleLicense.getTotalDevelopers()).isEqualTo(TOTAL_DEVELOPER);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithAllExpired() {
    CIModuleLicense moduleLicense =
        CIModuleLicense.builder().totalDevelopers(TOTAL_DEVELOPER + 2).maxDevelopers(MAX_DEVELOPER).build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CI);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);

    CILicenseTransaction transaction = CILicenseTransaction.builder()
                                           .uuid("expire")
                                           .developers(2)
                                           .moduleType(ModuleType.CI)
                                           .accountIdentifier(ACCOUNT_IDENTIFIER)
                                           .licenseType(LicenseType.TRIAL)
                                           .status(LicenseStatus.ACTIVE)
                                           .startTime(0)
                                           .expiryTime(0)
                                           .build();
    moduleLicense.setTransactions(Lists.newArrayList(CILicenseTransaction.builder()
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
                                                         .build(),
        transaction));

    CheckResult checkResult =
        ciCheckProcessor.checkExpiry(DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense);
    assertThat(checkResult.isAllInactive()).isTrue();
    assertThat(checkResult.isUpdated()).isTrue();
    assertThat(moduleLicense.getTotalDevelopers()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithAlreadyExpired() {
    CIModuleLicense moduleLicense = CIModuleLicense.builder().totalDevelopers(0).maxDevelopers(MAX_DEVELOPER).build();
    moduleLicense.setStatus(LicenseStatus.EXPIRED);
    moduleLicense.setModuleType(ModuleType.CI);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);
    moduleLicense.setTransactions(Lists.newArrayList(CILicenseTransaction.builder()
                                                         .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
                                                         .developers(TOTAL_DEVELOPER)
                                                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                         .licenseType(LicenseType.TRIAL)
                                                         .moduleType(ModuleType.CI)
                                                         .status(LicenseStatus.EXPIRED)
                                                         .edition(Edition.ENTERPRISE)
                                                         .startTime(0)
                                                         .expiryTime(0)
                                                         .createdAt(0L)
                                                         .lastUpdatedAt(0L)
                                                         .build()));

    CheckResult checkResult =
        ciCheckProcessor.checkExpiry(DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense);
    assertThat(checkResult.isAllInactive()).isTrue();
    assertThat(checkResult.isUpdated()).isFalse();
    assertThat(moduleLicense.getTotalDevelopers()).isEqualTo(0);
  }
}
