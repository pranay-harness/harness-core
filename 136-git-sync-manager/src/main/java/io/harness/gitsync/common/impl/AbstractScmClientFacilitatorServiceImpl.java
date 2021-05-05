package io.harness.gitsync.common.impl;

import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.connector.ConnectorResponseDTO;
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
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PROTECTED)
@OwnedBy(HarnessTeam.DX)
public abstract class AbstractScmClientFacilitatorServiceImpl implements ScmClientFacilitatorService {
  private final ConnectorService connectorService;
  private final ConnectorErrorMessagesHelper connectorErrorMessagesHelper;
  private final YamlGitConfigService yamlGitConfigService;

  @Inject
  protected AbstractScmClientFacilitatorServiceImpl(ConnectorService connectorService,
      ConnectorErrorMessagesHelper connectorErrorMessagesHelper, YamlGitConfigService yamlGitConfigService) {
    this.connectorService = connectorService;
    this.connectorErrorMessagesHelper = connectorErrorMessagesHelper;
    this.yamlGitConfigService = yamlGitConfigService;
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

  ScmConnector getScmConnector(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifierRef) {
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        connectorIdentifierRef, accountIdentifier, orgIdentifier, projectIdentifier);
    final ConnectorResponseDTO connectorResponseDTO =
        connectorService
            .get(identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
                identifierRef.getProjectIdentifier(), identifierRef.getIdentifier())
            .orElseThrow(()
                             -> new InvalidRequestException(connectorErrorMessagesHelper.createConnectorNotFoundMessage(
                                 identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
                                 identifierRef.getProjectIdentifier(), identifierRef.getIdentifier())));
    return (ScmConnector) connectorResponseDTO.getConnector();
  }

  IdentifierRef getYamlGitConfigIdentifierRef(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String yamlGitConfigIdentifier) {
    YamlGitConfigDTO yamlGitConfig =
        yamlGitConfigService.get(projectIdentifier, orgIdentifier, accountIdentifier, yamlGitConfigIdentifier);
    return IdentifierRefHelper.getIdentifierRef(yamlGitConfig.getGitConnectorRef(), accountIdentifier,
        yamlGitConfig.getOrganizationIdentifier(), yamlGitConfig.getProjectIdentifier());
  }

  void validateFileContentParams(String branch, String commitId) {
    if (commitId != null && branch != null) {
      throw new InvalidRequestException("Only one of branch or commit id can be present.", USER);
    }
    if (commitId == null && branch == null) {
      throw new InvalidRequestException("One of branch or commit id should be present.", USER);
    }
  }

  GitFilePathDetails getGitFilePathDetails(String filePath, String branch, String commitId) {
    return GitFilePathDetails.builder().filePath(filePath).branch(branch).ref(commitId).build();
  }

  GitFileContent validateAndGetGitFileContent(FileContent fileContent) {
    ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(fileContent.getStatus());
    return GitFileContent.builder().content(fileContent.getContent()).objectId(fileContent.getBlobId()).build();
  }
}
