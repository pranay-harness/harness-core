package software.wings.security.encryption.secretsmanagerconfigs;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType.BASH;
import static software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType.POWERSHELL;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingValue.SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig.CustomSecretsManagerConfigBuilder;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CustomSecretsManagerConfigTest extends CategoryTest {
  private static CustomSecretsManagerConfigBuilder getBaseBuilder(ScriptType scriptType) {
    CustomSecretsManagerShellScript shellScript = CustomSecretsManagerShellScript.builder()
                                                      .scriptString(UUIDGenerator.generateUuid())
                                                      .scriptType(scriptType)
                                                      .variables(new ArrayList<>())
                                                      .build();
    return CustomSecretsManagerConfig.builder()
        .name("CustomSecretsManager")
        .templateId(UUIDGenerator.generateUuid())
        .delegateSelectors(new ArrayList<>())
        .isConnectorTemplatized(false)
        .customSecretsManagerShellScript(shellScript)
        .testVariables(new HashSet<>());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_fetchRequiredExecutionCapabilities_executeOnDelegate_bash() {
    CustomSecretsManagerConfig config = getBaseBuilder(BASH).executeOnDelegate(true).build();
    List<ExecutionCapability> executionCapabilities = config.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).isNotNull();
    assertThat(executionCapabilities).hasSize(0);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_fetchRequiredExecutionCapabilities_executeOnDelegate_powershell() {
    CustomSecretsManagerConfig config = getBaseBuilder(POWERSHELL).executeOnDelegate(true).build();
    List<ExecutionCapability> executionCapabilities = config.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).isNotNull();
    assertThat(executionCapabilities).hasSize(1);
    ExecutionCapability executionCapability = executionCapabilities.get(0);
    assertThat(executionCapability instanceof ProcessExecutorCapability).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_fetchRequiredExecutionCapabilities_executeRemote_bash() {
    String host = "app.harness.io";
    Integer port = 22;
    HostConnectionAttributes remoteHostConnector = mock(HostConnectionAttributes.class);
    when(remoteHostConnector.getSettingType()).thenReturn(HOST_CONNECTION_ATTRIBUTES);
    when(remoteHostConnector.getSshPort()).thenReturn(port);
    CustomSecretsManagerConfig config =
        getBaseBuilder(BASH).executeOnDelegate(false).host(host).remoteHostConnector(remoteHostConnector).build();
    List<ExecutionCapability> executionCapabilities = config.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).isNotNull();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0) instanceof SocketConnectivityExecutionCapability).isTrue();
    SocketConnectivityExecutionCapability executionCapability =
        (SocketConnectivityExecutionCapability) executionCapabilities.get(0);
    assertThat(executionCapability.getHostName()).isEqualTo(host);
    assertThat(executionCapability.getPort()).isEqualTo(port.toString());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_fetchRequiredExecutionCapabilities_executeRemote_powershell() {
    String host = "app.harness.io";
    int port = 5986;
    WinRmConnectionAttributes remoteHostConnector = mock(WinRmConnectionAttributes.class);
    when(remoteHostConnector.getSettingType()).thenReturn(WINRM_CONNECTION_ATTRIBUTES);
    when(remoteHostConnector.getPort()).thenReturn(port);
    CustomSecretsManagerConfig config =
        getBaseBuilder(POWERSHELL).executeOnDelegate(false).host(host).remoteHostConnector(remoteHostConnector).build();
    List<ExecutionCapability> executionCapabilities = config.fetchRequiredExecutionCapabilities();
    assertThat(executionCapabilities).isNotNull();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0) instanceof SocketConnectivityExecutionCapability).isTrue();
    SocketConnectivityExecutionCapability executionCapability =
        (SocketConnectivityExecutionCapability) executionCapabilities.get(0);
    assertThat(executionCapability.getHostName()).isEqualTo(host);
    assertThat(executionCapability.getPort()).isEqualTo(Integer.toString(port));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_getValidationCriteria_executeOnDelegate() {
    CustomSecretsManagerConfig config = getBaseBuilder(BASH).executeOnDelegate(true).build();
    String criteria = config.getValidationCriteria();
    assertThat(criteria).isEqualTo("localhost");
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_getValidationCriteria_executeRemote() {
    String host = "app.harness.io";
    CustomSecretsManagerConfig config = getBaseBuilder(BASH).executeOnDelegate(false).host(host).build();
    String criteria = config.getValidationCriteria();
    assertThat(criteria).isEqualTo(host);
  }
}
