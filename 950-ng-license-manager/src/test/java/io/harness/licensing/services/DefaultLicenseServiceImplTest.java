package io.harness.licensing.services;

import static io.harness.licensing.services.DefaultLicenseServiceImpl.SUCCEED_OPERATION;
import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.account.services.AccountService;
import io.harness.category.element.UnitTests;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseTestBase;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.AccountLicensesDTO;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialRequestDTO;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.interfaces.ModuleLicenseInterface;
import io.harness.licensing.mappers.LicenseObjectMapper;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.repositories.ModuleLicenseRepository;
import io.harness.rule.Owner;
import io.harness.telemetry.TelemetryReporter;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DefaultLicenseServiceImplTest extends LicenseTestBase {
  @Mock ModuleLicenseRepository moduleLicenseRepository;
  @Mock ModuleLicenseInterface moduleLicenseInterface;
  @Mock LicenseObjectMapper licenseObjectMapper;
  @Mock AccountService accountService;
  @Mock TelemetryReporter telemetryReporter;
  @InjectMocks DefaultLicenseServiceImpl licenseService;

  private ModuleLicenseDTO defaultModueLicenseDTO;
  private ModuleLicense defaultModuleLicense;
  private AccountLicensesDTO defaultAccountLicensesDTO;
  private StartTrialRequestDTO startTrialRequestDTO;
  private static final String ACCOUNT_IDENTIFIER = "account";
  private static final ModuleType DEFAULT_MODULE_TYPE = ModuleType.CI;

  @Before
  public void setUp() {
    defaultModueLicenseDTO = CIModuleLicenseDTO.builder()
                                 .numberOfCommitters(10)
                                 .id("id")
                                 .accountIdentifier(ACCOUNT_IDENTIFIER)
                                 .licenseType(LicenseType.TRIAL)
                                 .edition(Edition.ENTERPRISE)
                                 .status(LicenseStatus.ACTIVE)
                                 .moduleType(DEFAULT_MODULE_TYPE)
                                 .startTime(0)
                                 .expiryTime(0)
                                 .createdAt(0)
                                 .lastModifiedAt(0)
                                 .build();
    defaultModuleLicense = CIModuleLicense.builder().numberOfCommitters(10).build();
    defaultModuleLicense.setId("id");
    defaultModuleLicense.setAccountIdentifier(ACCOUNT_IDENTIFIER);
    defaultModuleLicense.setModuleType(DEFAULT_MODULE_TYPE);
    defaultModuleLicense.setEdition(Edition.ENTERPRISE);
    defaultModuleLicense.setStatus(LicenseStatus.ACTIVE);
    defaultModuleLicense.setLicenseType(LicenseType.TRIAL);
    defaultModuleLicense.setStartTime(0);
    defaultModuleLicense.setExpiryTime(0);
    defaultModuleLicense.setCreatedAt(0L);
    defaultModuleLicense.setLastUpdatedAt(0L);

    defaultAccountLicensesDTO =
        AccountLicensesDTO.builder()
            .accountId(ACCOUNT_IDENTIFIER)
            .moduleLicenses(Collections.singletonMap(DEFAULT_MODULE_TYPE, defaultModueLicenseDTO))
            .build();
    startTrialRequestDTO = StartTrialRequestDTO.builder().moduleType(DEFAULT_MODULE_TYPE).build();
    when(licenseObjectMapper.toDTO(defaultModuleLicense)).thenReturn(defaultModueLicenseDTO);
    when(licenseObjectMapper.toEntity(defaultModueLicenseDTO)).thenReturn(defaultModuleLicense);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetModuleLicense() {
    when(moduleLicenseRepository.findByAccountIdentifierAndModuleType(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE))
        .thenReturn(defaultModuleLicense);
    ModuleLicenseDTO moduleLicense = licenseService.getModuleLicense(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);

    assertThat(moduleLicense).isEqualTo(defaultModueLicenseDTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetModuleLicenseWithNoResult() {
    when(moduleLicenseRepository.findByAccountIdentifierAndModuleType(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE))
        .thenReturn(null);
    ModuleLicenseDTO moduleLicense = licenseService.getModuleLicense(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);
    assertThat(moduleLicense).isNull();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetAccountLicense() {
    when(moduleLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER))
        .thenReturn(ImmutableList.<ModuleLicense>builder().add(defaultModuleLicense).build());
    AccountLicensesDTO accountLicensesDTO = licenseService.getAccountLicense(ACCOUNT_IDENTIFIER);

    assertThat(accountLicensesDTO).isEqualTo(defaultAccountLicensesDTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetAccountLicenseWithNoResult() {
    when(moduleLicenseRepository.findByAccountIdentifier(ACCOUNT_IDENTIFIER)).thenReturn(Collections.emptyList());
    AccountLicensesDTO accountLicensesDTO = licenseService.getAccountLicense(ACCOUNT_IDENTIFIER);

    assertThat(accountLicensesDTO.getAccountId()).isEqualTo(ACCOUNT_IDENTIFIER);
    assertThat(accountLicensesDTO.getModuleLicenses().size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetModuleLicenseById() {
    when(moduleLicenseRepository.findById(any())).thenReturn(Optional.of(defaultModuleLicense));
    ModuleLicenseDTO moduleLicense = licenseService.getModuleLicenseById("1");

    assertThat(moduleLicense).isEqualTo(defaultModueLicenseDTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testStartTrial() {
    when(moduleLicenseRepository.save(defaultModuleLicense)).thenReturn(defaultModuleLicense);
    when(moduleLicenseInterface.createTrialLicense(any(), eq(ACCOUNT_IDENTIFIER), any(), eq(DEFAULT_MODULE_TYPE)))
        .thenReturn(defaultModueLicenseDTO);
    ModuleLicenseDTO result = licenseService.startTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
    verify(accountService, times(1)).updateDefaultExperienceIfNull(ACCOUNT_IDENTIFIER, DefaultExperience.NG);
    verify(telemetryReporter, times(1)).sendGroupEvent(eq(ACCOUNT_IDENTIFIER), any(), any());
    verify(telemetryReporter, times(1))
        .sendTrackEvent(eq(SUCCEED_OPERATION), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
    assertThat(result).isEqualTo(defaultModueLicenseDTO);
  }
}
