package io.harness.connector.task.git;

import com.google.inject.Inject;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.scm.ScmValidationParams;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.secrets.remote.SecretNGManagerClient;
import io.harness.shell.SshSessionConfig;

public class GitValidationHandlerViaManager extends AbstractGitValidationHandler {
    @Inject
    SecretNGManagerClient secretManagerClient;

    public ConnectorValidationResult validate(
            ConnectorValidationParams connectorValidationParams, String accountIdentifier) {

        return super.validate(connectorValidationParams,accountIdentifier);
    }

    @Override
    public SshSessionConfig decrypt(GitConfigDTO gitConfig, ScmValidationParams scmValidationParams, String accountIdentifier) {
        secretManagerClient.decryptUsingManager(gitConfig.getGitAuth(),scmValidationParams.getEncryptedDataDetails(), accountIdentifier );
        return null;
    }
}
