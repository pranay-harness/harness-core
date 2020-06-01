package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.service.impl.security.customsecretsmanager.CustomSecretsManagerUtils.obtainConfig;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingValue.SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.exception.CommandExecutionException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.delegatetasks.ShellScriptTaskHandler;
import software.wings.delegatetasks.validation.ShellScriptValidationHandler;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;

import java.util.HashMap;
import java.util.Map;

public class CustomSecretsManagerDelegateServiceImplTest extends CategoryTest {
  @Mock private ShellScriptTaskHandler shellScriptTaskHandler;
  @Mock private ShellScriptValidationHandler shellScriptValidationHandler;
  @Inject @InjectMocks private CustomSecretsManagerDelegateServiceImpl customSecretsManagerDelegateService;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_isExecutableOnDelegate_shouldReturnTrue() {
    CustomSecretsManagerConfig config = obtainConfig(null);
    when(shellScriptValidationHandler.handle(any(ShellScriptParameters.class))).thenReturn(true);
    boolean isValidated = customSecretsManagerDelegateService.isExecutableOnDelegate(config);
    assertThat(isValidated).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_isExecutableOnDelegate_shouldReturnFalse() {
    CustomSecretsManagerConfig config = obtainConfig(null);
    when(shellScriptValidationHandler.handle(any(ShellScriptParameters.class))).thenReturn(false);
    boolean isValidated = customSecretsManagerDelegateService.isExecutableOnDelegate(config);
    assertThat(isValidated).isFalse();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_fetchSecret_shouldPass1() {
    CustomSecretsManagerConfig config = obtainConfig(HOST_CONNECTION_ATTRIBUTES);
    EncryptedRecord encryptedRecord = mock(EncryptedRecord.class);
    CommandExecutionResult commandExecutionResult = mock(CommandExecutionResult.class);
    ShellExecutionData shellExecutionData = mock(ShellExecutionData.class);
    Map<String, String> map = new HashMap<>();
    map.put("secret", "value");
    when(shellExecutionData.getSweepingOutputEnvVariables()).thenReturn(map);
    when(commandExecutionResult.getCommandExecutionData()).thenReturn(shellExecutionData);
    when(commandExecutionResult.getStatus()).thenReturn(SUCCESS);
    when(shellScriptTaskHandler.handle(any(ShellScriptParameters.class))).thenReturn(commandExecutionResult);
    char[] secret = customSecretsManagerDelegateService.fetchSecret(encryptedRecord, config);
    assertThat(secret).isEqualTo("value".toCharArray());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_fetchSecret_shouldPass2() {
    CustomSecretsManagerConfig config = obtainConfig(WINRM_CONNECTION_ATTRIBUTES);
    EncryptedRecord encryptedRecord = mock(EncryptedRecord.class);
    CommandExecutionResult commandExecutionResult = mock(CommandExecutionResult.class);
    ShellExecutionData shellExecutionData = mock(ShellExecutionData.class);
    Map<String, String> map = new HashMap<>();
    map.put("secret", "value");
    when(shellExecutionData.getSweepingOutputEnvVariables()).thenReturn(map);
    when(commandExecutionResult.getCommandExecutionData()).thenReturn(shellExecutionData);
    when(commandExecutionResult.getStatus()).thenReturn(SUCCESS);
    when(shellScriptTaskHandler.handle(any(ShellScriptParameters.class))).thenReturn(commandExecutionResult);
    char[] secret = customSecretsManagerDelegateService.fetchSecret(encryptedRecord, config);
    assertThat(secret).isEqualTo("value".toCharArray());
  }

  @Test(expected = CommandExecutionException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void test_fetchSecret_shouldFail() {
    CustomSecretsManagerConfig config = obtainConfig(HOST_CONNECTION_ATTRIBUTES);
    EncryptedRecord encryptedRecord = mock(EncryptedRecord.class);
    CommandExecutionResult commandExecutionResult = mock(CommandExecutionResult.class);
    when(commandExecutionResult.getStatus()).thenReturn(FAILURE);
    when(shellScriptTaskHandler.handle(any(ShellScriptParameters.class))).thenReturn(commandExecutionResult);
    customSecretsManagerDelegateService.fetchSecret(encryptedRecord, config);
  }
}
