package io.harness.licensing.services;

import static io.harness.licensing.LicenseTestConstant.ACCOUNT_IDENTIFIER;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_MODULE_LICENSE;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_MODULE_LICENSE_DTO;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_MODULE_TYPE;
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
import io.harness.licensing.LicenseTestBase;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialRequestDTO;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.interfaces.ModuleLicenseInterface;
import io.harness.licensing.mappers.LicenseObjectConverter;
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
  @Mock LicenseObjectConverter licenseObjectConverter;
  @Mock AccountService accountService;
  @Mock TelemetryReporter telemetryReporter;
  @InjectMocks DefaultLicenseServiceImpl licenseService;

  private StartTrialRequestDTO startTrialRequestDTO;
  private AccountLicenseDTO defaultAccountLicensesDTO;

  @Before
  public void setUp() {
    startTrialRequestDTO = StartTrialRequestDTO.builder().moduleType(DEFAULT_MODULE_TYPE).build();
    when(licenseObjectConverter.toDTO(DEFAULT_CI_MODULE_LICENSE)).thenReturn(DEFAULT_CI_MODULE_LICENSE_DTO);
    when(licenseObjectConverter.toEntity(DEFAULT_CI_MODULE_LICENSE_DTO)).thenReturn(DEFAULT_CI_MODULE_LICENSE);

    defaultAccountLicensesDTO =
        AccountLicenseDTO.builder()
            .accountId(ACCOUNT_IDENTIFIER)
            .moduleLicenses(Collections.singletonMap(DEFAULT_MODULE_TYPE, DEFAULT_CI_MODULE_LICENSE_DTO))
            .build();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testGetModuleLicense() {
    when(moduleLicenseRepository.findByAccountIdentifierAndModuleType(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE))
        .thenReturn(DEFAULT_CI_MODULE_LICENSE);
    ModuleLicenseDTO moduleLicense = licenseService.getModuleLicense(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);

    assertThat(moduleLicense).isEqualTo(DEFAULT_CI_MODULE_LICENSE_DTO);
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
    ModuleLicenseDTO result = licenseService.startTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
    verify(accountService, times(1)).updateDefaultExperienceIfApplicable(ACCOUNT_IDENTIFIER, DefaultExperience.NG);
    verify(telemetryReporter, times(1)).sendGroupEvent(eq(ACCOUNT_IDENTIFIER), any(), any());
    verify(telemetryReporter, times(1))
        .sendTrackEvent(eq(SUCCEED_OPERATION), any(), any(), eq(io.harness.telemetry.Category.SIGN_UP));
    assertThat(result).isEqualTo(DEFAULT_CI_MODULE_LICENSE_DTO);
  }
}
