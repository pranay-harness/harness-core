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
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
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
          .appId(APP_ID)
          .envId(ENV_ID)
          .accountId(ACCOUNT_ID)
          .activityId(ACTIVITY_ID)
          .host(Host.Builder.aHost().withPublicDns(WingsTestConstants.PUBLIC_DNS).build());

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesK8s() {
    CommandExecutionContext executionContext =
        contextBuilder.deploymentType(DeploymentType.KUBERNETES.name())
            .cloudProviderSetting(SettingAttributeTestHelper.obtainKubernetesClusterSettingAttribute(false))
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
        contextBuilder.deploymentType(DeploymentType.KUBERNETES.name())
            .cloudProviderSetting(SettingAttributeTestHelper.obtainKubernetesClusterSettingAttribute(true))
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
        contextBuilder.deploymentType(DeploymentType.WINRM.name())
            .cloudProviderSetting(SettingAttributeTestHelper.obtainWinrmSettingAttribute())
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
        contextBuilder.deploymentType(DeploymentType.SSH.name())
            .cloudProviderSetting(SettingAttributeTestHelper.obtainSshSettingAttribute())
            .executionCredential(aSSHExecutionCredential().withSshUser(USER_NAME).withSshPassword(PASSWORD).build())
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
        contextBuilder.deploymentType(DeploymentType.ECS.name())
            .containerResizeParams(SettingAttributeTestHelper.obtainECSResizeParams())
            .build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).isEmpty();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesECSSetup() {
    CommandExecutionContext executionContext =
        contextBuilder.deploymentType(DeploymentType.ECS.name())
            .containerSetupParams(SettingAttributeTestHelper.obtainECSSetupParams())
            .build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).isEmpty();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesAWSCodeDeploy() {
    CommandExecutionContext executionContext =
        contextBuilder.deploymentType(DeploymentType.AWS_CODEDEPLOY.name()).build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0)).isExactlyInstanceOf(HttpConnectionExecutionCapability.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesAMI() {
    CommandExecutionContext executionContext = contextBuilder.deploymentType(DeploymentType.AMI.name()).build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(0);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchRequiredExecutionCapabilitiesLambda() {
    CommandExecutionContext executionContext = contextBuilder.deploymentType(DeploymentType.AWS_LAMBDA.name()).build();

    List<ExecutionCapability> executionCapabilities = executionContext.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).hasSize(0);
  }
}