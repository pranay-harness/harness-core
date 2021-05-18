package io.harness.licensing.mappers.modules;

import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.licensing.LicenseTestBase;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.CDModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class CDLicenseObjectMapperTest extends LicenseTestBase {
  @InjectMocks CDLicenseObjectMapper objectMapper;
  private CDModuleLicense moduleLicense;
  private CDModuleLicenseDTO moduleLicenseDTO;
  private static final int DEFAULT_MAX_WORK_LOAD = 5;
  private static final int DEFAULT_DEPLOYMENT_PER_DAY = 10;

  @Before
  public void setUp() {
    moduleLicense = CDModuleLicense.builder()
                        .maxWorkLoads(DEFAULT_MAX_WORK_LOAD)
                        .deploymentsPerDay(DEFAULT_DEPLOYMENT_PER_DAY)
                        .build();
    moduleLicenseDTO = CDModuleLicenseDTO.builder()
                           .maxWorkLoads(DEFAULT_MAX_WORK_LOAD)
                           .deploymentsPerDay(DEFAULT_DEPLOYMENT_PER_DAY)
                           .build();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testToDTO() {
    ModuleLicenseDTO result = objectMapper.toDTO(moduleLicense);
    assertThat(result).isEqualTo(moduleLicenseDTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testToEntity() {
    ModuleLicense result = objectMapper.toEntity(moduleLicenseDTO);
    assertThat(result).isEqualTo(moduleLicense);
  }
}
