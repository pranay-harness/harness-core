package software.wings.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.REPO;
import static io.harness.delegate.beans.connector.scm.github.GithubApiAccessType.TOKEN;

import static software.wings.beans.GitConfig.ProviderType.GITHUB;
import static software.wings.beans.GitConfig.ProviderType.GITLAB;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpecDTO;
import io.harness.encryption.SecretRefData;

import software.wings.beans.GitConfig;

import com.google.inject.Singleton;
import java.util.Arrays;

@Singleton
@OwnedBy(CDP)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class ScmFetchFilesHelper {
  public boolean shouldUseScm(boolean isOptimizedFilesFetch, GitConfig gitConfig) {
    return isOptimizedFilesFetch && gitConfig.getSshSettingAttribute() == null
        && Arrays.asList(GITHUB, GITLAB).contains(gitConfig.getProviderType());
  }

  public ScmConnector getScmConnector(GitConfig gitConfig) {
    switch (gitConfig.getProviderType()) {
      case GITHUB:
        return getGitHubConnector(gitConfig);
      case GITLAB:
        return getGitLabConnector(gitConfig);
      default:
        return null;
    }
  }

  private GithubConnectorDTO getGitHubConnector(GitConfig gitConfig) {
    return GithubConnectorDTO.builder()
        .url(gitConfig.getRepoUrl())
        .connectionType(REPO)
        .apiAccess(GithubApiAccessDTO.builder()
                       .type(TOKEN)
                       .spec(GithubTokenSpecDTO.builder()
                                 .tokenRef(SecretRefData.builder().decryptedValue(gitConfig.getPassword()).build())
                                 .build())
                       .build())
        .build();
  }

  private GitlabConnectorDTO getGitLabConnector(GitConfig gitConfig) {
    return GitlabConnectorDTO.builder()
        .url(gitConfig.getRepoUrl())
        .connectionType(REPO)
        .apiAccess(GitlabApiAccessDTO.builder()
                       .spec(GitlabTokenSpecDTO.builder()
                                 .tokenRef(SecretRefData.builder().decryptedValue(gitConfig.getPassword()).build())
                                 .build())
                       .build())
        .build();
  }
}
