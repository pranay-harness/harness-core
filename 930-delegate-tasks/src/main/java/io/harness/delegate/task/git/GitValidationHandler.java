package io.harness.delegate.task.git;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.connector.task.git.AbstractGitValidationHandler;
import io.harness.connector.task.git.GitCommandTaskHandler;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.scm.ScmValidationParams;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.SshSessionConfig;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.connector.helper.GitApiAccessDecryptionHelper.hasApiAccess;

@Singleton
@OwnedBy(DX)
public class GitValidationHandler extends AbstractGitValidationHandler {
  @Inject private GitCommandTaskHandler gitCommandTaskHandler;
  @Inject(optional = true) private SecretDecryptionService decryptionService;
  @Inject private GitDecryptionHelper gitDecryptionHelper;

  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
     return super.validate(connectorValidationParams,accountIdentifier);
  }

  @Override
  public SshSessionConfig decrypt(GitConfigDTO gitConfig, ScmValidationParams scmValidationParams, String accountIdentifier) {
    gitDecryptionHelper.decryptGitConfig(gitConfig, scmValidationParams.getEncryptedDataDetails());


    if (hasApiAccess(scmValidationParams.getScmConnector())) {
      decryptionService.decrypt(
              GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmValidationParams.getScmConnector()),
              scmValidationParams.getEncryptedDataDetails());
    }
    return gitDecryptionHelper.getSSHSessionConfig(
            scmValidationParams.getSshKeySpecDTO(), scmValidationParams.getEncryptedDataDetails());
  }


}