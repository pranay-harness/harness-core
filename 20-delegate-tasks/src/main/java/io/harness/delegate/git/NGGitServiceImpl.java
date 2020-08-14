package io.harness.delegate.git;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.git.GitClientV2;
import io.harness.git.model.AuthRequest;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.UsernamePasswordAuthRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class NGGitServiceImpl implements NGGitService {
  @Inject private GitClientV2 gitClientV2;

  @Override
  public String validate(GitConfigDTO gitConfig, String accountId) {
    return gitClientV2.validate(getGitBaseRequest(gitConfig, accountId));
  }

  @VisibleForTesting
  GitBaseRequest getGitBaseRequest(GitConfigDTO gitConfig, String accountId) {
    // todo @arvind: change repotype
    return GitBaseRequest.builder()
        .authRequest(getGitConnectionType(gitConfig))
        .branch(gitConfig.getGitAuth().getBranchName())
        .repoType("YAML")
        .repoUrl(gitConfig.getGitAuth().getUrl())
        .accountId(accountId)
        .build();
  }

  private AuthRequest getGitConnectionType(GitConfigDTO gitConfig) {
    switch (gitConfig.getGitAuthType()) {
      case SSH:
        // todo @deepak @vaibhav: handle ssh auth when added for ng.
        throw new NotImplementedException("Ssh not yet implemented");
      case HTTP:
        // todo @deepak @vaibhav: handle kerboros when added.
        GitHTTPAuthenticationDTO httpAuthenticationDTO = (GitHTTPAuthenticationDTO) gitConfig.getGitAuth();
        return UsernamePasswordAuthRequest.builder()
            .username(httpAuthenticationDTO.getUsername())
            .password(httpAuthenticationDTO.getPasswordRef().getDecryptedValue())
            .build();
      default:
        throw new InvalidRequestException("Unknown auth type.");
    }
  }
}
