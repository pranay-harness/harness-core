package io.harness.licensing.services;

import static io.harness.ModuleType.CD;
import static io.harness.ModuleType.CE;
import static io.harness.licensing.LicenseTestConstant.ACCOUNT_IDENTIFIER;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_MODULE_LICENSE;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_MODULE_LICENSE_DTO;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_MODULE_TYPE;
import static io.harness.licensing.services.DefaultLicenseServiceImpl.SUCCEED_EXTEND_TRIAL_OPERATION;
import static io.harness.licensing.services.DefaultLicenseServiceImpl.SUCCEED_START_TRIAL_OPERATION;
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
import io.harness.ModuleType;
import io.harness.account.services.AccountService;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.ccm.license.remote.CeLicenseClient;
import io.harness.exception.InvalidRequestException;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialDTO;
import io.harness.licensing.beans.response.CheckExpiryResultDTO;
import io.harness.licensing.entities.modules.CDModuleLicense;
import io.harness.licensing.entities.modules.CEModuleLicense;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.interfaces.ModuleLicenseInterface;
import io.harness.licensing.mappers.LicenseObjectConverter;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.repositories.ModuleLicenseRepository;
import io.harness.rule.Owner;
import io.harness.telemetry.TelemetryReporter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DefaultLicenseServiceImplTest extends CategoryTest {
  @Mock ModuleLicenseRepository moduleLicenseRepository;
  @Mock ModuleLicenseInterface moduleLicenseInterface;
  @Mock LicenseObjectConverter licenseObjectConverter;
  @Mock AccountService accountService;
  @Mock TelemetryReporter telemetryReporter;
  @Mock CeLicenseClient ceLicenseClient;
  @InjectMocks DefaultLicenseServiceImpl licenseService;

  private StartTrialDTO startTrialRequestDTO;
  private AccountLicenseDTO defaultAccountLicensesDTO;

  @Before
  public void setUp() {
    initMocks(this);
    startTrialRequestDTO = StartTrialDTO.builder().moduleType(DEFAULT_MODULE_TYPE).build();
    when(licenseObjectConverter.toDTO(DEFAULT_CI_MODULE_LICENSE)).thenReturn(DEFAULT_CI_MODULE_LICENSE_DTO);
    when(licenseObjectConverter.toEntity(DEFAULT_CI_MODULE_LICENSE_DTO)).thenReturn(DEFAULT_CI_MODULE_LICENSE);

    defaultAccountLicensesDTO =
        AccountLicenseDTO.builder()
            .accountId(ACCOUNT_IDENTIFIER)
            .moduleLicenses(Collections.singletonMap(DEFAULT_MODULE_TYPE, DEFAULT_CI_MODULE_LICENSE_DTO))
            .allModuleLicenses(
                Collections.singletonMap(DEFAULT_MODULE_TYPE, Lists.newArrayList(DEFAULT_CI_MODULE_LICENSE_DTO)))
            .build();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetModuleLicenses() {
    when(moduleLicenseRepository.findByAccountIdentifierAndModuleType(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE))
        .thenReturn(Lists.newArrayList(DEFAULT_CI_MODULE_LICENSE));
    List<ModuleLicenseDTO> moduleLicense = licenseService.getModuleLicenses(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);

    assertThat(moduleLicense).isEqualTo(Lists.newArrayList(DEFAULT_CI_MODULE_LICENSE_DTO));
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetModuleLicenseWithNoResult() {
    when(moduleLicenseRepository.findByAccountIdentifierAndModuleType(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE))
        .thenReturn(new ArrayList<>());
    ModuleLicenseDTO moduleLicense = licenseService.getModuleLicense(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);
    assertThat(moduleLicense).isNull();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetAccountLicense() {
    when(moduleLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER))
        .thenReturn(ImmutableList.<ModuleLicense>builder().add(DEFAULT_CI_MODULE_LICENSE).build());
    AccountLicenseDTO accountLicenseDTO = licenseService.getAccountLicense(ACCOUNT_IDENTIFIER);

    assertThat(accountLicenseDTO).isEqualTo(defaultAccountLicensesDTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetAccountLicenseWithNoResult() {
    when(moduleLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(Collections.emptyList());
    AccountLicenseDTO accountLicenseDTO = licenseService.getAccountLicense(ACCOUNT_IDENTIFIER);

    assertThat(accountLicenseDTO.getAccountId()).isEqualTo(ACCOUNT_IDENTIFIER);
    assertThat(accountLicenseDTO.getModuleLicenses().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetModuleLicenseById() {
    when(moduleLicenseRepository.findById(any())).thenReturn(Optional.of(DEFAULT_CI_MODULE_LICENSE));
    ModuleLicenseDTO moduleLicense = licenseService.getModuleLicenseById("1");

    assertThat(moduleLicense).isEqualTo(DEFAULT_CI_MODULE_LICENSE_DTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testStartTrial() {
    when(moduleLicenseRepository.save(DEFAULT_CI_MODULE_LICENSE)).thenReturn(DEFAULT_CI_MODULE_LICENSE);
    when(moduleLicenseInterface.generateTrialLicense(any(), eq(ACCOUNT_IDENTIFIER), any(), eq(DEFAULT_MODULE_TYPE)))
        .thenReturn(DEFAULT_CI_MODULE_LICENSE_DTO);
    when(accountService.getAccount(ACCOUNT_IDENTIFIER)).thenReturn(AccountDTO.builder().build());
    ModuleLicenseDTO result = licenseService.startTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
    verify(accountService, times(1)).updateDefaultExperienceIfApplicable(ACCOUNT_IDENTIFIER, DefaultExperience.NG);
    verify(telemetryReporter, times(1)).sendGroupEvent(eq(ACCOUNT_IDENTIFIER), any(), any());
    verify(telemetryReporter, times(1))
        .sendTrackEvent(eq(SUCCEED_START_TRIAL_OPERATION), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
    verifyZeroInteractions(ceLicenseClient);
    assertThat(result).isEqualTo(DEFAULT_CI_MODULE_LICENSE_DTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testStartTrialForCE() {
    CEModuleLicense ceModuleLicense = CEModuleLicense.builder().spendLimit(-1L).build();
    ceModuleLicense.setAccountIdentifier(ACCOUNT_IDENTIFIER);
    ceModuleLicense.setModuleType(CE);
    ceModuleLicense.setLicenseType(LicenseType.TRIAL);
    ceModuleLicense.setEdition(Edition.ENTERPRISE);

    CEModuleLicenseDTO ceModuleLicenseDTO = CEModuleLicenseDTO.builder()
                                                .spendLimit(-1L)
                                                .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                .edition(Edition.ENTERPRISE)
                                                .licenseType(LicenseType.TRIAL)
                                                .moduleType(CE)
                                                .build();

    StartTrialDTO startTrialDTO = StartTrialDTO.builder().moduleType(CE).build();
    when(licenseObjectConverter.toEntity(ceModuleLicenseDTO)).thenReturn(ceModuleLicense);
    when(moduleLicenseRepository.save(ceModuleLicense)).thenReturn(ceModuleLicense);
    when(moduleLicenseInterface.generateTrialLicense(any(), eq(ACCOUNT_IDENTIFIER), any(), eq(ModuleType.CE)))
        .thenReturn(ceModuleLicenseDTO);
    when(accountService.getAccount(ACCOUNT_IDENTIFIER)).thenReturn(AccountDTO.builder().build());
    licenseService.startTrialLicense(ACCOUNT_IDENTIFIER, startTrialDTO);
    verify(ceLicenseClient, times(1)).createCeTrial(any());
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testExtendTrial() {
    when(moduleLicenseRepository.save(DEFAULT_CI_MODULE_LICENSE)).thenReturn(DEFAULT_CI_MODULE_LICENSE);
    when(moduleLicenseInterface.generateTrialLicense(any(), eq(ACCOUNT_IDENTIFIER), any(), eq(DEFAULT_MODULE_TYPE)))
        .thenReturn(DEFAULT_CI_MODULE_LICENSE_DTO);

    CIModuleLicense expiredTrial = CIModuleLicense.builder().numberOfCommitters(10).build();
    expiredTrial.setLicenseType(LicenseType.TRIAL);
    expiredTrial.setEdition(Edition.ENTERPRISE);
    expiredTrial.setExpiryTime(Instant.now().minus(5, ChronoUnit.DAYS).toEpochMilli());
    expiredTrial.setStatus(LicenseStatus.EXPIRED);
    when(moduleLicenseRepository.findByAccountIdentifierAndModuleType(eq(ACCOUNT_IDENTIFIER), eq(DEFAULT_MODULE_TYPE)))
        .thenReturn(Lists.newArrayList(expiredTrial));

    ModuleLicenseDTO result = licenseService.extendTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
    verify(telemetryReporter, times(1))
        .sendTrackEvent(eq(SUCCEED_EXTEND_TRIAL_OPERATION), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
    verifyZeroInteractions(ceLicenseClient);
    assertThat(result).isEqualTo(DEFAULT_CI_MODULE_LICENSE_DTO);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testExtendTrialFailed() {
    CIModuleLicense expiredTrial = CIModuleLicense.builder().numberOfCommitters(10).build();
    expiredTrial.setLicenseType(LicenseType.TRIAL);
    expiredTrial.setEdition(Edition.ENTERPRISE);
    expiredTrial.setExpiryTime(Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli());
    expiredTrial.setStatus(LicenseStatus.EXPIRED);
    when(moduleLicenseRepository.findByAccountIdentifierAndModuleType(eq(ACCOUNT_IDENTIFIER), eq(DEFAULT_MODULE_TYPE)))
        .thenReturn(Lists.newArrayList(expiredTrial));

    licenseService.extendTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithRegularTrial() {
    ModuleLicense moduleLicense = CIModuleLicense.builder().numberOfCommitters(10).build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CI);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);
    moduleLicense.setExpiryTime(Long.MAX_VALUE);
    when(moduleLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER))
        .thenReturn(ImmutableList.<ModuleLicense>builder().add(moduleLicense).build());

    CheckExpiryResultDTO checkExpiryResultDTO = licenseService.checkExpiry(ACCOUNT_IDENTIFIER);
    assertThat(checkExpiryResultDTO.isShouldDelete()).isFalse();
    assertThat(checkExpiryResultDTO.getExpiryTime()).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithTrialExpire() {
    ModuleLicense expiryModuleLicense = CDModuleLicense.builder().build();
    expiryModuleLicense.setStatus(LicenseStatus.ACTIVE);
    expiryModuleLicense.setModuleType(CD);
    expiryModuleLicense.setEdition(Edition.ENTERPRISE);
    expiryModuleLicense.setLicenseType(LicenseType.TRIAL);
    expiryModuleLicense.setExpiryTime(0);
    expiryModuleLicense.setCreatedBy(EmbeddedUser.builder().email("test").build());

    when(moduleLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER))
        .thenReturn(ImmutableList.<ModuleLicense>builder().add(expiryModuleLicense).build());

    CheckExpiryResultDTO checkExpiryResultDTO = licenseService.checkExpiry(ACCOUNT_IDENTIFIER);
    assertThat(checkExpiryResultDTO.isShouldDelete()).isTrue();
    assertThat(checkExpiryResultDTO.getExpiryTime()).isEqualTo(0);
    verify(moduleLicenseRepository, times(1)).save(any());
    verify(telemetryReporter, times(1)).sendTrackEvent(any(), any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithAlreadyExpiredTrial() {
    ModuleLicense expiredModuleLicense = CDModuleLicense.builder().build();
    expiredModuleLicense.setStatus(LicenseStatus.EXPIRED);
    expiredModuleLicense.setModuleType(CD);
    expiredModuleLicense.setEdition(Edition.ENTERPRISE);
    expiredModuleLicense.setLicenseType(LicenseType.TRIAL);
    expiredModuleLicense.setExpiryTime(0);

    when(moduleLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER))
        .thenReturn(ImmutableList.<ModuleLicense>builder().add(expiredModuleLicense).build());

    CheckExpiryResultDTO checkExpiryResultDTO = licenseService.checkExpiry(ACCOUNT_IDENTIFIER);
    assertThat(checkExpiryResultDTO.isShouldDelete()).isTrue();
    assertThat(checkExpiryResultDTO.getExpiryTime()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithFreeEdition() {
    ModuleLicense moduleLicense = CIModuleLicense.builder().numberOfCommitters(10).build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CI);
    moduleLicense.setEdition(Edition.FREE);
    moduleLicense.setLicenseType(LicenseType.TRIAL);

    when(moduleLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER))
        .thenReturn(ImmutableList.<ModuleLicense>builder().add(moduleLicense).build());

    CheckExpiryResultDTO checkExpiryResultDTO = licenseService.checkExpiry(ACCOUNT_IDENTIFIER);
    assertThat(checkExpiryResultDTO.isShouldDelete()).isFalse();
    assertThat(checkExpiryResultDTO.getExpiryTime()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testCheckExpiryWithPaid() {
    ModuleLicense moduleLicense = CIModuleLicense.builder().numberOfCommitters(10).build();
    moduleLicense.setStatus(LicenseStatus.ACTIVE);
    moduleLicense.setModuleType(ModuleType.CI);
    moduleLicense.setEdition(Edition.ENTERPRISE);
    moduleLicense.setLicenseType(LicenseType.PAID);

    when(moduleLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER))
        .thenReturn(ImmutableList.<ModuleLicense>builder().add(moduleLicense).build());

    CheckExpiryResultDTO checkExpiryResultDTO = licenseService.checkExpiry(ACCOUNT_IDENTIFIER);
    assertThat(checkExpiryResultDTO.isShouldDelete()).isFalse();
    assertThat(checkExpiryResultDTO.getExpiryTime()).isEqualTo(0);
    verify(moduleLicenseRepository, times(1)).save(any());
  }
}
