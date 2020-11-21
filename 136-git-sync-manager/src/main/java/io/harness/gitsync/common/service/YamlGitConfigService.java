package io.harness.gitsync.common.service;

import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.validation.Create;
import io.harness.validation.Update;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

public interface YamlGitConfigService {
  Optional<ConnectorInfoDTO> getGitConnector(
      YamlGitConfigDTO ygs, String gitConnectorId, String repoName, String branchName);

  YamlGitConfigDTO getByFolderIdentifier(
      String projectIdentifier, String orgIdentifier, String accountId, String identifier);

  Optional<YamlGitConfigDTO.RootFolder> getDefault(String projectIdentifier, String orgIdentifier, String accountId);

  YamlGitConfigDTO getByYamlGitConfigIdAndBranchAndRepoAndConnectorId(
      String uuid, String branch, String repo, String connectorId, String accountId);

  List<YamlGitConfigDTO> getByConnectorRepoAndBranch(
      String gitConnectorId, String repo, String branchName, String accountId);

  List<YamlGitConfigDTO> get(String projectId, String orgId, String accountId);

  YamlGitConfigDTO getByFolderIdentifierAndIsEnabled(
      String projectIdentifier, String orgIdentifier, String accountId, String folderId);

  List<YamlGitConfigDTO> orderedGet(String projectIdentifier, String orgIdentifier, String accountId);

  List<YamlGitConfigDTO> updateDefault(
      String projectIdentifier, String orgId, String accountId, String Id, String folderId);

  @ValidationGroups(Create.class) YamlGitConfigDTO save(@Valid YamlGitConfigDTO yamlGitConfig);

  @ValidationGroups(Update.class) YamlGitConfigDTO update(@Valid YamlGitConfigDTO yamlGitConfig);

  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier);

  YamlGitConfigDTO getByIdentifier(String projectId, String organizationId, String accountId, String identifier);
}
