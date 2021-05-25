package io.harness.licensing.services;

import static io.harness.licensing.LicenseTestConstant.DEFAULT_ACCOUNT_LICENSE;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_ACCOUNT_LICENSE_DTO;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_MODULE_LICENSE;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_MODULE_LICENSE_DTO;
import static io.harness.licensing.services.AccountLicenseServiceImpl.SUCCEED_START_TRIAL_OPERATION;
import static io.harness.licensing.services.DefaultLicenseServiceImpl.FAILED_OPERATION;
import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.account.services.AccountService;
import io.harness.category.element.UnitTests;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialRequestDTO;
import io.harness.licensing.entities.account.AccountLicense;
import io.harness.licensing.entities.modules.CDModuleLicense;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.entities.transactions.LicenseTransaction;
import io.harness.licensing.entities.transactions.modules.CILicenseTransaction;
import io.harness.licensing.interfaces.ModuleLicenseInterface;
import io.harness.licensing.mappers.AccountLicenseMapper;
import io.harness.licensing.mappers.LicenseObjectConverter;
import io.harness.licensing.scheduler.AccountLicenseCheckHandler;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.repositories.AccountLicenseRepository;
import io.harness.rule.Owner;
import io.harness.telemetry.TelemetryReporter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class AccountLicenseServiceImplTest extends CategoryTest {
  @Mock AccountLicenseRepository accountLicenseRepository;
  @Mock ModuleLicenseInterface moduleLicenseInterface;
  @Mock LicenseObjectConverter licenseObjectConverter;
  @Mock AccountService accountService;
  @Mock TelemetryReporter telemetryReporter;
  @Mock AccountLicenseMapper accountLicenseMapper;
  @Mock AccountLicenseCheckHandler accountLicenseCheckHandler;
  @InjectMocks AccountLicenseServiceImpl licenseService;

  private AccountLicense emptyAccountLicense;
  private StartTrialRequestDTO startTrialRequestDTO;
  private static final String ACCOUNT_IDENTIFIER = "account";
  private static final ModuleType DEFAULT_MODULE_TYPE = ModuleType.CI;

  @Before
  public void setUp() {
    initMocks(this);
    emptyAccountLicense = AccountLicense.builder().accountIdentifier(ACCOUNT_IDENTIFIER).build();

    startTrialRequestDTO = StartTrialRequestDTO.builder().moduleType(DEFAULT_MODULE_TYPE).build();
    when(licenseObjectConverter.toDTO(DEFAULT_CI_MODULE_LICENSE)).thenReturn(DEFAULT_CI_MODULE_LICENSE_DTO);
    when(licenseObjectConverter.toEntity(DEFAULT_CI_MODULE_LICENSE_DTO)).thenReturn(DEFAULT_CI_MODULE_LICENSE);
    when(accountLicenseMapper.toDTO(DEFAULT_ACCOUNT_LICENSE)).thenReturn(DEFAULT_ACCOUNT_LICENSE_DTO);
    when(accountLicenseMapper.toEntity(DEFAULT_ACCOUNT_LICENSE_DTO)).thenReturn(DEFAULT_ACCOUNT_LICENSE);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetModuleLicense() {
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(DEFAULT_ACCOUNT_LICENSE);
    ModuleLicenseDTO moduleLicense = licenseService.getModuleLicense(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);

    assertThat(moduleLicense).isEqualTo(DEFAULT_CI_MODULE_LICENSE_DTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetModuleLicenseWithNoAccount() {
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(null);
    ModuleLicenseDTO moduleLicense = licenseService.getModuleLicense(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);
    assertThat(moduleLicense).isNull();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetModuleLicenseWithNoModuleType() {
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(emptyAccountLicense);
    ModuleLicenseDTO moduleLicense = licenseService.getModuleLicense(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);
    assertThat(moduleLicense).isNull();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetAccountLicense() {
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(DEFAULT_ACCOUNT_LICENSE);
    AccountLicenseDTO accountLicenseDTO = licenseService.getAccountLicense(ACCOUNT_IDENTIFIER);

    assertThat(accountLicenseDTO).isEqualTo(DEFAULT_ACCOUNT_LICENSE_DTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetAccountLicenseWithNoResult() {
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(null);
    AccountLicenseDTO accountLicenseDTO = licenseService.getAccountLicense(ACCOUNT_IDENTIFIER);

    assertThat(accountLicenseDTO).isNull();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetAccountLicenseById() {
    when(accountLicenseRepository.findById(any())).thenReturn(Optional.of(DEFAULT_ACCOUNT_LICENSE));
    AccountLicenseDTO accountLicenseDTO = licenseService.getAccountLicenseById("1");

    assertThat(accountLicenseDTO).isEqualTo(DEFAULT_ACCOUNT_LICENSE_DTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testStartTrialWithoutAccountLicense() {
    when(accountLicenseRepository.save(any())).thenReturn(DEFAULT_ACCOUNT_LICENSE);
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(null);
    when(moduleLicenseInterface.generateTrialLicense(any(), eq(ACCOUNT_IDENTIFIER), any(), eq(DEFAULT_MODULE_TYPE)))
        .thenReturn(DEFAULT_CI_MODULE_LICENSE_DTO);
    ModuleLicenseDTO result = licenseService.startTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
    verify(accountService, times(1)).updateDefaultExperienceIfApplicable(ACCOUNT_IDENTIFIER, DefaultExperience.NG);
    verify(telemetryReporter, times(1)).sendGroupEvent(eq(ACCOUNT_IDENTIFIER), any(), any());
    verify(telemetryReporter, times(1))
        .sendTrackEvent(eq(SUCCEED_START_TRIAL_OPERATION), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
    assertThat(result).isEqualTo(DEFAULT_CI_MODULE_LICENSE_DTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testStartTrialWithAccountLicense() {
    Map<ModuleType, ModuleLicense> existingModuleLicense = new HashMap<>();
    existingModuleLicense.put(ModuleType.CD, CDModuleLicense.builder().build());
    AccountLicense existingAccountLicense = AccountLicense.builder()
                                                .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                .allInactive(false)
                                                .moduleLicenses(existingModuleLicense)
                                                .build();

    when(accountLicenseRepository.save(any())).thenReturn(existingAccountLicense);
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(existingAccountLicense);
    when(moduleLicenseInterface.generateTrialLicense(any(), eq(ACCOUNT_IDENTIFIER), any(), eq(DEFAULT_MODULE_TYPE)))
        .thenReturn(DEFAULT_CI_MODULE_LICENSE_DTO);

    ModuleLicenseDTO result = licenseService.startTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
    verify(accountService, times(1)).updateDefaultExperienceIfApplicable(ACCOUNT_IDENTIFIER, DefaultExperience.NG);
    verify(telemetryReporter, times(1)).sendGroupEvent(eq(ACCOUNT_IDENTIFIER), any(), any());
    verify(telemetryReporter, times(1))
        .sendTrackEvent(eq(SUCCEED_START_TRIAL_OPERATION), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
    assertThat(existingAccountLicense.getModuleLicenses().get(DEFAULT_MODULE_TYPE)).isNotNull();
    assertThat(result).isEqualTo(DEFAULT_CI_MODULE_LICENSE_DTO);
  }

  @Test(expected = DuplicateFieldException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testStartTrialOnExistingOne() {
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(DEFAULT_ACCOUNT_LICENSE);
    when(moduleLicenseInterface.generateTrialLicense(any(), eq(ACCOUNT_IDENTIFIER), any(), eq(DEFAULT_MODULE_TYPE)))
        .thenReturn(DEFAULT_CI_MODULE_LICENSE_DTO);
    try {
      licenseService.startTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
    } catch (DuplicateFieldException e) {
      verifyZeroInteractions(accountService);
      verify(telemetryReporter, times(1))
          .sendTrackEvent(eq(FAILED_OPERATION), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
      throw e;
    }
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckNGLicensesAllInactiveWithiytAccountLicense() {
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(null);
    boolean result = licenseService.shouldRemoveAccount(ACCOUNT_IDENTIFIER);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckNGLicensesAllInactiveWithAccountLicense() {
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(DEFAULT_ACCOUNT_LICENSE);
    boolean result = licenseService.shouldRemoveAccount(ACCOUNT_IDENTIFIER);
    assertThat(result).isFalse();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testExtendTrial() {
    CIModuleLicense moduleLicense = CIModuleLicense.builder().build();
    moduleLicense.setStatus(LicenseStatus.EXPIRED);
    moduleLicense.setExpireTime(Instant.now().toEpochMilli());
    moduleLicense.setTotalDevelopers(10);
    moduleLicense.setMaxDevelopers(20);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);
    moduleLicense.setModuleType(ModuleType.CI);
    moduleLicense.setAccountIdentifier(ACCOUNT_IDENTIFIER);
    moduleLicense.setTransactions(Lists.newArrayList(CILicenseTransaction.builder()
                                                         .developers(10)
                                                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                         .edition(Edition.ENTERPRISE)
                                                         .licenseType(LicenseType.TRIAL)
                                                         .status(LicenseStatus.EXPIRED)
                                                         .moduleType(ModuleType.CI)
                                                         .build()));
    AccountLicense accountLicense = AccountLicense.builder()
                                        .moduleLicenses(ImmutableMap.of(ModuleType.CI, moduleLicense))
                                        .allInactive(true)
                                        .build();
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(accountLicense);
    licenseService.extendTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);

    verify(accountLicenseCheckHandler, times(1)).handle(accountLicense);
    LicenseTransaction licenseTransaction =
        accountLicense.getModuleLicenses().get(ModuleType.CI).getTransactions().get(1);
    assertThat(licenseTransaction.getAccountIdentifier()).isEqualTo(ACCOUNT_IDENTIFIER);
    assertThat(licenseTransaction.getStatus()).isEqualTo(LicenseStatus.ACTIVE);
    assertThat(licenseTransaction.getEdition()).isEqualTo(Edition.ENTERPRISE);
    assertThat(licenseTransaction.getLicenseType()).isEqualTo(LicenseType.TRIAL);
    assertThat(licenseTransaction.getModuleType()).isEqualTo(ModuleType.CI);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testExtendTrialWithoutStart() {
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(emptyAccountLicense);
    licenseService.extendTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testExtendTrialExceedDeadline() {
    CIModuleLicense moduleLicense = CIModuleLicense.builder().build();
    moduleLicense.setExpireTime(0);
    AccountLicense accountLicense =
        AccountLicense.builder().moduleLicenses(ImmutableMap.of(ModuleType.CI, moduleLicense)).build();
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(accountLicense);
    licenseService.extendTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
  }
}
