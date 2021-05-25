package io.harness.licensing.api.resource;

import static io.harness.licensing.LicenseTestConstant.ACCOUNT_IDENTIFIER;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_ACCOUNT_LICENSE_DTO;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_MODULE_LICENSE_DTO;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_MODULE_TYPE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

import io.harness.category.element.UnitTests;
import io.harness.licensing.LicenseTestBase;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialRequestDTO;
import io.harness.licensing.services.AccountLicenseService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class AccountLicenseResourceTest extends LicenseTestBase {
  @Mock AccountLicenseService licenseService;
  @InjectMocks AccountLicenseResource licenseResource;
  private StartTrialRequestDTO startTrialRequestDTO;

  @Before
  public void setUp() {
    startTrialRequestDTO = StartTrialRequestDTO.builder().moduleType(DEFAULT_MODULE_TYPE).build();
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testGetModuleLicense() {
    doReturn(DEFAULT_CI_MODULE_LICENSE_DTO)
        .when(licenseService)
        .getModuleLicense(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);
    ResponseDTO<ModuleLicenseDTO> licenseResponseDTO =
        licenseResource.getModuleLicense(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);
    Mockito.verify(licenseService, times(1)).getModuleLicense(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);
    assertThat(licenseResponseDTO.getData()).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testGetAccountLicensesDTO() {
    doReturn(DEFAULT_ACCOUNT_LICENSE_DTO).when(licenseService).getAccountLicense(ACCOUNT_IDENTIFIER);
    ResponseDTO<AccountLicenseDTO> responseDTO = licenseResource.getAccountLicensesDTO(ACCOUNT_IDENTIFIER);
    Mockito.verify(licenseService, times(1)).getAccountLicense(ACCOUNT_IDENTIFIER);
    assertThat(responseDTO.getData()).isNotNull();
    assertThat(responseDTO.getData().getModuleLicenses().get(ModuleType.CI)).isEqualTo(DEFAULT_CI_MODULE_LICENSE_DTO);
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testGet() {
    doReturn(DEFAULT_ACCOUNT_LICENSE_DTO).when(licenseService).getAccountLicenseById(any());
    ResponseDTO<AccountLicenseDTO> responseDTO = licenseResource.get(ACCOUNT_IDENTIFIER);
    Mockito.verify(licenseService, times(1)).getAccountLicenseById(ACCOUNT_IDENTIFIER);
    assertThat(responseDTO.getData()).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testStartTrial() {
    doReturn(DEFAULT_CI_MODULE_LICENSE_DTO)
        .when(licenseService)
        .startTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
    ResponseDTO<ModuleLicenseDTO> responseDTO =
        licenseResource.startTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
    Mockito.verify(licenseService, times(1)).startTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
    assertThat(responseDTO.getData()).isNotNull();
  }
}