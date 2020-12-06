package software.wings.service.impl.security.customsecretsmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.service.impl.security.customsecretsmanager.CustomSecretsManagerValidationUtils.buildShellScriptParameters;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.delegatetasks.validation.ShellScriptValidationHandler;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.intfc.security.CustomSecretsManagerValidation;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(PL)
@Singleton
public class CustomSecretsManagerValidationImpl implements CustomSecretsManagerValidation {
  private final ShellScriptValidationHandler shellScriptValidationHandler;

  @Inject
  CustomSecretsManagerValidationImpl(ShellScriptValidationHandler shellScriptValidationHandler) {
    this.shellScriptValidationHandler = shellScriptValidationHandler;
  }

  @Override
  public boolean isExecutableOnDelegate(CustomSecretsManagerConfig customSecretsManagerConfig) {
    ShellScriptParameters shellScriptParameters = buildShellScriptParameters(customSecretsManagerConfig);
    return shellScriptValidationHandler.handle(shellScriptParameters);
  }
}
