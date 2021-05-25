package io.harness.licensing.scheduler.modules;

import static io.harness.licensing.LicenseTestConstant.ACCOUNT_IDENTIFIER;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_ACCOUNT_UUID;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_LICENSE_TRANSACTION_UUID;
import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.category.element.UnitTests;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.entities.modules.CFModuleLicense;
import io.harness.licensing.entities.transactions.modules.CFLicenseTransaction;
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

public class CFCheckProcessorTest {
  @InjectMocks private CFCheckProcessor cfCheckProcessor;
  private static final Long MAX_MAUS = 20L;
  private static final Long TOTAL_MAUS = 10L;
  private static final Integer MAX_FFU = 50;
  private static final Integer TOTAL_FFU = 10;

  @Before
  public void setup() {
    initMocks(this);
    Instant.now(Clock.fixed(Instant.parse("2018-08-22T10:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithNoChange() {
    CFModuleLicense moduleLicense = CFModuleLicense.builder()
                                        .maxFeatureFlagUnit(MAX_FFU)
                                        .maxClientMAUs(MAX_MAUS)
                                        .totalFeatureFlagUnit(TOTAL_FFU)
                                        .totalClientMAUs(TOTAL_MAUS)
                                        .build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CF);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);
    moduleLicense.setTransactions(Lists.newArrayList(CFLicenseTransaction.builder()
                                                         .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
                                                         .featureFlagUnit(TOTAL_FFU)
                                                         .clientMAU(TOTAL_MAUS)
                                                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                         .licenseType(LicenseType.TRIAL)
                                                         .moduleType(ModuleType.CF)
                                                         .status(LicenseStatus.ACTIVE)
                                                         .edition(Edition.ENTERPRISE)
                                                         .startTime(0)
                                                         .expiryTime(Long.MAX_VALUE)
                                                         .createdAt(0L)
                                                         .lastUpdatedAt(0L)
                                                         .build()));

    CheckResult checkResult =
        cfCheckProcessor.checkExpiry(DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense);
    assertThat(checkResult.isAllInactive()).isFalse();
    assertThat(checkResult.isUpdated()).isFalse();
    assertThat(moduleLicense.getTotalClientMAUs()).isEqualTo(TOTAL_MAUS);
    assertThat(moduleLicense.getTotalFeatureFlagUnit()).isEqualTo(TOTAL_FFU);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithOneExpired() {
    CFModuleLicense moduleLicense = CFModuleLicense.builder()
                                        .maxFeatureFlagUnit(MAX_FFU)
                                        .maxClientMAUs(MAX_MAUS)
                                        .totalFeatureFlagUnit(TOTAL_FFU + 10)
                                        .totalClientMAUs(TOTAL_MAUS + 10)
                                        .build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CF);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);

    CFLicenseTransaction transaction = CFLicenseTransaction.builder()
                                           .uuid("expire")
                                           .clientMAU(10L)
                                           .featureFlagUnit(10)
                                           .moduleType(ModuleType.CF)
                                           .accountIdentifier(ACCOUNT_IDENTIFIER)
                                           .licenseType(LicenseType.TRIAL)
                                           .status(LicenseStatus.ACTIVE)
                                           .startTime(0)
                                           .expiryTime(0)
                                           .build();
    moduleLicense.setTransactions(Lists.newArrayList(CFLicenseTransaction.builder()
                                                         .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
                                                         .clientMAU(TOTAL_MAUS)
                                                         .featureFlagUnit(TOTAL_FFU)
                                                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                         .licenseType(LicenseType.TRIAL)
                                                         .moduleType(ModuleType.CF)
                                                         .status(LicenseStatus.ACTIVE)
                                                         .edition(Edition.ENTERPRISE)
                                                         .startTime(0)
                                                         .expiryTime(Long.MAX_VALUE)
                                                         .createdAt(0L)
                                                         .lastUpdatedAt(0L)
                                                         .build(),
        transaction));

    CheckResult checkResult =
        cfCheckProcessor.checkExpiry(DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense);
    assertThat(checkResult.isAllInactive()).isFalse();
    assertThat(checkResult.isUpdated()).isTrue();
    assertThat(moduleLicense.getTotalFeatureFlagUnit()).isEqualTo(TOTAL_FFU);
    assertThat(moduleLicense.getTotalClientMAUs()).isEqualTo(TOTAL_MAUS);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithAllExpired() {
    CFModuleLicense moduleLicense = CFModuleLicense.builder()
                                        .maxFeatureFlagUnit(MAX_FFU)
                                        .maxClientMAUs(MAX_MAUS)
                                        .totalFeatureFlagUnit(TOTAL_FFU + 10)
                                        .totalClientMAUs(TOTAL_MAUS + 10)
                                        .build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CF);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);

