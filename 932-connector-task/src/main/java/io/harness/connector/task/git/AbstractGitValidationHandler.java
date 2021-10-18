package io.harness.connector.task.git;

import com.google.inject.Inject;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationHandler;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.scm.ScmValidationParams;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.shell.SshSessionConfig;

import static io.harness.delegate.beans.connector.scm.GitConnectionType.ACCOUNT;

public abstract  class AbstractGitValidationHandler implements ConnectorValidationHandler {
    @Inject
    private GitCommandTaskHandler gitCommandTaskHandler;

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
        final SshSessionConfig sshSessionConfig = decrypt(gitConfig, scmValidationParams, accountIdentifier);

        return gitCommandTaskHandler.validateGitCredentials(scmValidationParams.getGitConfigDTO(),
                scmValidationParams.getScmConnector(), accountIdentifier, sshSessionConfig);
    }

    public abstract SshSessionConfig decrypt( GitConfigDTO gitConfig,ScmValidationParams scmValidationParams, String accountIdentifier );
}
