package io.harness.delegate.task.git;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus.SUCCESS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.ConnectorValidationResult.ConnectorValidationResultBuilder;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus;
import io.harness.delegate.git.NGGitService;
import io.harness.delegate.task.scm.ScmDelegateClient;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.product.ci.scm.proto.GetUserReposResponse;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.service.ScmServiceClient;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.DX)
public class GitCommandTaskHandler {
  @Inject private NGGitService gitService;
  @Inject private NGErrorHelper ngErrorHelper;
  @Inject private ScmDelegateClient scmDelegateClient;
  @Inject private ScmServiceClient scmServiceClient;

  public ConnectorValidationResult validateGitCredentials(GitConfigDTO gitConnector, ScmConnector scmConnector,
      String accountIdentifier, SshSessionConfig sshSessionConfig) {
    GitCommandExecutionResponse delegateResponseData = (GitCommandExecutionResponse) handleValidateTask(
        gitConnector, scmConnector, accountIdentifier, sshSessionConfig);
    if (delegateResponseData.getGitCommandStatus() == SUCCESS) {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.SUCCESS)
          .testedAt(delegateResponseData.getConnectorValidationResult().getTestedAt())
          .build();
    } else {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .testedAt(delegateResponseData.getConnectorValidationResult().getTestedAt())
          .errorSummary(delegateResponseData.getConnectorValidationResult().getErrorSummary())
          .errors(delegateResponseData.getConnectorValidationResult().getErrors())
          .build();
    }
  }

  public DelegateResponseData handleValidateTask(
      GitConfigDTO gitConfig, ScmConnector scmConnector, String accountId, SshSessionConfig sshSessionConfig) {
    log.info("Processing Git command: VALIDATE");
    String errorMessage = gitService.validate(gitConfig, accountId, sshSessionConfig);
    if (isEmpty(errorMessage)) {
      errorMessage = handleApiAccessValidation(scmConnector);
    }
    ConnectorValidationResultBuilder builder = ConnectorValidationResult.builder().testedAt(System.currentTimeMillis());
    if (isEmpty(errorMessage)) {
      return GitCommandExecutionResponse.builder()
          .gitCommandStatus(SUCCESS)
          .connectorValidationResult(builder.status(ConnectivityStatus.SUCCESS).build())
          .build();
    } else {
      builder.status(ConnectivityStatus.FAILURE)
          .errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
          .errors(Collections.singletonList(ngErrorHelper.createErrorDetail(errorMessage)));
      return GitCommandExecutionResponse.builder()
          .gitCommandStatus(GitCommandStatus.FAILURE)
          .errorMessage(errorMessage)
          .connectorValidationResult(builder.build())
          .build();
    }
  }

  String handleApiAccessValidation(ScmConnector scmConnector) {
    if (GitApiAccessDecryptionHelper.hasApiAccess(scmConnector)) {
      GetUserReposResponse reposResponse = scmDelegateClient.processScmRequest(
          c -> scmServiceClient.getUserRepos(scmConnector, SCMGrpc.newBlockingStub(c)));
      if (reposResponse.getStatus() > 300) {
        return reposResponse.getError();
      } else {
        return null;
      }
    }
    return null;
  }
}
