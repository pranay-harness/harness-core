package io.harness.connector.task.git;

import static io.harness.connector.helper.GitApiAccessDecryptionHelper.hasApiAccess;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.connector.task.shell.SshSessionConfigMapperViaManager;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.scm.ScmValidationParams;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.ng.core.DecryptableEntityWithEncryptionConsumers;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.remote.client.NGRestClientExecutor;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import java.util.List;

public class GitValidationHandlerViaManager extends AbstractGitValidationHandler {
  @Inject SecretNGManagerClient ngSecretDecryptionClient;
  @Inject NGRestClientExecutor restClientExecutor;
  @Inject SshSessionConfigMapperViaManager sshSessionConfigMapperViaManager;

  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    return super.validate(connectorValidationParams, accountIdentifier);
  }

  @Override
  public void decrypt(GitConfigDTO gitConfig, ScmValidationParams scmValidationParams, String accountIdentifier) {
    final DecryptableEntityWithEncryptionConsumers build =
        DecryptableEntityWithEncryptionConsumers.builder()
            .decryptableEntity(gitConfig.getGitAuth())
            .encryptedDataDetailList(scmValidationParams.getEncryptedDataDetails())
            .build();
    final DecryptableEntity response =
        restClientExecutor.getResponse(ngSecretDecryptionClient.decryptEncryptedDetails(build, accountIdentifier));
    gitConfig.setGitAuth((GitAuthenticationDTO) response);

    if (hasApiAccess(scmValidationParams.getScmConnector())) {
      DecryptableEntity apiAccessDecryptableEntity =
          GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmValidationParams.getScmConnector());
      DecryptableEntityWithEncryptionConsumers decryptableEntityWithEncryptionConsumers =
          buildDecryptableEntityWithEncryptionConsumers(
              apiAccessDecryptableEntity, scmValidationParams.getEncryptedDataDetails());

      final DecryptableEntity decryptedScmSpec =
          restClientExecutor.getResponse(ngSecretDecryptionClient.decryptEncryptedDetails(
              decryptableEntityWithEncryptionConsumers, accountIdentifier));
      GitApiAccessDecryptionHelper.setAPIAccessDecryptableEntity(
          scmValidationParams.getScmConnector(), decryptedScmSpec);
    }
  }

  @Override
  public SshSessionConfig getSSHSessionConfig(
      SSHKeySpecDTO sshKeySpecDTO, List<EncryptedDataDetail> encryptionDetails) {
    if (sshKeySpecDTO == null) {
      return null;
    }
    return sshSessionConfigMapperViaManager.getSSHSessionConfig(sshKeySpecDTO, encryptionDetails);
  }

  private DecryptableEntityWithEncryptionConsumers buildDecryptableEntityWithEncryptionConsumers(
      DecryptableEntity decryptableEntity, List<EncryptedDataDetail> encryptedDataDetails) {
    return DecryptableEntityWithEncryptionConsumers.builder()
        .decryptableEntity(decryptableEntity)
        .encryptedDataDetailList(encryptedDataDetails)
        .build();
  }
}