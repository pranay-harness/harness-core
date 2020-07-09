package io.harness.gitsync.common.service;

import io.harness.gitsync.common.dtos.YamlGitConfigDTO;
import io.harness.validation.Create;
import io.harness.validation.Update;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

import java.util.List;
import javax.validation.Valid;

public interface YamlGitConfigService {
  YamlGitConfigDTO save(YamlGitConfigDTO gsc, boolean performFullSync);

  List<YamlGitConfigDTO> get(String projectId, String orgId, String accountId);

  YamlGitConfigDTO getByIdentifier(String projectId, String orgId, String accountId, String identifier);

  List<YamlGitConfigDTO> updateDefault(
      String projectIdentifier, String orgId, String accountId, String Id, String folderId);

  @ValidationGroups(Create.class) YamlGitConfigDTO save(@Valid YamlGitConfigDTO yamlGitConfig);

  @ValidationGroups(Update.class) YamlGitConfigDTO update(@Valid YamlGitConfigDTO yamlGitConfig);

  boolean delete(String accountId, String orgId, String projectIdentifier, String identifier);
}
