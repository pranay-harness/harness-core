package io.harness.delegate.task.git;

import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.scm.ScmValidationParams;
import io.harness.delegate.task.k8s.ConnectorValidationHandler;

import com.google.inject.Inject;

public class GitValidationHandler implements ConnectorValidationHandler {
  @Inject private GitCommandTaskHandler gitCommandTaskHandler;
  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    final ScmValidationParams scmValidationParams = (ScmValidationParams) connectorValidationParams;
    // TODO Deepak: Ssh handle
    return gitCommandTaskHandler.validateGitCredentials(
        scmValidationParams.getGitConfigDTO(), accountIdentifier, scmValidationParams.getEncryptedDataDetails(), null);
  }
}