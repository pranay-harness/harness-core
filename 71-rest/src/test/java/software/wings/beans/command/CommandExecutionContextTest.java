package software.wings.beans.command;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.AwsRegionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.IgnoreValidationCapability;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.command.helpers.SettingAttributeTestHelper;
import software.wings.beans.infrastructure.Host;
import software.wings.delegatetasks.validation.capabilities.SSHHostValidationCapability;
import software.wings.delegatetasks.validation.capabilities.WinrmHostValidationCapability;
import software.wings.utils.WingsTestConstants;

import java.util.List;

public class CommandExecutionContextTest extends WingsBaseTest {
  CommandExecutionContext.Builder contextBuilder =
      aCommandExecutionContext()
          .withAppId(APP_ID)
          .withEnvId(ENV_ID)
          .withAccountId(ACCOUNT_ID)
          .withActivityId(ACTIVITY_ID)
          .withHost(Host.Builder.aHost().withPublicDns(WingsTestConstants.PUBLIC_DNS).build());

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesK8s() {
    CommandExecutionContext executionContext =
        contextBuilder.withDeploymentType(DeploymentType.KUBERNETES.name())
            .withCloudProviderSetting(SettingAttributeTestHelper.obtainKubernetesClusterSettingAttribute(false))
            .build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0)).isExactlyInstanceOf(HttpConnectionExecutionCapability.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesK8sWithDelegate() {
    CommandExecutionContext executionContext =
        contextBuilder.withDeploymentType(DeploymentType.KUBERNETES.name())
            .withCloudProviderSetting(SettingAttributeTestHelper.obtainKubernetesClusterSettingAttribute(true))
            .build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0)).isExactlyInstanceOf(SystemEnvCheckerCapability.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesWinrm() {
    CommandExecutionContext executionContext =
        contextBuilder.withDeploymentType(DeploymentType.WINRM.name())
            .withCloudProviderSetting(SettingAttributeTestHelper.obtainWinrmSettingAttribute())
            .build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0)).isExactlyInstanceOf(WinrmHostValidationCapability.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesSSH() {
    CommandExecutionContext executionContext =
        contextBuilder.withDeploymentType(DeploymentType.SSH.name())
            .withCloudProviderSetting(SettingAttributeTestHelper.obtainSshSettingAttribute())
            .withExecutionCredential(aSSHExecutionCredential().withSshUser(USER_NAME).withSshPassword(PASSWORD).build())
            .build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0)).isExactlyInstanceOf(SSHHostValidationCapability.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesECSResize() {
    CommandExecutionContext executionContext =
        contextBuilder.withDeploymentType(DeploymentType.ECS.name())
            .withContainerResizeParams(SettingAttributeTestHelper.obtainECSResizeParams())
            .build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0)).isExactlyInstanceOf(AwsRegionCapability.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesECSSetup() {
    CommandExecutionContext executionContext =
        contextBuilder.withDeploymentType(DeploymentType.ECS.name())
            .withContainerSetupParams(SettingAttributeTestHelper.obtainECSSetupParams())
            .build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0)).isExactlyInstanceOf(AwsRegionCapability.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesAWSCodeDeploy() {
    CommandExecutionContext executionContext =
        contextBuilder.withDeploymentType(DeploymentType.AWS_CODEDEPLOY.name()).build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0)).isExactlyInstanceOf(HttpConnectionExecutionCapability.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesAMI() {
    CommandExecutionContext executionContext = contextBuilder.withDeploymentType(DeploymentType.AMI.name()).build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0)).isExactlyInstanceOf(IgnoreValidationCapability.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesLambda() {
    CommandExecutionContext executionContext =
        contextBuilder.withDeploymentType(DeploymentType.AWS_LAMBDA.name()).build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0)).isExactlyInstanceOf(IgnoreValidationCapability.class);
  }
}