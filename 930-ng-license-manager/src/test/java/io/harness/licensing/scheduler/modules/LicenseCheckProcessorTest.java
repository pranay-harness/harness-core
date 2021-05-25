package io.harness.licensing.scheduler.modules;

import static io.harness.licensing.LicenseTestConstant.ACCOUNT_IDENTIFIER;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_ACCOUNT_UUID;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_LICENSE_TRANSACTION_UUID;
import static io.harness.licensing.LicenseTestConstant.MAX_DEVELOPER;
import static io.harness.licensing.LicenseTestConstant.TOTAL_DEVELOPER;
import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.transactions.modules.CILicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.rule.Owner;
import io.harness.telemetry.TelemetryReporter;

import com.google.common.collect.Lists;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class LicenseCheckProcessorTest extends CategoryTest {
  @InjectMocks private LicenseCheckProcessorimpl licenseCheckProcessorimpl;
  @Mock private TelemetryReporter telemetryReporter;

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
    moduleLicense.setExpireTime(Long.MAX_VALUE);
    moduleLicense.setTotalDevelopers(12);
    CILicenseTransaction transaction = CILicenseTransaction.builder()
                                           .uuid("another")
                                           .developers(2)
                                           .moduleType(ModuleType.CI)
                                           .accountIdentifier(ACCOUNT_IDENTIFIER)
                                           .licenseType(LicenseType.TRIAL)
                                           .status(LicenseStatus.ACTIVE)
                                           .startTime(0)
                                           .expiryTime(Long.MAX_VALUE - 1000)
                                           .build();
    moduleLicense.setTransactions(Lists.newArrayList(CILicenseTransaction.builder()
                                                         .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
                                                         .developers(10)
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

    CheckResult checkResult = licenseCheckProcessorimpl.checkExpiry(
        DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense, new CIAggregator());
    assertThat(checkResult.isAllInactive()).isFalse();
    assertThat(checkResult.isUpdated()).isFalse();
    assertThat(checkResult.getMaxExpiryTime()).isEqualTo(Long.MAX_VALUE);
    assertThat(checkResult.getTotalDevelopers()).isEqualTo(12);
    assertThat(moduleLicense.getTotalDevelopers()).isEqualTo(12);
    assertThat(moduleLicense.getExpireTime()).isEqualTo(Long.MAX_VALUE);
    assertThat(moduleLicense.getStatus()).isEqualTo(LicenseStatus.ACTIVE);
    verifyZeroInteractions(telemetryReporter);
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
    moduleLicense.setExpireTime(0);
    moduleLicense.setTotalDevelopers(12);
    moduleLicense.setCreatedBy(EmbeddedUser.builder().email("123").build());

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
                                                         .developers(10)
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

    CheckResult checkResult = licenseCheckProcessorimpl.checkExpiry(
        DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense, new CIAggregator());
    assertThat(checkResult.isAllInactive()).isFalse();
    assertThat(checkResult.isUpdated()).isTrue();
    assertThat(checkResult.getMaxExpiryTime()).isEqualTo(Long.MAX_VALUE);
    assertThat(checkResult.getTotalDevelopers()).isEqualTo(10);
    assertThat(moduleLicense.getTotalDevelopers()).isEqualTo(10);
    assertThat(moduleLicense.getExpireTime()).isEqualTo(Long.MAX_VALUE);
    assertThat(moduleLicense.getStatus()).isEqualTo(LicenseStatus.ACTIVE);
    verify(telemetryReporter, times(1)).sendTrackEvent(any(), any(), any(), any(), any(), any());
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
    moduleLicense.setExpireTime(0);
    moduleLicense.setTotalDevelopers(12);
    moduleLicense.setCreatedBy(EmbeddedUser.builder().email("123").build());

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

    CheckResult checkResult = licenseCheckProcessorimpl.checkExpiry(
        DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense, new CIAggregator());
    assertThat(checkResult.isAllInactive()).isTrue();
    assertThat(checkResult.isUpdated()).isTrue();
    assertThat(checkResult.getMaxExpiryTime()).isEqualTo(0);
    assertThat(checkResult.getTotalDevelopers()).isEqualTo(0);
    assertThat(moduleLicense.getTotalDevelopers()).isEqualTo(0);
    assertThat(moduleLicense.getExpireTime()).isEqualTo(0);
    assertThat(moduleLicense.getStatus()).isEqualTo(LicenseStatus.EXPIRED);
    verify(telemetryReporter, times(2)).sendTrackEvent(any(), any(), any(), any(), any(), any());
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
    moduleLicense.setExpireTime(0);
    moduleLicense.setTotalDevelopers(0);
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

    CheckResult checkResult = licenseCheckProcessorimpl.checkExpiry(
        DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense, new CIAggregator());
    assertThat(checkResult.isAllInactive()).isTrue();
    assertThat(checkResult.isUpdated()).isFalse();
    assertThat(checkResult.getMaxExpiryTime()).isEqualTo(0);
    assertThat(checkResult.getTotalDevelopers()).isEqualTo(0);
    assertThat(moduleLicense.getTotalDevelopers()).isEqualTo(0);
    assertThat(moduleLicense.getExpireTime()).isEqualTo(0);
    assertThat(moduleLicense.getStatus()).isEqualTo(LicenseStatus.EXPIRED);
    verifyZeroInteractions(telemetryReporter);
  }
}
