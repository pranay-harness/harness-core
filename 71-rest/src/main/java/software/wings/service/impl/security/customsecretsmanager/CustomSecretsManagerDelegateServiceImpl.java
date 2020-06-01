package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.sm.states.ShellScriptState.ConnectionType.SSH;
import static software.wings.sm.states.ShellScriptState.ConnectionType.WINRM;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.exception.CommandExecutionException;
import io.harness.security.encryption.EncryptedRecord;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.beans.delegation.ShellScriptParameters.ShellScriptParametersBuilder;
import software.wings.delegatetasks.ShellScriptTaskHandler;
import software.wings.delegatetasks.validation.ShellScriptValidationHandler;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.intfc.security.CustomSecretsManagerDelegateService;

import java.util.ArrayList;

@Singleton
public class CustomSecretsManagerDelegateServiceImpl implements CustomSecretsManagerDelegateService {
  private ShellScriptTaskHandler shellScriptTaskHandler;
  private ShellScriptValidationHandler shellScriptValidationHandler;
  private static final String OUTPUT_VARIABLE = "secret";

  @Inject
  CustomSecretsManagerDelegateServiceImpl(
      ShellScriptTaskHandler shellScriptTaskHandler, ShellScriptValidationHandler shellScriptValidationHandler) {
    this.shellScriptTaskHandler = shellScriptTaskHandler;
    this.shellScriptValidationHandler = shellScriptValidationHandler;
  }

  @Override
  public boolean isExecutableOnDelegate(CustomSecretsManagerConfig customSecretsManagerConfig) {
    ShellScriptParameters shellScriptParameters = buildShellScriptParameters(customSecretsManagerConfig);
    return shellScriptValidationHandler.handle(shellScriptParameters);
  }

  @Override
  public char[] fetchSecret(EncryptedRecord encryptedData, CustomSecretsManagerConfig customSecretsManagerConfig) {
    ShellScriptParameters shellScriptParameters = buildShellScriptParameters(customSecretsManagerConfig);
    CommandExecutionResult commandExecutionResult = shellScriptTaskHandler.handle(shellScriptParameters);
    if (commandExecutionResult.getStatus() != SUCCESS) {
      String errorMessage = String.format("Could not retrieve secret %s due to error", encryptedData.getName());
      throw new CommandExecutionException(errorMessage);
    }
    ShellExecutionData shellExecutionData = (ShellExecutionData) commandExecutionResult.getCommandExecutionData();
    String result = shellExecutionData.getSweepingOutputEnvVariables().get(OUTPUT_VARIABLE);
    return result == null ? null : result.toCharArray();
  }

  private static ShellScriptParameters buildShellScriptParameters(
      CustomSecretsManagerConfig customSecretsManagerConfig) {
    ShellScriptParametersBuilder shellScriptParametersBuilder =
        ShellScriptParameters.builder()
            .accountId(customSecretsManagerConfig.getAccountId())
            .host(customSecretsManagerConfig.getHost())
            .workingDirectory(customSecretsManagerConfig.getCommandPath())
            .scriptType(ScriptType.valueOf(
                customSecretsManagerConfig.getCustomSecretsManagerShellScript().getScriptType().name()))
            .script(customSecretsManagerConfig.getCustomSecretsManagerShellScript().getScriptString())
            .executeOnDelegate(customSecretsManagerConfig.isExecuteOnDelegate())
            .keyEncryptedDataDetails(new ArrayList<>())
            .winrmConnectionEncryptedDataDetails(new ArrayList<>())
            .activityId(UUIDGenerator.generateUuid())
            .appId(GLOBAL_APP_ID)
            .outputVars(OUTPUT_VARIABLE);

    if (!customSecretsManagerConfig.isExecuteOnDelegate()) {
      if (customSecretsManagerConfig.getRemoteHostConnector().getSettingType() == HOST_CONNECTION_ATTRIBUTES) {
        HostConnectionAttributes hostConnectionAttributes =
            (HostConnectionAttributes) customSecretsManagerConfig.getRemoteHostConnector();
        shellScriptParametersBuilder.connectionType(SSH)
            .hostConnectionAttributes(hostConnectionAttributes)
            .userName(hostConnectionAttributes.getUserName())
            .keyless(hostConnectionAttributes.isKeyless())
            .keyPath(hostConnectionAttributes.getKeyPath())
            .port(hostConnectionAttributes.getSshPort())
            .accessType(hostConnectionAttributes.getAccessType())
            .authenticationScheme(hostConnectionAttributes.getAuthenticationScheme())
            .kerberosConfig(hostConnectionAttributes.getKerberosConfig());
      } else {
        WinRmConnectionAttributes winRmConnectionAttributes =
            (WinRmConnectionAttributes) customSecretsManagerConfig.getRemoteHostConnector();
        shellScriptParametersBuilder.connectionType(WINRM)
            .winrmConnectionAttributes(winRmConnectionAttributes)
            .userName(winRmConnectionAttributes.getUsername());
      }
    }
    return shellScriptParametersBuilder.build();
  }
}