    CFLicenseTransaction transaction = CFLicenseTransaction.builder()
                                           .uuid("expire")
                                           .clientMAU(10L)
                                           .featureFlagUnit(10)
                                           .moduleType(ModuleType.CF)
                                           .accountIdentifier(ACCOUNT_IDENTIFIER)
                                           .licenseType(LicenseType.TRIAL)
                                           .status(LicenseStatus.ACTIVE)
                                           .startTime(0)
                                           .expiryTime(0)
                                           .build();
    moduleLicense.setTransactions(Lists.newArrayList(CFLicenseTransaction.builder()
                                                         .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
                                                         .clientMAU(TOTAL_MAUS)
                                                         .featureFlagUnit(TOTAL_FFU)
                                                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                         .licenseType(LicenseType.TRIAL)
                                                         .moduleType(ModuleType.CF)
                                                         .status(LicenseStatus.ACTIVE)
                                                         .edition(Edition.ENTERPRISE)
                                                         .startTime(0)
                                                         .expiryTime(0)
                                                         .createdAt(0L)
                                                         .lastUpdatedAt(0L)
                                                         .build(),
        transaction));

    CheckResult checkResult =
        cfCheckProcessor.checkExpiry(DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense);
    assertThat(checkResult.isAllInactive()).isTrue();
    assertThat(checkResult.isUpdated()).isTrue();
    assertThat(moduleLicense.getTotalFeatureFlagUnit()).isEqualTo(0);
    assertThat(moduleLicense.getTotalClientMAUs()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithAlreadyExpired() {
    CFModuleLicense moduleLicense = CFModuleLicense.builder()
                                        .maxFeatureFlagUnit(MAX_FFU)
                                        .maxClientMAUs(MAX_MAUS)
                                        .totalFeatureFlagUnit(0)
                                        .totalClientMAUs(0L)
                                        .build();
    moduleLicense.setStatus(LicenseStatus.EXPIRED);
    moduleLicense.setModuleType(ModuleType.CF);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);
    moduleLicense.setTransactions(Lists.newArrayList(CFLicenseTransaction.builder()
                                                         .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
                                                         .featureFlagUnit(TOTAL_FFU)
                                                         .clientMAU(TOTAL_MAUS)
                                                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                         .licenseType(LicenseType.TRIAL)
                                                         .moduleType(ModuleType.CF)
                                                         .status(LicenseStatus.EXPIRED)
                                                         .edition(Edition.ENTERPRISE)
                                                         .startTime(0)
                                                         .expiryTime(0)
                                                         .createdAt(0L)
                                                         .lastUpdatedAt(0L)
                                                         .build()));

    CheckResult checkResult =
        cfCheckProcessor.checkExpiry(DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense);
    assertThat(checkResult.isAllInactive()).isTrue();
    assertThat(checkResult.isUpdated()).isFalse();
    assertThat(moduleLicense.getTotalFeatureFlagUnit()).isEqualTo(0);
    assertThat(moduleLicense.getTotalClientMAUs()).isEqualTo(0);
  }
}
