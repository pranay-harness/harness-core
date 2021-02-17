package software.wings.beans.delegation;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.KmsConfig;
import software.wings.beans.SettingAttribute;

import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TerraformProvisionParametersTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void fetchRequiredExecutionCapabilities() {
    testWithGitConfig();
    testWithGitConfigSSHConnection();
    testWithoutGitConfig();
    testWithSecretManagerConfig();
  }

  private void testWithoutGitConfig() {
    assertThat(TerraformProvisionParameters.builder()
                   .build()
                   .fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.PROCESS_EXECUTOR);
  }

  private void testWithGitConfig() {
    TerraformProvisionParameters parameters =
        TerraformProvisionParameters.builder()
            .sourceRepo(GitConfig.builder().repoUrl("https://github.com/abc").build())
            .build();
    assertThat(parameters.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.HTTP, CapabilityType.PROCESS_EXECUTOR);
  }

  private void testWithGitConfigSSHConnection() {
    HostConnectionAttributes hostConnectionAttributes = new HostConnectionAttributes();
    hostConnectionAttributes.setSshPort(22);
    SettingAttribute sshSettingAttribute = new SettingAttribute();
    sshSettingAttribute.setValue(hostConnectionAttributes);
    TerraformProvisionParameters parameters =
        TerraformProvisionParameters.builder()
            .sourceRepo(
                GitConfig.builder().repoUrl("git@github.com:abc").sshSettingAttribute(sshSettingAttribute).build())
            .build();
    assertThat(parameters.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.SOCKET, CapabilityType.PROCESS_EXECUTOR);
  }

  private void testWithSecretManagerConfig() {
    TerraformProvisionParameters parameters =
        TerraformProvisionParameters.builder()
            .sourceRepo(GitConfig.builder().repoUrl("https://github.com/abc").build())
            .secretManagerConfig(KmsConfig.builder().build())
            .build();
    assertThat(parameters.fetchRequiredExecutionCapabilities(null)
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder(CapabilityType.HTTP, CapabilityType.PROCESS_EXECUTOR, CapabilityType.HTTP);
  }
}
