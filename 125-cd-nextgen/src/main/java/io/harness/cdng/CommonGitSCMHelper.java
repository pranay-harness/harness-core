package io.harness.cdng;

import static io.harness.beans.FeatureName.OPTIMIZED_GIT_FETCH_FILES;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.ListUtils.trimStrings;
import static io.harness.validation.Validator.notEmptyCheck;

import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import io.harness.beans.DecryptableEntity;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameTokenDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernameTokenDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import javax.annotation.Nonnull;

public class CommonGitSCMHelper {
  @Inject private GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;
  @Named("PRIVILEGED") @Inject private SecretManagerClientService secretManagerClientService;
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;

  public boolean isGitlabTokenAuth(ScmConnector scmConnector) {
    return scmConnector instanceof GitlabConnectorDTO
        && (((GitlabConnectorDTO) scmConnector).getApiAccess() != null
            || isGitlabUsernameTokenAuth((GitlabConnectorDTO) scmConnector));
  }

  public boolean isGitlabUsernameTokenAuth(GitlabConnectorDTO gitlabConnectorDTO) {
    return gitlabConnectorDTO.getAuthentication().getCredentials() instanceof GitlabHttpCredentialsDTO
        && ((GitlabHttpCredentialsDTO) gitlabConnectorDTO.getAuthentication().getCredentials())
               .getType()
               .equals(GitlabHttpAuthenticationType.USERNAME_AND_TOKEN);
  }

  public boolean isGithubUsernameTokenAuth(GithubConnectorDTO githubConnectorDTO) {
    return githubConnectorDTO.getAuthentication().getCredentials() instanceof GithubHttpCredentialsDTO
        && ((GithubHttpCredentialsDTO) githubConnectorDTO.getAuthentication().getCredentials())
               .getType()
               .equals(GithubHttpAuthenticationType.USERNAME_AND_TOKEN);
  }

  public boolean isGithubTokenAuth(ScmConnector scmConnector) {
    return scmConnector instanceof GithubConnectorDTO
        && (((GithubConnectorDTO) scmConnector).getApiAccess() != null
            || isGithubUsernameTokenAuth((GithubConnectorDTO) scmConnector));
  }

