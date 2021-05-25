package io.harness.licensing.services;

import static io.harness.licensing.LicenseTestConstant.DEFAULT_ACCOUNT_LICENSE;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_ACCOUNT_LICENSE_DTO;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_MODULE_LICENSE;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_MODULE_LICENSE_DTO;
import static io.harness.licensing.services.DefaultLicenseServiceImpl.FAILED_OPERATION;
import static io.harness.licensing.services.DefaultLicenseServiceImpl.SUCCEED_OPERATION;
import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.harness.account.services.AccountService;
import io.harness.category.element.UnitTests;
import io.harness.exception.DuplicateFieldException;
import io.harness.licensing.LicenseTestBase;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialRequestDTO;
import io.harness.licensing.entities.account.AccountLicense;
import io.harness.licensing.entities.modules.CDModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.interfaces.ModuleLicenseInterface;
import io.harness.licensing.mappers.AccountLicenseMapper;
import io.harness.licensing.mappers.LicenseObjectConverter;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.repositories.AccountLicenseRepository;
import io.harness.rule.Owner;
import io.harness.telemetry.TelemetryReporter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class AccountLicenseServiceImplTest extends LicenseTestBase {
  @Mock AccountLicenseRepository accountLicenseRepository;
  @Mock ModuleLicenseInterface moduleLicenseInterface;
  @Mock LicenseObjectConverter licenseObjectConverter;
  @Mock AccountService accountService;
  @Mock TelemetryReporter telemetryReporter;
  @Mock AccountLicenseMapper accountLicenseMapper;
  @InjectMocks AccountLicenseServiceImpl licenseService;

  private AccountLicense emptyAccountLicense;
  private StartTrialRequestDTO startTrialRequestDTO;
  private static final String ACCOUNT_IDENTIFIER = "account";
  private static final ModuleType DEFAULT_MODULE_TYPE = ModuleType.CI;

  @Before
  public void setUp() {
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
        .sendTrackEvent(eq(SUCCEED_OPERATION), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
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
        .sendTrackEvent(eq(SUCCEED_OPERATION), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
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
    boolean result = licenseService.checkNGLicensesAllInactive(ACCOUNT_IDENTIFIER);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckNGLicensesAllInactiveWithAccountLicense() {
    when(accountLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(DEFAULT_ACCOUNT_LICENSE);
    boolean result = licenseService.checkNGLicensesAllInactive(ACCOUNT_IDENTIFIER);
    assertThat(result).isFalse();
  }
}
