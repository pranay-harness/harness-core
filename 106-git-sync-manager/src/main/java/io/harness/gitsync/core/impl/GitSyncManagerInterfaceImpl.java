package io.harness.gitsync.core.impl;

import static io.harness.gitsync.common.ScopeHelper.getScope;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.GitFileLocationHelper;
import io.harness.gitsync.common.beans.GitFileChange;
import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.Status;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.dao.api.repositories.GitFileLocation.GitFileLocationRepository;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.ng.core.gitsync.ChangeType;
import io.harness.ng.core.gitsync.GitSyncManagerInterface;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Optional;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class GitSyncManagerInterfaceImpl implements GitSyncManagerInterface {
  private GitFileLocationRepository gitFileLocationRepository;
  private YamlGitConfigService yamlGitConfigService;
  private YamlChangeSetService yamlChangeSetService;

  @Override
  public String processHarnessToGit(ChangeType changeType, String yamlContent, String accountId, String orgId,
      String projectId, String entityName, String entityType, String entityIdentifier) {
    GitFileLocation gitFileLocation = getGitFileLocation(accountId, orgId, projectId, entityType, entityIdentifier);
    YamlGitConfigDTO yamlGitConfig = yamlGitConfigService.getByFolderIdentifierAndIsEnabled(
        projectId, orgId, accountId, gitFileLocation.getEntityRootFolderId());
    if (yamlGitConfig == null) {
      throw new InvalidRequestException("No git sync configured for given scope");
    }
    GitFileChange gitFileChange = buildGitFileChange(changeType, yamlContent, gitFileLocation, yamlGitConfig);
    YamlChangeSet yamlChangeSet =
        yamlChangeSetService.save(buildYamlChangeSet(accountId, orgId, projectId, gitFileChange, yamlGitConfig));
    return yamlChangeSet.getUuid();
  }

  private YamlChangeSet buildYamlChangeSet(
      String accountId, String orgId, String projectId, GitFileChange gitFileChange, YamlGitConfigDTO yamlGitConfig) {
    return YamlChangeSet.builder()
        .gitFileChanges(Collections.singletonList(gitFileChange))
        .gitToHarness(false)
        .organizationId(orgId)
        .projectId(projectId)
        .status(Status.QUEUED)
        .accountId(accountId)
        .scope(getScope(accountId, orgId, projectId))
        .fullSync(false)
        .build();
  }

  private GitFileChange buildGitFileChange(
      ChangeType changeType, String yamlContent, GitFileLocation gitFileLocation, YamlGitConfigDTO yamlGitConfig) {
    return GitFileChange.builder()
        .yamlGitConfig(yamlGitConfig)
        .fileContent(yamlContent)
        .filePath(gitFileLocation.getEntityGitPath())
        .oldFilePath(gitFileLocation.getEntityGitPath())
        .syncFromGit(false)
        .accountId(gitFileLocation.getAccountId())
        .rootPath(gitFileLocation.getEntityRootFolderName())
        .rootPathId(gitFileLocation.getEntityRootFolderId())
        .changeType(changeType)
        .rootPath(gitFileLocation.getEntityRootFolderName())
        .build();
  }

  private GitFileLocation getGitFileLocation(
      String accountId, String orgId, String projectId, String entityType, String entityIdentifier) {
    Optional<GitFileLocation> gitFileLocation =
        gitFileLocationRepository.findByProjectIdAndOrganizationIdAndAccountIdAndEntityTypeAndEntityIdentifier(
            projectId, orgId, accountId, entityType, entityIdentifier);
    return gitFileLocation.orElseGet(
        () -> buildGitFileLocation(accountId, orgId, projectId, entityType, entityIdentifier));
  }

  private GitFileLocation buildGitFileLocation(
      String accountId, String orgId, String projectId, String entityType, String entityIdentifier) {
    Optional<YamlGitConfigDTO.RootFolder> defaultRootFolder =
        yamlGitConfigService.getDefault(projectId, orgId, accountId);
    return defaultRootFolder
        .map(rootFolder -> {
          return gitFileLocationRepository.save(GitFileLocation.builder()
                                                    .accountId(accountId)
                                                    .entityIdentifier(entityIdentifier)
                                                    .entityType(entityType)
                                                    .entityIdentifier(entityIdentifier)
                                                    .entityType(entityType)
                                                    .organizationId(orgId)
                                                    .projectId(projectId)
                                                    .yamlGitFolderConfigId(rootFolder.getIdentifier())
                                                    .entityRootFolderName(rootFolder.getRootFolder())
                                                    .entityRootFolderId(rootFolder.getIdentifier())
                                                    .entityGitPath(GitFileLocationHelper.getEntityPath(
                                                        rootFolder.getRootFolder(), entityType, entityIdentifier))
                                                    .build());
        })
        .orElseThrow(() -> new InvalidRequestException("No git sync configured for given scope"));
  }
}
