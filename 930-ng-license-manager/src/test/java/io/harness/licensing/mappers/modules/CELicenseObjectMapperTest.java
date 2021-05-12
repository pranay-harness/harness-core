package io.harness.licensing.mappers.modules;

import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.licensing.LicenseTestBase;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.CEModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class CELicenseObjectMapperTest extends LicenseTestBase {
  @InjectMocks CELicenseObjectMapper objectMapper;
  private CEModuleLicense moduleLicense;
  private CEModuleLicenseDTO moduleLicenseDTO;
  private static final long DEFAULT_SPEND = 250000;
  private static final int DEFAULT_CLUSTERS = 2;
  private static final int DEFAULT_RETENTIOn = 30;

  @Before
  public void setUp() {
    moduleLicense = CEModuleLicense.builder()
                        .numberOfCluster(DEFAULT_CLUSTERS)
                        .spendLimit(DEFAULT_SPEND)
                        .dataRetentionInDays(DEFAULT_RETENTIOn)
                        .build();
    moduleLicenseDTO = CEModuleLicenseDTO.builder()
                           .numberOfCluster(DEFAULT_CLUSTERS)
                           .spendLimit(DEFAULT_SPEND)
                           .dataRetentionInDays(DEFAULT_RETENTIOn)
                           .build();
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testToDTO() {
    ModuleLicenseDTO moduleLicenseDTO = objectMapper.toDTO(moduleLicense);
    assertThat(moduleLicenseDTO).isEqualTo(moduleLicenseDTO);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testToEntity() {
    ModuleLicense moduleLicense = objectMapper.toEntity(moduleLicenseDTO);
    assertThat(moduleLicense).isEqualTo(moduleLicense);
  }
}