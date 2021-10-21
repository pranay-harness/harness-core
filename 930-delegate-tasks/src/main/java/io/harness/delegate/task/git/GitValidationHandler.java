package io.harness.delegate.task.git;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.connector.helper.GitApiAccessDecryptionHelper.hasApiAccess;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.ACCOUNT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.connector.task.ConnectorValidationHandler;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.scm.ScmValidationParams;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class GitValidationHandler implements ConnectorValidationHandler {
  @Inject private GitCommandTaskHandler gitCommandTaskHandler;
  @Inject private SecretDecryptionService decryptionService;
  @Inject private GitDecryptionHelper gitDecryptionHelper;

  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    final ScmValidationParams scmValidationParams = (ScmValidationParams) connectorValidationParams;
    GitConfigDTO gitConfig = ScmConnectorMapper.toGitConfigDTO(scmValidationParams.getGitConfigDTO());
    if (gitConfig.getGitConnectionType() == ACCOUNT) {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.SUCCESS)
          .testedAt(System.currentTimeMillis())
          .build();
    }
    gitDecryptionHelper.decryptGitConfig(gitConfig, scmValidationParams.getEncryptedDataDetails());
    SshSessionConfig sshSessionConfig = gitDecryptionHelper.getSSHSessionConfig(
        scmValidationParams.getSshKeySpecDTO(), scmValidationParams.getEncryptedDataDetails());

    if (hasApiAccess(scmValidationParams.getScmConnector())) {
      decryptionService.decrypt(
          GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmValidationParams.getScmConnector()),
          scmValidationParams.getEncryptedDataDetails());
    }
    return gitCommandTaskHandler.validateGitCredentials(scmValidationParams.getGitConfigDTO(),
        scmValidationParams.getScmConnector(), accountIdentifier, sshSessionConfig);
  }
}