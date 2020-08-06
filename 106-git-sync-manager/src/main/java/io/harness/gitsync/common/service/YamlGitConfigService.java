package io.harness.gitsync.common.service;

import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.validation.Create;
import io.harness.validation.Update;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;

public interface YamlGitConfigService {
  Optional<GitConfigDTO> getGitConfig(YamlGitConfigDTO ygs, String gitConnectorId, String repoName, String branchName);

  YamlGitConfigDTO getByIdentifier(String projectIdentifier, String orgIdentifier, String accountId, String identifier);

  Optional<YamlGitConfigDTO.RootFolder> getDefault(String projectIdentifier, String orgIdentifier, String accountId);

  List<YamlGitConfigDTO> get(String projectId, String orgId, String accountId);

  YamlGitConfigDTO getByFolderIdentifierAndIsEnabled(
      String projectIdentifier, String orgIdentifier, String accountId, String folderId);

  List<YamlGitConfigDTO> orderedGet(String projectIdentifier, String orgIdentifier, String accountId);

  List<YamlGitConfigDTO> updateDefault(
      String projectIdentifier, String orgId, String accountId, String Id, String folderId);

  @ValidationGroups(Create.class) YamlGitConfigDTO save(@Valid YamlGitConfigDTO yamlGitConfig);

  @ValidationGroups(Update.class) YamlGitConfigDTO update(@Valid YamlGitConfigDTO yamlGitConfig);

  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
}
