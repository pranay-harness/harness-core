package io.harness.gitsync.common.service;

import io.harness.connector.ConnectorInfoDTO;
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

  List<YamlGitConfigDTO> getByConnectorRepoAndBranch(
      String gitConnectorId, String repo, String branchName, String accountId);

  YamlGitConfigDTO get(String projectId, String orgId, String accountId, String identifier);

  YamlGitConfigDTO getByFolderIdentifierAndIsEnabled(
      String projectIdentifier, String orgIdentifier, String accountId, String folderId);

  List<YamlGitConfigDTO> list(String projectIdentifier, String orgIdentifier, String accountId);

  YamlGitConfigDTO updateDefault(String projectIdentifier, String orgId, String accountId, String Id, String folderId);

  @ValidationGroups(Create.class) YamlGitConfigDTO save(@Valid YamlGitConfigDTO yamlGitConfig);

  @ValidationGroups(Update.class) YamlGitConfigDTO update(@Valid YamlGitConfigDTO yamlGitConfig);

  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier);

  YamlGitConfigDTO get(String uuid, String accountId);
  // todo(abhinav): change get function to provide accountId, orgId and projectId along with identifier

  Boolean isGitSyncEnabled(String accountIdentifier, String organizationIdentifier, String projectIdentifier);
}
