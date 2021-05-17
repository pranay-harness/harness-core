package io.harness.licensing.api.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

import io.harness.category.element.UnitTests;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseTestBase;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.AccountLicensesDTO;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialRequestDTO;
import io.harness.licensing.services.LicenseService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class LicenseResourceTest extends LicenseTestBase {
  @Mock LicenseService licenseService;
  @InjectMocks LicenseResource licenseResource;
  private ModuleLicenseDTO defaultModueLicenseDTO;
  private AccountLicensesDTO defaultAccountLicensesDTO;
  private static final String ACCOUNT_IDENTIFIER = "account";
  private static final ModuleType DEFAULT_MODULE_TYPE = ModuleType.CI;
  private StartTrialRequestDTO startTrialRequestDTO;

  @Before
  public void setUp() {
    defaultModueLicenseDTO = CIModuleLicenseDTO.builder()
                                 .numberOfCommitters(10)
                                 .id("id")
                                 .accountIdentifier(ACCOUNT_IDENTIFIER)
                                 .licenseType(LicenseType.TRIAL)
                                 .moduleType(DEFAULT_MODULE_TYPE)
                                 .status(LicenseStatus.ACTIVE)
                                 .startTime(0)
                                 .expiryTime(0)
                                 .createdAt(0)
                                 .lastModifiedAt(0)
                                 .build();
    defaultAccountLicensesDTO =
        AccountLicensesDTO.builder()
            .accountId(ACCOUNT_IDENTIFIER)
            .moduleLicenses(Collections.singletonMap(DEFAULT_MODULE_TYPE, defaultModueLicenseDTO))
            .build();
    startTrialRequestDTO = StartTrialRequestDTO.builder().moduleType(DEFAULT_MODULE_TYPE).build();
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testGetModuleLicense() {
    doReturn(defaultModueLicenseDTO).when(licenseService).getModuleLicense(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);
    ResponseDTO<ModuleLicenseDTO> licenseResponseDTO =
        licenseResource.getModuleLicense(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);
    Mockito.verify(licenseService, times(1)).getModuleLicense(ACCOUNT_IDENTIFIER, DEFAULT_MODULE_TYPE);
    assertThat(licenseResponseDTO.getData()).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testGetAccountLicensesDTO() {
    doReturn(defaultAccountLicensesDTO).when(licenseService).getAccountLicense(ACCOUNT_IDENTIFIER);
    ResponseDTO<AccountLicensesDTO> responseDTO = licenseResource.getAccountLicensesDTO(ACCOUNT_IDENTIFIER);
    Mockito.verify(licenseService, times(1)).getAccountLicense(ACCOUNT_IDENTIFIER);
    assertThat(responseDTO.getData()).isNotNull();
    assertThat(responseDTO.getData().getModuleLicenses().get(ModuleType.CI)).isEqualTo(defaultModueLicenseDTO);
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testGet() {
    doReturn(defaultModueLicenseDTO).when(licenseService).getModuleLicenseById(any());
    ResponseDTO<ModuleLicenseDTO> responseDTO = licenseResource.get("1", ACCOUNT_IDENTIFIER);
    Mockito.verify(licenseService, times(1)).getModuleLicenseById(any());
    assertThat(responseDTO.getData()).isNotNull();
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testStartTrial() {
    doReturn(defaultModueLicenseDTO).when(licenseService).startTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
    ResponseDTO<ModuleLicenseDTO> responseDTO =
        licenseResource.startTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
    Mockito.verify(licenseService, times(1)).startTrialLicense(ACCOUNT_IDENTIFIER, startTrialRequestDTO);
    assertThat(responseDTO.getData()).isNotNull();
  }
}