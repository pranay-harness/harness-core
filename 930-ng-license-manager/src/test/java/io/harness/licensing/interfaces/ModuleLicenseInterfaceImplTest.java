package io.harness.licensing.interfaces;

import static io.harness.licensing.LicenseConstant.UNLIMITED;
import static io.harness.licensing.LicenseTestConstant.ACCOUNT_IDENTIFIER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.category.element.UnitTests;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.types.CDLicenseType;
import io.harness.licensing.interfaces.clients.ModuleLicenseClient;
import io.harness.licensing.interfaces.clients.local.CDLocalClient;
import io.harness.licensing.interfaces.clients.local.CELocalClient;
import io.harness.licensing.interfaces.clients.local.CFLocalClient;
import io.harness.licensing.interfaces.clients.local.CILocalClient;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ModuleLicenseInterfaceImplTest extends CategoryTest {
  @InjectMocks ModuleLicenseImpl moduleLicenseInterface;
  @Mock Map<ModuleType, ModuleLicenseClient> clientMap;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testStartEnterpriseTrialOnCI() {
    when(clientMap.get(ModuleType.CI)).thenReturn(new CILocalClient());
    ModuleLicenseDTO expectedDTO = CIModuleLicenseDTO.builder()
                                       .numberOfCommitters(200)
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CI)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.ENTERPRISE)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(0)
                                       .expiryTime(0)
                                       .build();
    CIModuleLicenseDTO dto = (CIModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.ENTERPRISE, ACCOUNT_IDENTIFIER, ModuleType.CI);
    dto.setStartTime(0L);
    dto.setExpiryTime(0);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.NATHAN)
  @Category(UnitTests.class)
  public void testStartTeamTrialOnCI() {
    when(clientMap.get(ModuleType.CI)).thenReturn(new CILocalClient());
    ModuleLicenseDTO expectedDTO = CIModuleLicenseDTO.builder()
                                       .numberOfCommitters(200)
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CI)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.TEAM)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(0)
                                       .expiryTime(0)
                                       .build();
    CIModuleLicenseDTO dto = (CIModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.TEAM, ACCOUNT_IDENTIFIER, ModuleType.CI);
    dto.setStartTime(0L);
    dto.setExpiryTime(0);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.NATHAN)
  @Category(UnitTests.class)
  public void testStartFreeLicenseOnCI() {
    when(clientMap.get(ModuleType.CI)).thenReturn(new CILocalClient());
    ModuleLicenseDTO expectedDTO = CIModuleLicenseDTO.builder()
                                       .numberOfCommitters(Integer.valueOf(UNLIMITED))
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CI)
                                       .edition(Edition.FREE)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(0)
                                       .expiryTime(Long.valueOf(UNLIMITED))
                                       .build();
    CIModuleLicenseDTO dto = (CIModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.FREE, ACCOUNT_IDENTIFIER, ModuleType.CI);
    dto.setStartTime(0L);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testStartEnterpriseTrialOnCF() {
    when(clientMap.get(ModuleType.CF)).thenReturn(new CFLocalClient());
    ModuleLicenseDTO expectedDTO = CFModuleLicenseDTO.builder()
                                       .numberOfUsers(50)
                                       .numberOfClientMAUs(1000000L)
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CF)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.ENTERPRISE)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(0)
                                       .expiryTime(0)
                                       .build();
    CFModuleLicenseDTO dto = (CFModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.ENTERPRISE, ACCOUNT_IDENTIFIER, ModuleType.CF);
    dto.setStartTime(0L);
    dto.setExpiryTime(0);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.NATHAN)
  @Category(UnitTests.class)
  public void testStartTeamTrialOnCF() {
    when(clientMap.get(ModuleType.CF)).thenReturn(new CFLocalClient());
    ModuleLicenseDTO expectedDTO = CFModuleLicenseDTO.builder()
                                       .numberOfUsers(50)
                                       .numberOfClientMAUs(1000000L)
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CF)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.TEAM)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(0)
                                       .expiryTime(0)
                                       .build();
    CFModuleLicenseDTO dto = (CFModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.TEAM, ACCOUNT_IDENTIFIER, ModuleType.CF);
    dto.setStartTime(0L);
    dto.setExpiryTime(0);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.NATHAN)
  @Category(UnitTests.class)
  public void testStartFreeLicenseOnCF() {
    when(clientMap.get(ModuleType.CF)).thenReturn(new CFLocalClient());
    ModuleLicenseDTO expectedDTO = CFModuleLicenseDTO.builder()
                                       .numberOfUsers(2)
                                       .numberOfClientMAUs(25000L)
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CF)
                                       .edition(Edition.FREE)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(0)
                                       .expiryTime(Long.valueOf(UNLIMITED))
                                       .build();
    CFModuleLicenseDTO dto = (CFModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.FREE, ACCOUNT_IDENTIFIER, ModuleType.CF);
    dto.setStartTime(0L);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testStartEnterpriseTrialOnCE() {
    when(clientMap.get(ModuleType.CE)).thenReturn(new CELocalClient());
    ModuleLicenseDTO expectedDTO = CEModuleLicenseDTO.builder()
                                       .spendLimit(Long.valueOf(UNLIMITED))
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CE)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.ENTERPRISE)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(0)
                                       .expiryTime(0)
                                       .build();
    CEModuleLicenseDTO dto = (CEModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.ENTERPRISE, ACCOUNT_IDENTIFIER, ModuleType.CE);
    dto.setStartTime(0L);
    dto.setExpiryTime(0);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.NATHAN)
  @Category(UnitTests.class)
  public void testStartTeamTrialOnCE() {
    when(clientMap.get(ModuleType.CE)).thenReturn(new CELocalClient());
    ModuleLicenseDTO expectedDTO = CEModuleLicenseDTO.builder()
                                       .spendLimit(Long.valueOf(UNLIMITED))
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CE)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.TEAM)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(0)
                                       .expiryTime(0)
                                       .build();
    CEModuleLicenseDTO dto = (CEModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.TEAM, ACCOUNT_IDENTIFIER, ModuleType.CE);
    dto.setStartTime(0L);
    dto.setExpiryTime(0);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.NATHAN)
  @Category(UnitTests.class)
  public void testStartFreeLicenseOnCE() {
    when(clientMap.get(ModuleType.CE)).thenReturn(new CELocalClient());
    ModuleLicenseDTO expectedDTO = CEModuleLicenseDTO.builder()
                                       .spendLimit(250000L)
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CE)
                                       .edition(Edition.FREE)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(0)
                                       .expiryTime(Long.valueOf(UNLIMITED))
                                       .build();
    CEModuleLicenseDTO dto = (CEModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.FREE, ACCOUNT_IDENTIFIER, ModuleType.CE);
    dto.setStartTime(0L);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testStartEnterpriseTrialOnCD() {
    when(clientMap.get(ModuleType.CD)).thenReturn(new CDLocalClient());
    ModuleLicenseDTO expectedDTO = CDModuleLicenseDTO.builder()
                                       .cdLicenseType(CDLicenseType.SERVICES)
                                       .workloads(100)
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CD)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.ENTERPRISE)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(0)
                                       .expiryTime(0)
                                       .build();
    CDModuleLicenseDTO dto = (CDModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.ENTERPRISE, ACCOUNT_IDENTIFIER, ModuleType.CD);
    dto.setStartTime(0L);
    dto.setExpiryTime(0);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.NATHAN)
  @Category(UnitTests.class)
  public void testStartTeamTrialOnCD() {
    when(clientMap.get(ModuleType.CD)).thenReturn(new CDLocalClient());
    ModuleLicenseDTO expectedDTO = CDModuleLicenseDTO.builder()
                                       .cdLicenseType(CDLicenseType.SERVICES)
                                       .workloads(100)
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CD)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.TEAM)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(0)
                                       .expiryTime(0)
                                       .build();
    CDModuleLicenseDTO dto = (CDModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.TEAM, ACCOUNT_IDENTIFIER, ModuleType.CD);
    dto.setStartTime(0L);
    dto.setExpiryTime(0);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.NATHAN)
  @Category(UnitTests.class)
  public void testStartFreeLicenseOnCD() {
    when(clientMap.get(ModuleType.CD)).thenReturn(new CDLocalClient());
    ModuleLicenseDTO expectedDTO = CDModuleLicenseDTO.builder()
                                       .cdLicenseType(CDLicenseType.SERVICES)
                                       .workloads(5)
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CD)
                                       .edition(Edition.FREE)
                                       .status(LicenseStatus.ACTIVE)
                                       .startTime(0)
                                       .expiryTime(Long.valueOf(UNLIMITED))
                                       .build();
    CDModuleLicenseDTO dto = (CDModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.FREE, ACCOUNT_IDENTIFIER, ModuleType.CD);
    dto.setStartTime(0L);
    assertThat(dto).isEqualTo(expectedDTO);
  }
}
