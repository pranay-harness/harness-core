package io.harness.connector.task.git;

import static io.harness.delegate.beans.connector.scm.GitConnectionType.ACCOUNT;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.task.ConnectorValidationHandler;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.scm.ScmValidationParams;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import java.util.List;

public abstract class AbstractGitValidationHandler implements ConnectorValidationHandler {
  @Inject private GitCommandTaskHandler gitCommandTaskHandler;

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

    decrypt(gitConfig, scmValidationParams, accountIdentifier);

    final SshSessionConfig sshSessionConfig =
        getSSHSessionConfig(scmValidationParams.getSshKeySpecDTO(), scmValidationParams.getEncryptedDataDetails());

    return gitCommandTaskHandler.validateGitCredentials(
        gitConfig, scmValidationParams.getScmConnector(), accountIdentifier, sshSessionConfig);
  }

  public abstract void decrypt(
      GitConfigDTO gitConfig, ScmValidationParams scmValidationParams, String accountIdentifier);

  public abstract SshSessionConfig getSSHSessionConfig(
      SSHKeySpecDTO sshKeySpecDTO, List<EncryptedDataDetail> encryptionDetails);
}
