package software.wings.helpers.ext.pcf.request;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.sm.states.pcf.PcfStateTestHelper.ORG;
import static software.wings.sm.states.pcf.PcfStateTestHelper.SPACE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.common.collect.ImmutableList;

import io.harness.beans.FileData;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.PcfConfig;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PcfCommandTaskParametersTest extends WingsBaseTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilities() {
    PcfCommandTaskParameters taskParameters =
        PcfCommandTaskParameters.builder()
            .pcfCommandRequest(PcfCommandDeployRequest.builder().useAppAutoscalar(true).build())
            .encryptedDataDetails(new ArrayList<>())
            .build();

    List<ExecutionCapability> executionCapabilities = taskParameters.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(3);
    assertThat(executionCapabilities.stream().map(ExecutionCapability::getCapabilityType))
        .containsExactlyInAnyOrder(
            CapabilityType.PCF_CONNECTIVITY, CapabilityType.PROCESS_EXECUTOR, CapabilityType.PCF_AUTO_SCALAR);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesPluginRequest() {
    PcfCommandTaskParameters taskParameters =
        PcfCommandTaskParameters.builder()
            .pcfCommandRequest(
                PcfRunPluginCommandRequest.builder()
                    .pcfCommandType(PcfCommandRequest.PcfCommandType.SETUP)
                    .pcfConfig(PcfConfig.builder().build())
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
            .encryptedDataDetails(new ArrayList<>())
            .build();

    List<ExecutionCapability> executionCapabilities = taskParameters.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(2);
    assertThat(executionCapabilities.stream().map(ExecutionCapability::getCapabilityType))
        .containsExactlyInAnyOrder(CapabilityType.PCF_CONNECTIVITY, CapabilityType.PROCESS_EXECUTOR);
  }
}