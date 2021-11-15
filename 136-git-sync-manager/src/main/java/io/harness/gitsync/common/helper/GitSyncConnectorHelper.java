package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.impl.ConnectorErrorMessagesHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccess;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccess;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccess;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.tasks.DecryptGitApiAccessHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Singleton
@OwnedBy(DX)
@Slf4j
public class GitSyncConnectorHelper {
  ConnectorService connectorService;
  DecryptGitApiAccessHelper decryptGitApiAccessHelper;
  YamlGitConfigService yamlGitConfigService;
  ConnectorErrorMessagesHelper connectorErrorMessagesHelper;

  @Inject
  public GitSyncConnectorHelper(@Named("connectorDecoratorService") ConnectorService connectorService,
      DecryptGitApiAccessHelper decryptGitApiAccessHelper, YamlGitConfigService yamlGitConfigService) {
    this.connectorService = connectorService;
    this.decryptGitApiAccessHelper = decryptGitApiAccessHelper;
    this.yamlGitConfigService = yamlGitConfigService;
  }

  public ScmConnector getDecryptedConnector(
      String yamlGitConfigIdentifier, String projectIdentifier, String orgIdentifier, String accountId) {
    final YamlGitConfigDTO yamlGitConfigDTO =
        yamlGitConfigService.get(projectIdentifier, orgIdentifier, accountId, yamlGitConfigIdentifier);
    if (yamlGitConfigDTO == null) {
      throw new InvalidRequestException(String.format(
          "Git sync configuration not found for identifier: [%s], projectIdentifier: [%s], orgIdentifier: [%s]",
          projectIdentifier, orgIdentifier, yamlGitConfigIdentifier));
    }
    return getDecryptedConnector(yamlGitConfigDTO, accountId);
  }

  public ScmConnector getDecryptedConnector(YamlGitConfigDTO gitSyncConfigDTO, String accountId) {
    final String connectorRef = gitSyncConfigDTO.getGitConnectorRef();
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        connectorRef, accountId, gitSyncConfigDTO.getOrganizationIdentifier(), gitSyncConfigDTO.getProjectIdentifier());
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(accountId, identifierRef.getOrgIdentifier(),
        identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    if (connectorDTO.isPresent()) {
      ConnectorInfoDTO connector = connectorDTO.get().getConnector();
      ConnectorConfigDTO connectorConfig = connector.getConnectorConfig();
      if (connectorConfig instanceof ScmConnector) {
        ScmConnector gitConnectorConfig = (ScmConnector) connector.getConnectorConfig();
        final ScmConnector scmConnector = getDecryptedConnector(
            accountId, connector.getOrgIdentifier(), connector.getProjectIdentifier(), gitConnectorConfig);
        scmConnector.setUrl(gitSyncConfigDTO.getRepo());
        return scmConnector;
      } else {
        throw new UnexpectedException(
            String.format("The connector with the  id %s, accountId %s, orgId %s, projectId %s is not a scm connector",
                gitSyncConfigDTO.getIdentifier(), accountId, gitSyncConfigDTO.getOrganizationIdentifier(),
                gitSyncConfigDTO.getProjectIdentifier()));
      }
    } else {
      throw new UnexpectedException(String.format(
          "No connector found with the id %s, accountId %s, orgId %s, projectId %s", gitSyncConfigDTO.getIdentifier(),
          accountId, gitSyncConfigDTO.getOrganizationIdentifier(), gitSyncConfigDTO.getProjectIdentifier()));
    }
  }

  public ScmConnector getDecryptedConnector(
      String accountId, String orgIdentifier, String projectIdentifier, ScmConnector connectorDTO) {
    return decryptGitApiAccessHelper.decryptScmApiAccess(connectorDTO, accountId, projectIdentifier, orgIdentifier);
  }

  public ScmConnector getDecryptedConnector(
      YamlGitConfigDTO gitSyncConfigDTO, String accountId, ConnectorResponseDTO connectorDTO) {
    ConnectorInfoDTO connector = connectorDTO.getConnector();
    ConnectorConfigDTO connectorConfig = connector.getConnectorConfig();
    if (connectorConfig instanceof ScmConnector) {
      ScmConnector gitConnectorConfig = (ScmConnector) connector.getConnectorConfig();
      final ScmConnector scmConnector = decryptGitApiAccessHelper.decryptScmApiAccess(gitConnectorConfig, accountId,
          gitSyncConfigDTO.getProjectIdentifier(), gitSyncConfigDTO.getOrganizationIdentifier());
      scmConnector.setUrl(gitSyncConfigDTO.getRepo());
      return scmConnector;
    } else {
      throw new UnexpectedException(
          String.format("The connector with the  id %s, accountId %s, orgId %s, projectId %s is not a scm connector",
              gitSyncConfigDTO.getIdentifier(), accountId, gitSyncConfigDTO.getOrganizationIdentifier(),
              gitSyncConfigDTO.getProjectIdentifier()));
    }
  }

  public void validateTheAPIAccessPresence(ScmConnector scmConnector) {
    if (scmConnector instanceof GithubConnector) {
      checkAPIAccessFieldPresence((GithubConnector) scmConnector);
    } else if (scmConnector instanceof GitlabConnector) {
      checkAPIAccessFieldPresence((GitlabConnector) scmConnector);
    } else if (scmConnector instanceof BitbucketConnector) {
      checkAPIAccessFieldPresence((BitbucketConnector) scmConnector);
    } else {
      throw new NotImplementedException(
          String.format("The scm apis for the provider type %s is not supported", scmConnector.getClass()));
    }
  }

  private void checkAPIAccessFieldPresence(GithubConnector githubConnector) {
    GithubApiAccess apiAccess = githubConnector.getApiAccess();
    if (apiAccess == null) {
      throw new InvalidRequestException(
          "The connector doesn't contain api access field which is required for the git sync ");
    }
  }

  private void checkAPIAccessFieldPresence(GitlabConnector gitlabConnector) {
    GitlabApiAccess apiAccess = gitlabConnector.getApiAccess();
    if (apiAccess == null) {
      throw new InvalidRequestException(
          "The connector doesn't contain api access field which is required for the git sync ");
    }
  }

  private void checkAPIAccessFieldPresence(BitbucketConnector bitbucketConnector) {
    BitbucketApiAccess apiAccess = bitbucketConnector.getApiAccess();
    if (apiAccess == null) {
      throw new InvalidRequestException(
          "The connector doesn't contain api access field which is required for the git sync ");
    }
  }

  public ScmConnector getScmConnector(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorRef) {
    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef(connectorRef, accountIdentifier, orgIdentifier, projectIdentifier);
    final ConnectorResponseDTO connectorResponseDTO =
        connectorService
            .get(identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
                identifierRef.getProjectIdentifier(), identifierRef.getIdentifier())
            .orElseThrow(()
                             -> new InvalidRequestException(connectorErrorMessagesHelper.createConnectorNotFoundMessage(
                                 identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
                                 identifierRef.getProjectIdentifier(), identifierRef.getIdentifier())));
    return (ScmConnector) connectorResponseDTO.getConnector().getConnectorConfig();
  }
}
