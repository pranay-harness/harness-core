package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.service.impl.security.customsecretsmanager.CustomSecretsManagerValidationUtils.OUTPUT_VARIABLE;
import static software.wings.service.impl.security.customsecretsmanager.CustomSecretsManagerValidationUtils.buildShellScriptParameters;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult;
import io.harness.exception.CommandExecutionException;
import io.harness.security.encryption.EncryptedRecord;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.delegatetasks.ShellScriptTaskHandler;
import software.wings.delegatetasks.validation.ShellScriptValidationHandler;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.intfc.security.CustomSecretsManagerDelegateService;

@Singleton
public class CustomSecretsManagerDelegateServiceImpl implements CustomSecretsManagerDelegateService {
  private ShellScriptTaskHandler shellScriptTaskHandler;
  private ShellScriptValidationHandler shellScriptValidationHandler;

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
}
