package io.harness.licensing.interfaces;

import static io.harness.licensing.LicenseTestConstant.ACCOUNT_IDENTIFIER;
import static io.harness.licensing.LicenseTestConstant.TOTAL_DEVELOPER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseTestBase;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.transactions.CDLicenseTransactionDTO;
import io.harness.licensing.beans.transactions.CELicenseTransactionDTO;
import io.harness.licensing.beans.transactions.CFLicenseTransactionDTO;
import io.harness.licensing.beans.transactions.CILicenseTransactionDTO;
import io.harness.licensing.interfaces.clients.ModuleLicenseClient;
import io.harness.licensing.interfaces.clients.local.CDLocalClient;
import io.harness.licensing.interfaces.clients.local.CELocalClient;
import io.harness.licensing.interfaces.clients.local.CFLocalClient;
import io.harness.licensing.interfaces.clients.local.CILocalClient;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.collect.Lists;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ModuleLicenseInterfaceImplTest extends LicenseTestBase {
  @InjectMocks ModuleLicenseInterfaceImpl moduleLicenseInterface;
  @Mock Map<ModuleType, ModuleLicenseClient> clientMap;

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testStartTrialOnCI() {
    when(clientMap.get(ModuleType.CI)).thenReturn(new CILocalClient());
    ModuleLicenseDTO expectedDTO = CIModuleLicenseDTO.builder()
                                       .totalDevelopers(TOTAL_DEVELOPER)
                                       .maxDevelopers(TOTAL_DEVELOPER)
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CI)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.ENTERPRISE)
                                       .status(LicenseStatus.ACTIVE)
                                       .transactions(Lists.newArrayList(CILicenseTransactionDTO.builder()
                                                                            .developers(TOTAL_DEVELOPER)
                                                                            .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                                            .licenseType(LicenseType.TRIAL)
                                                                            .moduleType(ModuleType.CI)
                                                                            .status(LicenseStatus.ACTIVE)
                                                                            .edition(Edition.ENTERPRISE)
                                                                            .startTime(0)
                                                                            .expiryTime(0)
                                                                            .build()))
                                       .build();
    CIModuleLicenseDTO dto = (CIModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.ENTERPRISE, ACCOUNT_IDENTIFIER, LicenseType.TRIAL, ModuleType.CI);
    dto.getTransactions().get(0).setStartTime(0L);
    dto.getTransactions().get(0).setExpiryTime(0L);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testStartTrialOnCF() {
    when(clientMap.get(ModuleType.CF)).thenReturn(new CFLocalClient());
    ModuleLicenseDTO expectedDTO = CFModuleLicenseDTO.builder()
                                       .totalFeatureFlagUnit(3)
                                       .totalClientMAUs(25000L)
                                       .maxFeatureFlagUnit(3)
                                       .maxClientMAUs(25000L)
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CF)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.ENTERPRISE)
                                       .status(LicenseStatus.ACTIVE)
                                       .transactions(Lists.newArrayList(CFLicenseTransactionDTO.builder()
                                                                            .clientMAU(25000L)
                                                                            .featureFlagUnit(3)
                                                                            .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                                            .licenseType(LicenseType.TRIAL)
                                                                            .moduleType(ModuleType.CF)
                                                                            .status(LicenseStatus.ACTIVE)
                                                                            .edition(Edition.ENTERPRISE)
                                                                            .startTime(0)
                                                                            .expiryTime(0)
                                                                            .build()))
                                       .build();
    CFModuleLicenseDTO dto = (CFModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.ENTERPRISE, ACCOUNT_IDENTIFIER, LicenseType.TRIAL, ModuleType.CF);
    dto.getTransactions().get(0).setStartTime(0L);
    dto.getTransactions().get(0).setExpiryTime(0L);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testStartTrialOnCE() {
    when(clientMap.get(ModuleType.CE)).thenReturn(new CELocalClient());
    ModuleLicenseDTO expectedDTO = CEModuleLicenseDTO.builder()
                                       .numberOfCluster(-1)
                                       .dataRetentionInDays(1825)
                                       .spendLimit(-1L)
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CE)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.ENTERPRISE)
                                       .status(LicenseStatus.ACTIVE)
                                       .transactions(Lists.newArrayList(CELicenseTransactionDTO.builder()
                                                                            .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                                            .licenseType(LicenseType.TRIAL)
                                                                            .moduleType(ModuleType.CE)
                                                                            .status(LicenseStatus.ACTIVE)
                                                                            .edition(Edition.ENTERPRISE)
                                                                            .startTime(0)
                                                                            .expiryTime(0)
                                                                            .build()))
                                       .build();
    CEModuleLicenseDTO dto = (CEModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.ENTERPRISE, ACCOUNT_IDENTIFIER, LicenseType.TRIAL, ModuleType.CE);
    dto.getTransactions().get(0).setStartTime(0L);
    dto.getTransactions().get(0).setExpiryTime(0L);
    assertThat(dto).isEqualTo(expectedDTO);
  }

  @Test
  @Owner(developers = OwnerRule.ZHUO)
  @Category(UnitTests.class)
  public void testStartTrialOnCD() {
    when(clientMap.get(ModuleType.CD)).thenReturn(new CDLocalClient());
    ModuleLicenseDTO expectedDTO = CDModuleLicenseDTO.builder()
                                       .maxWorkLoads(-1)
                                       .totalWorkLoads(-1)
                                       .deploymentsPerDay(-1)
                                       .accountIdentifier(ACCOUNT_IDENTIFIER)
                                       .moduleType(ModuleType.CD)
                                       .licenseType(LicenseType.TRIAL)
                                       .edition(Edition.ENTERPRISE)
                                       .status(LicenseStatus.ACTIVE)
                                       .transactions(Lists.newArrayList(CDLicenseTransactionDTO.builder()
                                                                            .workload(-1)
                                                                            .accountIdentifier(ACCOUNT_IDENTIFIER)
                                                                            .licenseType(LicenseType.TRIAL)
                                                                            .moduleType(ModuleType.CD)
                                                                            .status(LicenseStatus.ACTIVE)
                                                                            .edition(Edition.ENTERPRISE)
                                                                            .startTime(0)
                                                                            .expiryTime(0)
                                                                            .build()))
                                       .build();
    CDModuleLicenseDTO dto = (CDModuleLicenseDTO) moduleLicenseInterface.generateTrialLicense(
        Edition.ENTERPRISE, ACCOUNT_IDENTIFIER, LicenseType.TRIAL, ModuleType.CD);
    dto.getTransactions().get(0).setStartTime(0L);
    dto.getTransactions().get(0).setExpiryTime(0L);
    assertThat(dto).isEqualTo(expectedDTO);
  }
}
