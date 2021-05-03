package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.connector.impl.ConnectorErrorMessagesHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.dtos.GitFileContent;
import io.harness.gitsync.common.service.ScmClientFacilitatorService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.impl.ScmResponseStatusUtils;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.service.ScmClient;
import io.harness.tasks.DecryptGitApiAccessHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(DX)
public class ScmClientFacilitatorServiceImpl implements ScmClientFacilitatorService {
  private final DecryptGitApiAccessHelper decryptGitApiAccessHelper;
  private final ScmClient scmClient;
  private final ConnectorService connectorService;
  private final ConnectorErrorMessagesHelper connectorErrorMessagesHelper;
  private final YamlGitConfigService yamlGitConfigService;

  @Inject
  public ScmClientFacilitatorServiceImpl(DecryptGitApiAccessHelper decryptGitApiAccessHelper, ScmClient scmClient,
      @Named("connectorDecoratorService") ConnectorService connectorService,
      ConnectorErrorMessagesHelper connectorErrorMessagesHelper, YamlGitConfigService yamlGitConfigService) {
    this.decryptGitApiAccessHelper = decryptGitApiAccessHelper;
    this.scmClient = scmClient;
    this.connectorService = connectorService;
    this.connectorErrorMessagesHelper = connectorErrorMessagesHelper;
    this.yamlGitConfigService = yamlGitConfigService;
  }

  @Override
  public List<String> listBranchesForRepoByConnector(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifierRef, String repoURL,
      io.harness.ng.beans.PageRequest pageRequest, String searchTerm) {
    ScmConnector scmConnector =
        getDecryptedScmConnector(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifierRef);
    scmConnector.setUrl(repoURL);
    ListBranchesResponse listBranchesResponse = scmClient.listBranches(scmConnector);
    return listBranchesResponse.getBranchesList();
  }

  private ScmConnector getDecryptedScmConnector(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifierRef) {
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        connectorIdentifierRef, accountIdentifier, orgIdentifier, projectIdentifier);
    return connectorService
        .get(identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
            identifierRef.getProjectIdentifier(), identifierRef.getIdentifier())
        .map(connectorResponseDTO
            -> decryptGitApiAccessHelper.decryptScmApiAccess(
                (ScmConnector) connectorResponseDTO.getConnector().getConnectorConfig(),
                identifierRef.getAccountIdentifier(), identifierRef.getProjectIdentifier(),
                identifierRef.getOrgIdentifier()))
        .orElseThrow(()
                         -> new InvalidRequestException(connectorErrorMessagesHelper.createConnectorNotFoundMessage(
                             identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
                             identifierRef.getProjectIdentifier(), identifierRef.getIdentifier())));
  }

  @Override
  public List<String> listBranchesForRepoByGitSyncConfig(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String yamlGitConfigIdentifier, io.harness.ng.beans.PageRequest pageRequest,
      String searchTerm) {
    YamlGitConfigDTO yamlGitConfig =
        yamlGitConfigService.get(projectIdentifier, orgIdentifier, accountIdentifier, yamlGitConfigIdentifier);
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(yamlGitConfig.getGitConnectorRef(),
        accountIdentifier, yamlGitConfig.getOrganizationIdentifier(), yamlGitConfig.getProjectIdentifier());
    return listBranchesForRepoByConnector(identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
        identifierRef.getProjectIdentifier(), identifierRef.getIdentifier(), yamlGitConfig.getRepo(), pageRequest,
        searchTerm);
  }

  @Override
  public GitFileContent getFileContent(String yamlGitConfigIdentifier, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String filePath, String branch, String commitId) {
    if (commitId != null && branch != null) {
      throw new InvalidRequestException("Only one of branch or commit id can be present.", USER);
    }
    if (commitId == null && branch == null) {
      throw new InvalidRequestException("One of branch or commit id should be present.", USER);
    }
    YamlGitConfigDTO yamlGitConfig =
        yamlGitConfigService.get(projectIdentifier, orgIdentifier, accountIdentifier, yamlGitConfigIdentifier);
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(yamlGitConfig.getGitConnectorRef(),
        accountIdentifier, yamlGitConfig.getOrganizationIdentifier(), yamlGitConfig.getProjectIdentifier());
    final ScmConnector decryptedScmConnector = getDecryptedScmConnector(identifierRef.getAccountIdentifier(),
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    final GitFilePathDetails gitFilePathDetails =
        GitFilePathDetails.builder().filePath(filePath).branch(branch).ref(commitId).build();
    final FileContent latestFile = scmClient.getFileContent(decryptedScmConnector, gitFilePathDetails);
    ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(latestFile.getStatus());
    return GitFileContent.builder().content(latestFile.getContent()).objectId(latestFile.getBlobId()).build();
  }
}
