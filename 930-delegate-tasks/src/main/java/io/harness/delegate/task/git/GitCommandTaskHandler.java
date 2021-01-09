package io.harness.delegate.task.git;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus.SUCCESS;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.ConnectivityStatus;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.delegate.git.NGGitService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class GitCommandTaskHandler {
  @Inject private SecretDecryptionService decryptionService;
  @Inject private NGGitService gitService;

  public ConnectorValidationResult validateGitCredentials(
      GitConfigDTO gitConnector, String accountIdentifier, List<EncryptedDataDetail> encryptionDetailList) {
    decryptionService.decrypt(gitConnector.getGitAuth(), encryptionDetailList);
    GitCommandExecutionResponse delegateResponseData =
        (GitCommandExecutionResponse) handleValidateTask(gitConnector, accountIdentifier);
    return ConnectorValidationResult.builder()
        .status(delegateResponseData.getGitCommandStatus() == SUCCESS ? ConnectivityStatus.SUCCESS
                                                                      : ConnectivityStatus.FAILURE)
        .build();
  }

  public DelegateResponseData handleValidateTask(GitConfigDTO gitConfig, String accountId) {
    log.info("Processing Git command: VALIDATE");
    String errorMessage = gitService.validate(gitConfig, accountId);
    if (isEmpty(errorMessage)) {
      return GitCommandExecutionResponse.builder().gitCommandStatus(SUCCESS).build();
    } else {
      return GitCommandExecutionResponse.builder()
          .gitCommandStatus(GitCommandStatus.FAILURE)
          .errorMessage(errorMessage)
          .build();
    }
  }
}
