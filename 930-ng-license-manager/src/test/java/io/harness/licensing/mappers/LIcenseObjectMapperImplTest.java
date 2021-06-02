package io.harness.licensing.mappers;

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
import io.harness.licensing.entities.modules.CIModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.rule.Owner;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class LIcenseObjectMapperImplTest extends LicenseTestBase {
  @InjectMocks LicenseObjectMapperImpl licenseObjectMapper;
  @Mock LicenseObjectMapper CIMapper;
  @Mock Map<ModuleType, LicenseObjectMapper> mapperMap;
  private ModuleLicenseDTO defaultModueLicenseDTO;
  private ModuleLicense expectedModuleLicense;
  private ModuleLicense defaultModuleLicense;
  private static final String ACCOUNT_IDENTIFIER = "account";
  private static final ModuleType DEFAULT_MODULE_TYPE = ModuleType.CI;
  private static final int DEFAULT_NUMBER_OF_COMMITTERS = 10;

  @Before
  public void setUp() throws Exception {
    defaultModueLicenseDTO = CIModuleLicenseDTO.builder()
                                 .numberOfCommitters(DEFAULT_NUMBER_OF_COMMITTERS)
                                 .id("id")
                                 .accountIdentifier(ACCOUNT_IDENTIFIER)
                                 .licenseType(LicenseType.TRIAL)
                                 .edition(Edition.ENTERPRISE)
                                 .status(LicenseStatus.ACTIVE)
                                 .moduleType(DEFAULT_MODULE_TYPE)
                                 .startTime(0)
                                 .expiryTime(0)
                                 .createdAt(0L)
                                 .lastModifiedAt(0L)
                                 .build();
    defaultModuleLicense = CIModuleLicense.builder().numberOfCommitters(DEFAULT_NUMBER_OF_COMMITTERS).build();
    defaultModuleLicense.setId("id");
    defaultModuleLicense.setAccountIdentifier(ACCOUNT_IDENTIFIER);
    defaultModuleLicense.setModuleType(DEFAULT_MODULE_TYPE);
    defaultModuleLicense.setEdition(Edition.ENTERPRISE);
    defaultModuleLicense.setStatus(LicenseStatus.ACTIVE);
    defaultModuleLicense.setLicenseType(LicenseType.TRIAL);
    defaultModuleLicense.setStartTime(0);
    defaultModuleLicense.setExpiryTime(0);
    defaultModuleLicense.setCreatedAt(0l);
    defaultModuleLicense.setLastUpdatedAt(0l);

    expectedModuleLicense = CIModuleLicense.builder().numberOfCommitters(DEFAULT_NUMBER_OF_COMMITTERS).build();
    expectedModuleLicense.setId("id");
    expectedModuleLicense.setAccountIdentifier(ACCOUNT_IDENTIFIER);
    expectedModuleLicense.setModuleType(DEFAULT_MODULE_TYPE);
    expectedModuleLicense.setEdition(Edition.ENTERPRISE);
    expectedModuleLicense.setStatus(LicenseStatus.ACTIVE);
    expectedModuleLicense.setLicenseType(LicenseType.TRIAL);
    expectedModuleLicense.setStartTime(0);
    expectedModuleLicense.setExpiryTime(0);

    when(CIMapper.toEntity(any()))
        .thenReturn(CIModuleLicense.builder().numberOfCommitters(DEFAULT_NUMBER_OF_COMMITTERS).build());
    when(CIMapper.toDTO(any()))
        .thenReturn(CIModuleLicenseDTO.builder().numberOfCommitters(DEFAULT_NUMBER_OF_COMMITTERS).build());
    when(mapperMap.get(DEFAULT_MODULE_TYPE)).thenReturn(CIMapper);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testToDTO() {
    ModuleLicenseDTO moduleLicenseDTO = licenseObjectMapper.toDTO(defaultModuleLicense);
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
