package io.harness.licensing.mappers;

import static io.harness.licensing.LicenseTestConstant.ACCOUNT_IDENTIFIER;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_ACCOUNT_LICENSE_DTO;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_ACCOUNT_UUID;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_LICENSE_TRANSACTION;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_MODULE_LICENSE;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_MODULE_LICENSE_DTO;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_MODULE_TYPE;
import static io.harness.licensing.LicenseTestConstant.MAX_DEVELOPER;
import static io.harness.licensing.LicenseTestConstant.TOTAL_DEVELOPER;
import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.account.AccountLicense;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class AccountLicenseMapperImplTest extends CategoryTest {
  @InjectMocks AccountLicenseMapperImpl accountLicenseMapper;
  @Mock LicenseObjectConverter licenseObjectConverter;
  private ModuleLicenseDTO defaultModueLicenseDTO;
  private ModuleLicense expectedModuleLicense;
  private ModuleLicense defaultModuleLicense;
  private AccountLicenseDTO accountLicenseDTO;
  private AccountLicense accountLicense;

  @Before
  public void setup() {
    initMocks(this);
    defaultModueLicenseDTO = DEFAULT_CI_MODULE_LICENSE_DTO;
    accountLicenseDTO = DEFAULT_ACCOUNT_LICENSE_DTO;

    defaultModuleLicense = DEFAULT_CI_MODULE_LICENSE;

    expectedModuleLicense =
        CIModuleLicense.builder().totalDevelopers(TOTAL_DEVELOPER).maxDevelopers(MAX_DEVELOPER).build();
    expectedModuleLicense.setAccountIdentifier(ACCOUNT_IDENTIFIER);
    expectedModuleLicense.setModuleType(DEFAULT_MODULE_TYPE);
    expectedModuleLicense.setEdition(Edition.ENTERPRISE);
    expectedModuleLicense.setStatus(LicenseStatus.ACTIVE);
    expectedModuleLicense.setLicenseType(LicenseType.TRIAL);
    expectedModuleLicense.setTransactions(Lists.newArrayList(DEFAULT_CI_LICENSE_TRANSACTION));

    when(licenseObjectConverter.toEntity(any())).thenReturn(expectedModuleLicense);
    when(licenseObjectConverter.toDTO(any())).thenReturn(defaultModueLicenseDTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testToDTO() {
    accountLicense = AccountLicense.builder()
                         .uuid(DEFAULT_ACCOUNT_UUID)
                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                         .allInactive(false)
                         .moduleLicenses(ImmutableMap.of(DEFAULT_MODULE_TYPE, defaultModuleLicense))
                         .build();
    AccountLicenseDTO dto = accountLicenseMapper.toDTO(accountLicense);
    assertThat(dto).isEqualTo(accountLicenseDTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testToEntity() {
    accountLicense = AccountLicense.builder()
                         .uuid(DEFAULT_ACCOUNT_UUID)
                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                         .allInactive(false)
                         .moduleLicenses(ImmutableMap.of(DEFAULT_MODULE_TYPE, expectedModuleLicense))
                         .build();
    AccountLicense entity = accountLicenseMapper.toEntity(accountLicenseDTO);
    assertThat(entity).isEqualTo(accountLicense);
  }
}
