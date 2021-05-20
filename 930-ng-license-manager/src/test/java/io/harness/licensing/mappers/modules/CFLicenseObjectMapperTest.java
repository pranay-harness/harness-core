package io.harness.licensing.mappers.modules;

import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.licensing.LicenseTestBase;
import io.harness.licensing.UpdateChannel;
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.CFModuleLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class CFLicenseObjectMapperTest extends LicenseTestBase {
  @InjectMocks CFLicenseObjectMapper objectMapper;
  private CFModuleLicense moduleLicense;
  private CFModuleLicenseDTO moduleLicenseDTO;
  private static final List<UpdateChannel> DEFAULT_CHANNELS = Lists.newArrayList(UpdateChannel.POLLING);
  private static final int DEFAULT_USER_NUMBER = 2;
  private static final int DEFAULT_CLIENT_MAU = 5000;

  @Before
  public void setUp() {
    moduleLicense = CFModuleLicense.builder()
                        .updateChannels(DEFAULT_CHANNELS)
                        .numberOfUsers(DEFAULT_USER_NUMBER)
                        .numberOfClientMAUs(DEFAULT_CLIENT_MAU)
                        .build();
    moduleLicenseDTO = CFModuleLicenseDTO.builder()
                           .updateChannels(DEFAULT_CHANNELS)
                           .numberOfUsers(DEFAULT_USER_NUMBER)
                           .numberOfClientMAUs(DEFAULT_CLIENT_MAU)
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
