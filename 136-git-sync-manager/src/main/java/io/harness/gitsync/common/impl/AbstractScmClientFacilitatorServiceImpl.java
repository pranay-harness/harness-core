/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.IdentifierRef;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFileDetails.GitFileDetailsBuilder;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.impl.ConnectorErrorMessagesHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.gitsync.UserPrincipal;
import io.harness.gitsync.common.dtos.GitFileContent;
import io.harness.gitsync.common.helper.UserProfileHelper;
import io.harness.gitsync.common.service.ScmClientFacilitatorService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.helpers.ScmUserHelper;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.gitsync.scm.ScmGitUtils;
import io.harness.impl.ScmResponseStatusUtils;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED)
@OwnedBy(HarnessTeam.DX)
public abstract class AbstractScmClientFacilitatorServiceImpl implements ScmClientFacilitatorService {
  private ConnectorService connectorService;
  private ConnectorErrorMessagesHelper connectorErrorMessagesHelper;
  private YamlGitConfigService yamlGitConfigService;
  private UserProfileHelper userProfileHelper;

  @Inject
  protected AbstractScmClientFacilitatorServiceImpl(ConnectorService connectorService,
      ConnectorErrorMessagesHelper connectorErrorMessagesHelper, YamlGitConfigService yamlGitConfigService,
      UserProfileHelper userProfileHelper) {
    this.connectorService = connectorService;
    this.connectorErrorMessagesHelper = connectorErrorMessagesHelper;
    this.yamlGitConfigService = yamlGitConfigService;
    this.userProfileHelper = userProfileHelper;
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

  ScmConnector getScmConnector(IdentifierRef connectorIdentifierRef) {
    final ConnectorResponseDTO connectorResponseDTO =
        connectorService
            .get(connectorIdentifierRef.getAccountIdentifier(), connectorIdentifierRef.getOrgIdentifier(),
                connectorIdentifierRef.getProjectIdentifier(), connectorIdentifierRef.getIdentifier())
            .orElseThrow(
                ()
                    -> new InvalidRequestException(connectorErrorMessagesHelper.createConnectorNotFoundMessage(
                        connectorIdentifierRef.getAccountIdentifier(), connectorIdentifierRef.getOrgIdentifier(),
                        connectorIdentifierRef.getProjectIdentifier(), connectorIdentifierRef.getIdentifier())));
    return (ScmConnector) connectorResponseDTO.getConnector().getConnectorConfig();
  }

  YamlGitConfigDTO getYamlGitConfigDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String yamlGitConfigIdentifier) {
    return yamlGitConfigService.get(projectIdentifier, orgIdentifier, accountIdentifier, yamlGitConfigIdentifier);
  }

  IdentifierRef getConnectorIdentifierRef(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifierRef) {
    return IdentifierRefHelper.getIdentifierRef(
        connectorIdentifierRef, accountIdentifier, orgIdentifier, projectIdentifier);
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

  ConnectorResponseDTO getConnectorResponseDTO(YamlGitConfigDTO gitSyncConfigDTO, String accountId) {
    final String connectorRef = gitSyncConfigDTO.getGitConnectorRef();
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        connectorRef, accountId, gitSyncConfigDTO.getOrganizationIdentifier(), gitSyncConfigDTO.getProjectIdentifier());
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(accountId, identifierRef.getOrgIdentifier(),
        identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    return connectorDTO.orElseThrow(
        ()
            -> new UnexpectedException(
                String.format("No connector found with the id %s, accountId %s, orgId %s, projectId %s",
                    gitSyncConfigDTO.getIdentifier(), accountId, gitSyncConfigDTO.getOrganizationIdentifier(),
                    gitSyncConfigDTO.getProjectIdentifier())));
  }

  void checkAndSetUserFromUserProfile(
      boolean useUserFromToken, YamlGitConfigDTO yamlGitConfigDTO, ConnectorResponseDTO connectorResponseDTO) {
    if (useUserFromToken) {
      UserPrincipal userPrincipal = userProfileHelper.getUserPrincipal();
      userProfileHelper.setConnectorDetailsFromUserProfile(yamlGitConfigDTO, userPrincipal, connectorResponseDTO);
    }
  }

  GitFileDetailsBuilder getGitFileDetails(
      String yaml, String filePath, String folderPath, String commitMsg, String branch) {
    final EmbeddedUser currentUser = ScmUserHelper.getCurrentUser();
    String filePathForPush = ScmGitUtils.createFilePath(folderPath, filePath);
    return GitFileDetails.builder()
        .branch(branch)
        .commitMessage(isEmpty(commitMsg) ? GitSyncConstants.COMMIT_MSG : commitMsg)
        .fileContent(yaml)
        .filePath(filePathForPush)
        .userEmail(currentUser.getEmail())
        .userName(currentUser.getName());
  }
}