  public SSHKeySpecDTO getSshKeySpecDTO(GitConfigDTO gitConfigDTO, Ambiance ambiance) {
    return gitConfigAuthenticationInfoHelper.getSSHKey(gitConfigDTO, AmbianceUtils.getAccountId(ambiance),
        AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));
  }

  public boolean isOptimizedFilesFetch(@Nonnull ConnectorInfoDTO connectorDTO, String accountId) {
    return cdFeatureFlagHelper.isEnabled(accountId, OPTIMIZED_GIT_FETCH_FILES)
        && (isGithubTokenAuth((ScmConnector) connectorDTO.getConnectorConfig())
            || isGitlabTokenAuth((ScmConnector) connectorDTO.getConnectorConfig()));
  }

  public void addApiAuthIfRequired(ScmConnector scmConnector) {
    if (scmConnector instanceof GithubConnectorDTO && ((GithubConnectorDTO) scmConnector).getApiAccess() == null
        && isGithubUsernameTokenAuth((GithubConnectorDTO) scmConnector)) {
      GithubConnectorDTO githubConnectorDTO = (GithubConnectorDTO) scmConnector;
      SecretRefData tokenRef =
          ((GithubUsernameTokenDTO) ((GithubHttpCredentialsDTO) githubConnectorDTO.getAuthentication().getCredentials())
                  .getHttpCredentialsSpec())
              .getTokenRef();
      GithubApiAccessDTO apiAccessDTO = GithubApiAccessDTO.builder()
                                            .type(GithubApiAccessType.TOKEN)
                                            .spec(GithubTokenSpecDTO.builder().tokenRef(tokenRef).build())
                                            .build();
      githubConnectorDTO.setApiAccess(apiAccessDTO);
    } else if (scmConnector instanceof GitlabConnectorDTO && ((GitlabConnectorDTO) scmConnector).getApiAccess() == null
        && isGitlabUsernameTokenAuth((GitlabConnectorDTO) scmConnector)) {
      GitlabConnectorDTO gitlabConnectorDTO = (GitlabConnectorDTO) scmConnector;
      SecretRefData tokenRef =
          ((GitlabUsernameTokenDTO) ((GitlabHttpCredentialsDTO) gitlabConnectorDTO.getAuthentication().getCredentials())
                  .getHttpCredentialsSpec())
              .getTokenRef();
      GitlabApiAccessDTO apiAccessDTO = GitlabApiAccessDTO.builder()
                                            .type(GitlabApiAccessType.TOKEN)
                                            .spec(GitlabTokenSpecDTO.builder().tokenRef(tokenRef).build())
                                            .build();
      gitlabConnectorDTO.setApiAccess(apiAccessDTO);
    }
  }

  public String getGitRepoUrl(ScmConnector scmConnector, String repoName) {
    repoName = trimToEmpty(repoName);
    notEmptyCheck("Repo name cannot be empty for Account level git connector", repoName);
    String purgedRepoUrl = scmConnector.getUrl().replaceAll("/*$", "");
    String purgedRepoName = repoName.replaceAll("^/*", "");
    return purgedRepoUrl + "/" + purgedRepoName;
  }

  public void convertToRepoGitConfig(GitStoreConfig gitstoreConfig, ScmConnector scmConnector) {
    String repoName = gitstoreConfig.getRepoName() != null ? gitstoreConfig.getRepoName().getValue() : null;
    if (scmConnector instanceof GitConfigDTO) {
      GitConfigDTO gitConfigDTO = (GitConfigDTO) scmConnector;
      if (gitConfigDTO.getGitConnectionType() == GitConnectionType.ACCOUNT) {
        String repoUrl = getGitRepoUrl(gitConfigDTO, repoName);
        gitConfigDTO.setUrl(repoUrl);
        gitConfigDTO.setGitConnectionType(GitConnectionType.REPO);
      }
    } else if (scmConnector instanceof GithubConnectorDTO) {
      GithubConnectorDTO githubConnectorDTO = (GithubConnectorDTO) scmConnector;
      if (githubConnectorDTO.getConnectionType() == GitConnectionType.ACCOUNT) {
        String repoUrl = getGitRepoUrl(githubConnectorDTO, repoName);
        githubConnectorDTO.setUrl(repoUrl);
        githubConnectorDTO.setConnectionType(GitConnectionType.REPO);
      }
    } else if (scmConnector instanceof GitlabConnectorDTO) {
      GitlabConnectorDTO gitlabConnectorDTO = (GitlabConnectorDTO) scmConnector;
      if (gitlabConnectorDTO.getConnectionType() == GitConnectionType.ACCOUNT) {
        String repoUrl = getGitRepoUrl(gitlabConnectorDTO, repoName);
        gitlabConnectorDTO.setUrl(repoUrl);
        gitlabConnectorDTO.setConnectionType(GitConnectionType.REPO);
      }
    }
  }

  public GitStoreDelegateConfig getGitStoreDelegateConfig(@Nonnull GitStoreConfig gitstoreConfig,
      @Nonnull ConnectorInfoDTO connectorDTO, ManifestOutcome manifestOutcome, List<String> paths, Ambiance ambiance) {
    NGAccess basicNGAccessObject = AmbianceUtils.getNgAccess(ambiance);
    ScmConnector scmConnector;
    List<EncryptedDataDetail> apiAuthEncryptedDataDetails = null;
    GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO((ScmConnector) connectorDTO.getConnectorConfig());
    SSHKeySpecDTO sshKeySpecDTO = getSshKeySpecDTO(gitConfigDTO, ambiance);
    List<EncryptedDataDetail> encryptedDataDetails =
        gitConfigAuthenticationInfoHelper.getEncryptedDataDetails(gitConfigDTO, sshKeySpecDTO, basicNGAccessObject);

    scmConnector = gitConfigDTO;

    boolean optimizedFilesFetch = isOptimizedFilesFetch(connectorDTO, AmbianceUtils.getAccountId(ambiance))
        && !ManifestType.Kustomize.equals(manifestOutcome.getType());

    if (optimizedFilesFetch) {
      scmConnector = (ScmConnector) connectorDTO.getConnectorConfig();
      addApiAuthIfRequired(scmConnector);
      final DecryptableEntity apiAccessDecryptableEntity =
          GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmConnector);
      apiAuthEncryptedDataDetails =
          secretManagerClientService.getEncryptionDetails(basicNGAccessObject, apiAccessDecryptableEntity);
    }

    convertToRepoGitConfig(gitstoreConfig, scmConnector);
    return GitStoreDelegateConfig.builder()
        .gitConfigDTO(scmConnector)
        .sshKeySpecDTO(sshKeySpecDTO)
        .encryptedDataDetails(encryptedDataDetails)
        .apiAuthEncryptedDataDetails(apiAuthEncryptedDataDetails)
        .fetchType(gitstoreConfig.getGitFetchType())
        .branch(trim(getParameterFieldValue(gitstoreConfig.getBranch())))
        .commitId(trim(getParameterFieldValue(gitstoreConfig.getCommitId())))
        .paths(trimStrings(paths))
        .connectorName(connectorDTO.getName())
        .manifestType(manifestOutcome.getType())
        .manifestId(manifestOutcome.getIdentifier())
        .optimizedFilesFetch(optimizedFilesFetch)
        .build();
  }

  public List<EncryptedDataDetail> getEncryptedDataDetails(
      @Nonnull GitConfigDTO gitConfigDTO, @Nonnull Ambiance ambiance) {
    return secretManagerClientService.getEncryptionDetails(
        AmbianceUtils.getNgAccess(ambiance), gitConfigDTO.getGitAuth());
  }
}
