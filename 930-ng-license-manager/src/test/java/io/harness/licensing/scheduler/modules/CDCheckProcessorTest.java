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
import io.harness.licensing.entities.modules.CDModuleLicense;
import io.harness.licensing.entities.transactions.modules.CDLicenseTransaction;
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

public class CDCheckProcessorTest {
  @InjectMocks private CDCheckProcessor cdCheckProcessor;
  private static final Integer TOTAL_WORK_LOAD = 100;
  private static final Integer MAX_WORK_LOAD = 200;

  @Before
  public void setup() {
    initMocks(this);
    Instant.now(Clock.fixed(Instant.parse("2018-08-22T10:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithNoChange() {
    CDModuleLicense moduleLicense = CDModuleLicense.builder()
                                        .totalWorkLoads(TOTAL_WORK_LOAD)
                                        .maxWorkLoads(MAX_WORK_LOAD)
                                        .deploymentsPerDay(-1)
                                        .build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CD);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);
    moduleLicense.setTransactions(Lists.newArrayList(CDLicenseTransaction.builder()
                                                         .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
                                                         .workload(TOTAL_WORK_LOAD)
                                                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                         .licenseType(LicenseType.TRIAL)
                                                         .moduleType(ModuleType.CD)
                                                         .status(LicenseStatus.ACTIVE)
                                                         .edition(Edition.ENTERPRISE)
                                                         .startTime(0)
                                                         .expiryTime(Long.MAX_VALUE)
                                                         .createdAt(0L)
                                                         .lastUpdatedAt(0L)
                                                         .build()));

    CheckResult checkResult =
        cdCheckProcessor.checkExpiry(DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense);
    assertThat(checkResult.isAllInactive()).isFalse();
    assertThat(checkResult.isUpdated()).isFalse();
    assertThat(moduleLicense.getTotalWorkLoads()).isEqualTo(TOTAL_WORK_LOAD);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithOneExpired() {
    CDModuleLicense moduleLicense = CDModuleLicense.builder()
                                        .totalWorkLoads(TOTAL_WORK_LOAD + 50)
                                        .maxWorkLoads(MAX_WORK_LOAD)
                                        .deploymentsPerDay(-1)
                                        .build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CD);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);

    CDLicenseTransaction transaction = CDLicenseTransaction.builder()
                                           .uuid("expire")
                                           .workload(50)
                                           .moduleType(ModuleType.CD)
                                           .accountIdentifier(ACCOUNT_IDENTIFIER)
                                           .licenseType(LicenseType.TRIAL)
                                           .startTime(0)
                                           .expiryTime(0)
                                           .build();
    moduleLicense.setTransactions(Lists.newArrayList(CDLicenseTransaction.builder()
                                                         .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
                                                         .workload(TOTAL_WORK_LOAD)
                                                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                         .licenseType(LicenseType.TRIAL)
                                                         .moduleType(ModuleType.CD)
                                                         .status(LicenseStatus.ACTIVE)
                                                         .edition(Edition.ENTERPRISE)
                                                         .startTime(0)
                                                         .expiryTime(Long.MAX_VALUE)
                                                         .createdAt(0L)
                                                         .lastUpdatedAt(0L)
                                                         .build(),
        transaction));

    CheckResult checkResult =
        cdCheckProcessor.checkExpiry(DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense);
    assertThat(checkResult.isAllInactive()).isFalse();
    assertThat(checkResult.isUpdated()).isTrue();
    assertThat(moduleLicense.getTotalWorkLoads()).isEqualTo(TOTAL_WORK_LOAD);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithAllExpired() {
    CDModuleLicense moduleLicense = CDModuleLicense.builder()
                                        .totalWorkLoads(TOTAL_WORK_LOAD + 50)
                                        .maxWorkLoads(MAX_WORK_LOAD)
                                        .deploymentsPerDay(-1)
                                        .build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CD);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);

    CDLicenseTransaction transaction = CDLicenseTransaction.builder()
                                           .uuid("expire")
                                           .workload(50)
                                           .moduleType(ModuleType.CD)
                                           .accountIdentifier(ACCOUNT_IDENTIFIER)
                                           .licenseType(LicenseType.TRIAL)
                                           .status(LicenseStatus.ACTIVE)
                                           .startTime(0)
                                           .expiryTime(0)
                                           .build();
    moduleLicense.setTransactions(Lists.newArrayList(CDLicenseTransaction.builder()
                                                         .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
                                                         .workload(TOTAL_WORK_LOAD)
                                                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                         .licenseType(LicenseType.TRIAL)
                                                         .moduleType(ModuleType.CD)
                                                         .status(LicenseStatus.ACTIVE)
                                                         .edition(Edition.ENTERPRISE)
                                                         .startTime(0)
                                                         .expiryTime(0)
                                                         .createdAt(0L)
                                                         .lastUpdatedAt(0L)
                                                         .build(),
        transaction));

    CheckResult checkResult =
        cdCheckProcessor.checkExpiry(DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense);
    assertThat(checkResult.isAllInactive()).isTrue();
    assertThat(checkResult.isUpdated()).isTrue();
    assertThat(moduleLicense.getTotalWorkLoads()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithAlreadyExpired() {
    CDModuleLicense moduleLicense =
        CDModuleLicense.builder().totalWorkLoads(0).maxWorkLoads(MAX_WORK_LOAD).deploymentsPerDay(-1).build();
    moduleLicense.setStatus(LicenseStatus.EXPIRED);
    moduleLicense.setModuleType(ModuleType.CD);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);
    moduleLicense.setTransactions(Lists.newArrayList(CDLicenseTransaction.builder()
                                                         .uuid(DEFAULT_LICENSE_TRANSACTION_UUID)
                                                         .workload(TOTAL_WORK_LOAD)
                                                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                         .licenseType(LicenseType.TRIAL)
                                                         .moduleType(ModuleType.CD)
                                                         .status(LicenseStatus.EXPIRED)
                                                         .edition(Edition.ENTERPRISE)
                                                         .startTime(0)
                                                         .expiryTime(0)
                                                         .createdAt(0L)
                                                         .lastUpdatedAt(0L)
                                                         .build()));

    CheckResult checkResult =
        cdCheckProcessor.checkExpiry(DEFAULT_ACCOUNT_UUID, Instant.now().toEpochMilli(), moduleLicense);
    assertThat(checkResult.isAllInactive()).isTrue();
    assertThat(checkResult.isUpdated()).isFalse();
    assertThat(moduleLicense.getTotalWorkLoads()).isEqualTo(0);
  }
}
