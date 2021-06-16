package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.common.beans.BranchSyncMetadata;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;
import io.harness.gitsync.common.dtos.GitFileChangeDTO;
import io.harness.gitsync.common.helper.YamlGitConfigHelper;
import io.harness.gitsync.common.service.GitBranchSyncService;
import io.harness.gitsync.common.service.GitToHarnessProgressService;
import io.harness.gitsync.common.service.ScmOrchestratorService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;
import io.harness.gitsync.core.dtos.YamlChangeSetSaveDTO;
import io.harness.gitsync.core.service.YamlChangeSetService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class GitBranchSyncServiceImpl implements GitBranchSyncService {
  GitToHarnessProcessorService gitToHarnessProcessorService;
  ScmOrchestratorService scmOrchestratorService;
  GitToHarnessProgressService gitToHarnessProgressService;
  YamlGitConfigService yamlGitConfigService;
  YamlChangeSetService yamlChangeSetService;

  @Override
  public void createBranchSyncEvent(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String yamlGitConfigIdentifier, String repoURL, String branch, String filePathToBeExcluded) {
    final BranchSyncMetadata branchSyncMetadata = BranchSyncMetadata.builder()
                                                      .fileToBeExcluded(filePathToBeExcluded)
                                                      .orgIdentifier(orgIdentifier)
                                                      .projectIdentifier(projectIdentifier)
                                                      .yamlGitConfigId(yamlGitConfigIdentifier)
                                                      .build();
    final YamlChangeSetSaveDTO yamlChangeSetSaveDTO = YamlChangeSetSaveDTO.builder()
                                                          .accountId(accountIdentifier)
                                                          .branch(branch)
                                                          .repoUrl(repoURL)
                                                          .eventType(YamlChangeSetEventType.BRANCH_SYNC)
                                                          .eventMetadata(branchSyncMetadata)
                                                          .build();
    final YamlChangeSetDTO savedChangeSet = yamlChangeSetService.save(yamlChangeSetSaveDTO);
    log.info("Created the change set {} to process the branch {} in the repo {}", savedChangeSet.getChangesetId(),
        branch, repoURL);
  }

  @Override
  public void processBranchSyncEvent(YamlGitConfigDTO yamlGitConfig, String branchName, String accountIdentifier,
      String filePathToBeExcluded, String changeSetId, String gitToHarnessProgressRecordId) {
    List<YamlGitConfigDTO> yamlGitConfigDTOS = yamlGitConfigService.getByRepo(yamlGitConfig.getRepo());
    Set<String> foldersList = YamlGitConfigHelper.getRootFolderList(yamlGitConfigDTOS);
    List<GitFileChangeDTO> harnessFilesOfBranch =
        getFilesBelongingToThisBranch(accountIdentifier, yamlGitConfig, foldersList, branchName);
    log.info("Received file paths: [{}] from git in harness folders.",
        emptyIfNull(harnessFilesOfBranch).stream().map(GitFileChangeDTO::getPath).collect(Collectors.toList()));
    List<GitFileChangeDTO> filteredFileList = getFilteredFiles(harnessFilesOfBranch, filePathToBeExcluded);
    List<GitToHarnessFileProcessingRequest> gitToHarnessFilesToProcess =
        emptyIfNull(filteredFileList)
            .stream()
            .map(fileContent
                -> GitToHarnessFileProcessingRequest.builder()
                       .fileDetails(fileContent)
                       .changeType(ChangeType.ADD)
                       .build())
            .collect(toList());
    gitToHarnessProgressService.updateFilesInProgressRecord(gitToHarnessProgressRecordId, gitToHarnessFilesToProcess);
    String commitId = getCommitId(harnessFilesOfBranch);
    gitToHarnessProcessorService.processFiles(accountIdentifier, gitToHarnessFilesToProcess, branchName, yamlGitConfig,
        commitId, gitToHarnessProgressRecordId);
  }

  private String getCommitId(List<GitFileChangeDTO> harnessFilesOfBranch) {
    if (isEmpty(harnessFilesOfBranch)) {
      return null;
    }
    return harnessFilesOfBranch.get(0).getCommitId();
  }

  private List<GitFileChangeDTO> getFilesBelongingToThisBranch(
      String accountIdentifier, YamlGitConfigDTO yamlGitConfig, Set<String> foldersList, String branchName) {
    return scmOrchestratorService.processScmRequest(scmClientFacilitatorService
        -> scmClientFacilitatorService.listFilesOfBranches(accountIdentifier, yamlGitConfig.getOrganizationIdentifier(),
            yamlGitConfig.getProjectIdentifier(), yamlGitConfig.getIdentifier(), foldersList, branchName),
        yamlGitConfig.getProjectIdentifier(), yamlGitConfig.getOrganizationIdentifier(), accountIdentifier);
  }

  private List<GitFileChangeDTO> getFilteredFiles(List<GitFileChangeDTO> fileContents, String filePathToBeExcluded) {
    List<GitFileChangeDTO> filteredFileContents = new ArrayList<>();
    for (GitFileChangeDTO fileContent : fileContents) {
      if (fileContent.getPath().equals(filePathToBeExcluded)) {
        continue;
      }
      filteredFileContents.add(fileContent);
    }
    return filteredFileContents;
  }
}
