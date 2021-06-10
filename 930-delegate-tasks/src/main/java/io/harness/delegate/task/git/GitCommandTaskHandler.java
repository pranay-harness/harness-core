package io.harness.delegate.task.git;

import static io.harness.delegate.beans.git.GitCommandExecutionResponse.GitCommandStatus.SUCCESS;
import static io.harness.impl.ScmResponseStatusUtils.convertScmStatusCodeToErrorCode;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.git.NGGitService;
import io.harness.delegate.task.scm.ScmDelegateClient;
import io.harness.eraro.ErrorCode;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.runtime.SCMRuntimeException;
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
    GitCommandExecutionResponse delegateResponseData;
    try {
      delegateResponseData = (GitCommandExecutionResponse) handleValidateTask(
          gitConnector, scmConnector, accountIdentifier, sshSessionConfig);
    } catch (Exception e) {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .testedAt(System.currentTimeMillis())
          .errorSummary(ngErrorHelper.getErrorSummary(e.getMessage()))
          .errors(Collections.singletonList(ngErrorHelper.createErrorDetail(e.getMessage())))
          .build();
    }
    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(delegateResponseData.getConnectorValidationResult().getTestedAt())
        .build();
  }

  public DelegateResponseData handleValidateTask(
      GitConfigDTO gitConfig, ScmConnector scmConnector, String accountId, SshSessionConfig sshSessionConfig) {
    log.info("Processing Git command: VALIDATE");
    gitService.validateOrThrow(gitConfig, accountId, sshSessionConfig);
    handleApiAccessValidation(scmConnector);
    return GitCommandExecutionResponse.builder()
        .gitCommandStatus(SUCCESS)
        .connectorValidationResult(ConnectorValidationResult.builder()
                                       .testedAt(System.currentTimeMillis())
                                       .status(ConnectivityStatus.SUCCESS)
                                       .build())
        .build();
  }

  void handleApiAccessValidation(ScmConnector scmConnector) {
    if (GitApiAccessDecryptionHelper.hasApiAccess(scmConnector)) {
      GetUserReposResponse reposResponse;
      try {
        reposResponse = scmDelegateClient.processScmRequest(
            c -> scmServiceClient.getUserRepos(scmConnector, SCMGrpc.newBlockingStub(c)));
      } catch (Exception e) {
        throw SCMRuntimeException.builder().errorCode(ErrorCode.UNEXPECTED).cause(e).build();
      }
      if (reposResponse != null && reposResponse.getStatus() > 300) {
        ErrorCode errorCode = convertScmStatusCodeToErrorCode(reposResponse.getStatus());
        throw SCMRuntimeException.builder().errorCode(errorCode).message(reposResponse.getError()).build();
      }
    }
  }
}
