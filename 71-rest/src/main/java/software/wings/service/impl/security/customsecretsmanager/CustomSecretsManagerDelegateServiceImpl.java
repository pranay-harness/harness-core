package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static software.wings.service.impl.security.customsecretsmanager.CustomSecretsManagerValidationUtils.OUTPUT_VARIABLE;
import static software.wings.service.impl.security.customsecretsmanager.CustomSecretsManagerValidationUtils.buildShellScriptParameters;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.exception.CommandExecutionException;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.security.encryption.EncryptedRecord;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.delegatetasks.ShellScriptTaskHandler;
import software.wings.delegatetasks.validation.ShellScriptValidationHandler;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.intfc.security.CustomSecretsManagerDelegateService;

@OwnedBy(PL)
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
    if (isEmpty(result) || result.equals("null")) {
      String errorMessage =
          String.format("Empty or null value returned by custom shell script for %s", encryptedData.getName());
      throw new SecretManagementDelegateException(SECRET_MANAGEMENT_ERROR, errorMessage, USER);
    }
    return result.toCharArray();
  }
}
