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
import io.harness.gitsync.common.dtos.RepoProviders;
import io.harness.gitsync.common.dtos.SaasGitDTO;
import io.harness.gitsync.common.service.ScmClientFacilitatorService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.impl.ScmResponseStatusUtils;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import java.net.URL;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED)
@OwnedBy(HarnessTeam.DX)
public abstract class AbstractScmClientFacilitatorServiceImpl implements ScmClientFacilitatorService {
  private ConnectorService connectorService;
  private ConnectorErrorMessagesHelper connectorErrorMessagesHelper;
  private YamlGitConfigService yamlGitConfigService;

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

  @Override
  public SaasGitDTO isSaasGit(ScmConnector scmConnector) {
    try {
      URL url = new URL(
          (scmConnector.getUrl().startsWith("http")) ? (scmConnector.getUrl()) : ("http://" + scmConnector.getUrl()));
      String host = (url.getHost().startsWith("www.")) ? (url.getHost()) : ("www." + url.getHost());
      if (null != url.getHost()) {
        for (RepoProviders repoProvider : RepoProviders.values()) {
          if (StringUtils.containsIgnoreCase(host, repoProvider.name())) {
            return SaasGitDTO.builder()
                .isSaasGit(host.contains("www." + repoProvider.name().toLowerCase() + ".com"))
                .build();
          }
        }
      }
    } catch (Exception e) {
      log.error("Failed to generate Git Provider Repository Url {}", scmConnector.getUrl(), e);
    }
    return SaasGitDTO.builder().isSaasGit(false).build();
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
    return (ScmConnector) connectorResponseDTO.getConnector().getConnectorConfig();
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
    ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(fileContent.getStatus(), fileContent.getError());
    return GitFileContent.builder().content(fileContent.getContent()).objectId(fileContent.getBlobId()).build();
  }
}
