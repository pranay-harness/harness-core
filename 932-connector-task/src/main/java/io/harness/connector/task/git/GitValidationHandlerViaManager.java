package io.harness.connector.task.git;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.scm.ScmValidationParams;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.ng.core.DecryptableEntityWithEncryptionConsumers;
import io.harness.remote.client.NGRestClientExecutor;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;

public class GitValidationHandlerViaManager extends AbstractGitValidationHandler {
  @Inject SecretNGManagerClient secretManagerClient;
  @Inject NGRestClientExecutor restClientExecutor;

  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    return super.validate(connectorValidationParams, accountIdentifier);
  }

  @Override
  public SshSessionConfig decrypt(
      GitConfigDTO gitConfig, ScmValidationParams scmValidationParams, String accountIdentifier) {
    final DecryptableEntityWithEncryptionConsumers build =
        DecryptableEntityWithEncryptionConsumers.builder()
            .decryptableEntity(gitConfig.getGitAuth())
            .encryptedDataDetailList(scmValidationParams.getEncryptedDataDetails())
            .build();
    final DecryptableEntity response =
        restClientExecutor.getResponse(secretManagerClient.decryptUsingManager(build, accountIdentifier));
    gitConfig.setGitAuth((GitAuthenticationDTO) response);
    return null;
  }
}
