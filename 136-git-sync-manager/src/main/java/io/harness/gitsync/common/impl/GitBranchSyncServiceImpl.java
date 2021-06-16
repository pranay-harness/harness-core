package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.TO_DO;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepType;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;
import io.harness.gitsync.common.dtos.GitFileChangeDTO;
import io.harness.gitsync.common.dtos.GitToHarnessProgressDTO;
import io.harness.gitsync.common.helper.YamlGitConfigHelper;
import io.harness.gitsync.common.service.GitBranchSyncService;
import io.harness.gitsync.common.service.GitToHarnessProgressService;
import io.harness.gitsync.common.service.ScmOrchestratorService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;

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

  @Override
  public void syncBranch(YamlGitConfigDTO yamlGitConfig, String branchName, String accountId,
      String filePathToBeExcluded, YamlChangeSet yamlChangeSet) {
    final GitToHarnessProgressDTO gitToHarnessProgressRecord =
        saveGitToHarnessStatusRecord(yamlGitConfig, branchName, accountId, yamlChangeSet);
    try {
      List<YamlGitConfigDTO> yamlGitConfigDTOS = yamlGitConfigService.getByRepo(yamlGitConfig.getRepo());
      Set<String> foldersList = YamlGitConfigHelper.getRootFolderList(yamlGitConfigDTOS);
      List<GitFileChangeDTO> harnessFilesOfBranch =
          getFilesBelongingToThisBranch(accountId, yamlGitConfig, foldersList, branchName);
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
      gitToHarnessProgressService.updateFilesInProgressRecord(
          gitToHarnessProgressRecord.getUuid(), gitToHarnessFilesToProcess);
      String commitId = getCommitId(harnessFilesOfBranch);
      gitToHarnessProcessorService.processFiles(accountId, gitToHarnessFilesToProcess, branchName, yamlGitConfig,
          commitId, gitToHarnessProgressRecord.getUuid());
    } catch (Exception ex) {
      log.error("Error encountered while synching the branch {}", branchName, ex);
      gitToHarnessProgressService.updateStepStatus(
          gitToHarnessProgressRecord.getUuid(), GitToHarnessProcessingStepStatus.ERROR);
    }
  }

  private String getCommitId(List<GitFileChangeDTO> harnessFilesOfBranch) {
    if (isEmpty(harnessFilesOfBranch)) {
      return null;
    }
    return harnessFilesOfBranch.get(0).getCommitId();
  }

  private GitToHarnessProgressDTO saveGitToHarnessStatusRecord(
      YamlGitConfigDTO yamlGitConfig, String branchName, String accountId, YamlChangeSet yamlChangeSet) {
    GitToHarnessProgressDTO gitToHarnessProgress = GitToHarnessProgressDTO.builder()
                                                       .accountIdentifier(accountId)
                                                       .yamlChangeSetId(yamlChangeSet.getUuid())
                                                       .repoUrl(yamlGitConfig.getRepo())
                                                       .branch(branchName)
                                                       .eventType(YamlChangeSetEventType.BRANCH_SYNC)
                                                       .stepType(GitToHarnessProcessingStepType.GET_FILES)
                                                       .stepStatus(TO_DO)
                                                       .stepStartingTime(System.currentTimeMillis())
                                                       .build();
    return gitToHarnessProgressService.save(gitToHarnessProgress);
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
