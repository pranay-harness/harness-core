package io.harness.licensing.mappers;

import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_LICENSE_TRANSACTION;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_LICENSE_TRANSACTION_DTO;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_MODULE_LICENSE;
import static io.harness.licensing.LicenseTestConstant.DEFAULT_CI_MODULE_LICENSE_DTO;
import static io.harness.licensing.LicenseTestConstant.MAX_DEVELOPER;
import static io.harness.licensing.LicenseTestConstant.TOTAL_DEVELOPER;
import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseTestBase;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.transactions.LicenseTransactionDTO;
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.entities.transactions.LicenseTransaction;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class LicenseObjectConverterTest extends LicenseTestBase {
  @InjectMocks LicenseObjectConverter licenseObjectMapper;
  @Mock LicenseObjectMapper CIMapper;
  @Mock Map<ModuleType, LicenseObjectMapper> mapperMap;
  @Mock LicenseTransactionConverter licenseTransactionConverter;
  private ModuleLicenseDTO defaultModueLicenseDTO;
  private ModuleLicense expectedModuleLicense;
  private ModuleLicense defaultModuleLicense;
  private LicenseTransaction licenseTransaction;
  private LicenseTransactionDTO licenseTransactionDTO;
  private static final String ACCOUNT_IDENTIFIER = "account";
  private static final ModuleType DEFAULT_MODULE_TYPE = ModuleType.CI;

  @Before
  public void setUp() {
    defaultModueLicenseDTO = DEFAULT_CI_MODULE_LICENSE_DTO;
    defaultModuleLicense = DEFAULT_CI_MODULE_LICENSE;

    expectedModuleLicense =
        CIModuleLicense.builder().totalDevelopers(TOTAL_DEVELOPER).maxDevelopers(MAX_DEVELOPER).build();
    expectedModuleLicense.setAccountIdentifier(ACCOUNT_IDENTIFIER);
    expectedModuleLicense.setModuleType(DEFAULT_MODULE_TYPE);
    expectedModuleLicense.setEdition(Edition.ENTERPRISE);
    expectedModuleLicense.setStatus(LicenseStatus.ACTIVE);
    expectedModuleLicense.setLicenseType(LicenseType.TRIAL);
    expectedModuleLicense.setTransactions(Lists.newArrayList(DEFAULT_CI_LICENSE_TRANSACTION));

    licenseTransaction = DEFAULT_CI_LICENSE_TRANSACTION;
    licenseTransactionDTO = DEFAULT_CI_LICENSE_TRANSACTION_DTO;

    when(CIMapper.toEntity(any()))
        .thenReturn(CIModuleLicense.builder().totalDevelopers(TOTAL_DEVELOPER).maxDevelopers(MAX_DEVELOPER).build());
    when(CIMapper.toDTO(any()))
        .thenReturn(CIModuleLicenseDTO.builder().totalDevelopers(TOTAL_DEVELOPER).maxDevelopers(MAX_DEVELOPER).build());
    when(mapperMap.get(DEFAULT_MODULE_TYPE)).thenReturn(CIMapper);
    when(licenseTransactionConverter.toDTO(licenseTransaction)).thenReturn(licenseTransactionDTO);
    when(licenseTransactionConverter.toEntity(licenseTransactionDTO)).thenReturn(licenseTransaction);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testToDTO() {
    ModuleLicenseDTO moduleLicenseDTO = licenseObjectMapper.toDTO(defaultModuleLicense);
    // temporary set due to legacy module license
    moduleLicenseDTO.setCreatedAt(0L);
    moduleLicenseDTO.setLastModifiedAt(0L);
    assertThat(moduleLicenseDTO).isEqualTo(defaultModueLicenseDTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testToEntity() {
    ModuleLicense moduleLicense = licenseObjectMapper.toEntity(defaultModueLicenseDTO);
    assertThat(moduleLicense).isEqualTo(expectedModuleLicense);
  }
}
