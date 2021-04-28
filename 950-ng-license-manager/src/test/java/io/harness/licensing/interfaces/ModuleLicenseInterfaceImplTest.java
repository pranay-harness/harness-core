package io.harness.licensing.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseTestBase;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.UpdateChannel;
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.interfaces.clients.ModuleLicenseClient;
import io.harness.licensing.interfaces.clients.local.CFLocalClient;
import io.harness.licensing.interfaces.clients.local.CILocalClient;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.collect.Lists;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ModuleLicenseInterfaceImplTest extends LicenseTestBase {
  @InjectMocks ModuleLicenseInterfaceImpl moduleLicenseInterface;
  @Mock Map<ModuleType, ModuleLicenseClient> clientMap;
  private static final String DEFAULT_ACCOUNT = "123";

  @Before
  public void setup() {
    Instant.now(Clock.fixed(Instant.parse("2021-04-28T10:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testStartTrialOnCI() {
    when(clientMap.get(ModuleType.CI)).thenReturn(new CILocalClient());
    ModuleLicenseDTO expectedDTO = CIModuleLicenseDTO.builder()
                                       .numberOfCommitters(10)
                                       .accountIdentifier(DEFAULT_ACCOUNT)
                                       .moduleType(ModuleType.CI)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.ENTERPRISE)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(Instant.now().getEpochSecond())
                                       .expiryTime(0)
                                       .build();
    ModuleLicenseDTO dto = moduleLicenseInterface.createTrialLicense(
        Edition.ENTERPRISE, DEFAULT_ACCOUNT, LicenseType.TRIAL, ModuleType.CI);
    dto.setExpiryTime(0);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testStartTrialOnCF() {
    when(clientMap.get(ModuleType.CF)).thenReturn(new CFLocalClient());
    ModuleLicenseDTO expectedDTO = CFModuleLicenseDTO.builder()
                                       .numberOfUsers(2)
                                       .numberOfClientMAUs(5000)
                                       .updateChannels(Lists.newArrayList(UpdateChannel.POLLING))
                                       .accountIdentifier(DEFAULT_ACCOUNT)
                                       .moduleType(ModuleType.CF)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.ENTERPRISE)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(Instant.now().getEpochSecond())
                                       .expiryTime(0)
                                       .build();
    ModuleLicenseDTO dto = moduleLicenseInterface.createTrialLicense(
        Edition.ENTERPRISE, DEFAULT_ACCOUNT, LicenseType.TRIAL, ModuleType.CF);
    dto.setExpiryTime(0);
    assertThat(dto).isEqualTo(expectedDTO);
  }
}
