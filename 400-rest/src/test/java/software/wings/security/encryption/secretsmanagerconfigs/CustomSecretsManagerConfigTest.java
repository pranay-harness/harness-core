/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.encryption.secretsmanagerconfigs;

import static io.harness.annotations.dev.HarnessModule._360_CG_MANAGER;
import static io.harness.rule.OwnerRule.UTKARSH;

import static software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType.BASH;
import static software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType.POWERSHELL;
import static software.wings.settings.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;
import io.harness.rule.Owner;

import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.delegatetasks.validation.capabilities.ShellConnectionCapability;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig.CustomSecretsManagerConfigBuilder;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
@OwnedBy(HarnessTeam.PL)
@TargetModule(_360_CG_MANAGER)
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
        .delegateSelectors(new HashSet<>())
        .isConnectorTemplatized(false)
        .customSecretsManagerShellScript(shellScript)
        .testVariables(new HashSet<>());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_fetchRequiredExecutionCapabilities_executeOnDelegate_bash() {
    CustomSecretsManagerConfig config = getBaseBuilder(BASH).executeOnDelegate(true).build();
    List<ExecutionCapability> executionCapabilities = config.fetchRequiredExecutionCapabilities(null);
    assertThat(executionCapabilities).isNotNull();
    assertThat(executionCapabilities).hasSize(0);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_fetchRequiredExecutionCapabilities_executeOnDelegate_powershell() {
    CustomSecretsManagerConfig config = getBaseBuilder(POWERSHELL).executeOnDelegate(true).build();
    List<ExecutionCapability> executionCapabilities = config.fetchRequiredExecutionCapabilities(null);
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
    List<ExecutionCapability> executionCapabilities = config.fetchRequiredExecutionCapabilities(null);
    assertThat(executionCapabilities).isNotNull();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0) instanceof ShellConnectionCapability).isTrue();
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
    List<ExecutionCapability> executionCapabilities = config.fetchRequiredExecutionCapabilities(null);
    assertThat(executionCapabilities).isNotNull();
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0) instanceof ShellConnectionCapability).isTrue();
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
