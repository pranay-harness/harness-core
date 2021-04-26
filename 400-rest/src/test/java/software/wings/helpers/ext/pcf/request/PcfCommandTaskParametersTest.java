package software.wings.helpers.ext.pcf.request;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.TMACARI;

import static software.wings.sm.states.pcf.PcfStateTestHelper.ORG;
import static software.wings.sm.states.pcf.PcfStateTestHelper.SPACE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.FileData;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;

import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;
import software.wings.beans.VaultConfig;

import com.google.common.collect.ImmutableList;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
@TargetModule(_960_API_SERVICES)
public class PcfCommandTaskParametersTest extends WingsBaseTest {
  @Test
  @Owner(developers = {PRASHANT, TMACARI})
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilities() {
    PcfCommandTaskParameters taskParameters =
        PcfCommandTaskParameters.builder()
            .pcfCommandRequest(PcfCommandDeployRequest.builder()
                                   .pcfConfig(PcfConfig.builder().endpointUrl("pcfUrl").build())
                                   .useAppAutoscalar(true)
                                   .build())
            .encryptedDataDetails(Collections.singletonList(EncryptedDataDetail.builder()
                                                                .fieldName("test")
                                                                .encryptionConfig(VaultConfig.builder().build())
                                                                .encryptedData(EncryptedRecordData.builder().build())
                                                                .build()))
            .build();

    List<ExecutionCapability> executionCapabilities = taskParameters.fetchRequiredExecutionCapabilities(null);
    assertThat(executionCapabilities).hasSize(4);
    assertThat(executionCapabilities.stream().map(ExecutionCapability::getCapabilityType))
        .containsExactlyInAnyOrder(CapabilityType.PCF_CONNECTIVITY, CapabilityType.HTTP, CapabilityType.PCF_INSTALL,
            CapabilityType.PCF_AUTO_SCALAR);
  }

  @Test
  @Owner(developers = {PRASHANT, TMACARI})
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesPluginRequest() {
    PcfCommandTaskParameters taskParameters =
        PcfCommandTaskParameters.builder()
            .pcfCommandRequest(
                PcfRunPluginCommandRequest.builder()
                    .pcfCommandType(PcfCommandRequest.PcfCommandType.SETUP)
                    .pcfConfig(PcfConfig.builder().endpointUrl("pcfUrl").build())
                    .useCLIForPcfAppCreation(true)
                    .useAppAutoscalar(false)
                    .organization(ORG)
                    .space(SPACE)
                    .accountId(ACCOUNT_ID)
                    .timeoutIntervalInMin(5)
                    .renderedScriptString("cf create-service ${service.manifest.repoRoot}/manifest.yml")
                    .encryptedDataDetails(null)
                    .fileDataList(ImmutableList.of(FileData.builder()
                                                       .filePath("manifest.yml")
                                                       .fileBytes("file data ".getBytes(StandardCharsets.UTF_8))
                                                       .build()))
                    .filePathsInScript(ImmutableList.of("/manifest.yml"))
                    .build())
            .encryptedDataDetails(Collections.singletonList(EncryptedDataDetail.builder()
                                                                .fieldName("test")
                                                                .encryptionConfig(VaultConfig.builder().build())
                                                                .encryptedData(EncryptedRecordData.builder().build())
                                                                .build()))
            .build();

    List<ExecutionCapability> executionCapabilities = taskParameters.fetchRequiredExecutionCapabilities(null);
    assertThat(executionCapabilities).hasSize(3);
    assertThat(executionCapabilities.stream().map(ExecutionCapability::getCapabilityType))
        .containsExactlyInAnyOrder(CapabilityType.PCF_CONNECTIVITY, CapabilityType.HTTP, CapabilityType.PCF_INSTALL);
  }
}
