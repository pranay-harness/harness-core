package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.connector.impl.ConnectorErrorMessagesHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.beans.YamlGitConfig;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.repositories.repositories.yamlGitConfig.YamlGitConfigRepository;
import io.harness.service.ScmClient;
import io.harness.tasks.DecryptGitApiAccessHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class GitBranchServiceImpl implements GitBranchService {
  private final DecryptGitApiAccessHelper decryptGitApiAccessHelper;
  private final ScmClient scmClient;
  private final ConnectorService connectorService;
  private final YamlGitConfigRepository yamlGitConfigRepository;
  private final ConnectorErrorMessagesHelper connectorErrorMessagesHelper;

  @Inject
  public GitBranchServiceImpl(DecryptGitApiAccessHelper decryptGitApiAccessHelper, ScmClient scmClient,
      @Named("connectorDecoratorService") ConnectorService connectorService,
      YamlGitConfigRepository yamlGitConfigRepository, ConnectorErrorMessagesHelper connectorErrorMessagesHelper) {
    this.decryptGitApiAccessHelper = decryptGitApiAccessHelper;
    this.scmClient = scmClient;
    this.connectorService = connectorService;
    this.yamlGitConfigRepository = yamlGitConfigRepository;
    this.connectorErrorMessagesHelper = connectorErrorMessagesHelper;
  }

  @Override
  public List<String> listBranchesForRepoByConnector(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifier, String repoURL) {
    ScmConnector scmConnector =
        connectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier)
            .map(connectorResponseDTO
                -> decryptGitApiAccessHelper.decryptScmApiAccess(
                    (ScmConnector) connectorResponseDTO.getConnector().getConnectorConfig(), accountIdentifier,
                    projectIdentifier, orgIdentifier))
            .orElseThrow(()
                             -> new InvalidRequestException(connectorErrorMessagesHelper.createConnectorNotFoundMessage(
                                 accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier)));
    scmConnector.setUrl(repoURL);
    ListBranchesResponse listBranchesResponse = scmClient.listBranches(scmConnector);
    return listBranchesResponse.getBranchesList();
  }

  @Override
  public List<String> listBranchesForRepoByGitSyncConfig(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String yamlGitConfigIdentifier) {
    YamlGitConfig yamlGitConfig =
        yamlGitConfigRepository
            .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
                accountIdentifier, orgIdentifier, projectIdentifier, yamlGitConfigIdentifier)
            .orElseThrow(()
                             -> new InvalidRequestException(
                                 "No Git Config exists with the identifier" + yamlGitConfigIdentifier));
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(yamlGitConfig.getGitConnectorRef(),
        yamlGitConfig.getAccountId(), yamlGitConfig.getOrgIdentifier(), yamlGitConfig.getProjectIdentifier());
    return listBranchesForRepoByConnector(identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
        identifierRef.getProjectIdentifier(), identifierRef.getIdentifier(), yamlGitConfig.getRepo());
  }
}
