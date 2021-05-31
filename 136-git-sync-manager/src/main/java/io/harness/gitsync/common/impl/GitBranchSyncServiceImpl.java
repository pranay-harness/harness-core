package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.ChangeType;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.service.GitBranchSyncService;
import io.harness.gitsync.common.service.ScmOrchestratorService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileContent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class GitBranchSyncServiceImpl implements GitBranchSyncService {
  GitToHarnessProcessorService gitToHarnessProcessorService;
  ScmOrchestratorService scmOrchestratorService;

  @Override
  public void syncBranch(
      YamlGitConfigDTO yamlGitConfig, String branchName, String accountId, String filePathToBeExcluded) {
    FileBatchContentResponse harnessFilesOfBranch = getFilesBelongingToThisBranch(accountId, yamlGitConfig, branchName);
    List<FileContent> filteredFileList = getFilteredFiles(harnessFilesOfBranch, filePathToBeExcluded);
    if (isEmpty(filteredFileList)) {
      log.info("No files needed to be processed for the branch {}", branchName);
    }
    List<GitToHarnessFileProcessingRequest> gitToHarnessFilesToProcess =
        filteredFileList.stream()
            .map(fileContent
                -> GitToHarnessFileProcessingRequest.builder()
                       .fileDetails(fileContent)
                       .changeType(ChangeType.ADD)
                       .build())
            .collect(toList());
    gitToHarnessProcessorService.processFiles(accountId, gitToHarnessFilesToProcess, branchName, yamlGitConfig);
  }

  private FileBatchContentResponse getFilesBelongingToThisBranch(
      String accountIdentifier, YamlGitConfigDTO yamlGitConfig, String branchName) {
    List<String> foldersList = CollectionUtils.emptyIfNull(yamlGitConfig.getRootFolders())
                                   .stream()
                                   .map(YamlGitConfigDTO.RootFolder::getRootFolder)
                                   .collect(toList());
    return scmOrchestratorService.processScmRequest(scmClientFacilitatorService
        -> scmClientFacilitatorService.listFilesOfBranches(accountIdentifier, yamlGitConfig.getOrganizationIdentifier(),
            yamlGitConfig.getProjectIdentifier(), yamlGitConfig.getIdentifier(), foldersList, branchName),
        yamlGitConfig.getProjectIdentifier(), yamlGitConfig.getOrganizationIdentifier(), accountIdentifier);
  }

  private List<FileContent> getFilteredFiles(
      FileBatchContentResponse allFilesOfDefaultBranch, String filePathToBeExcluded) {
    List<FileContent> fileContents = allFilesOfDefaultBranch.getFileContentsList();
    List<FileContent> filteredFileContents = new ArrayList<>();
    for (FileContent fileContent : fileContents) {
      if (fileContent.getPath().equals(filePathToBeExcluded)) {
        continue;
      }
      filteredFileContents.add(fileContent);
    }
    return filteredFileContents;
  }
}
