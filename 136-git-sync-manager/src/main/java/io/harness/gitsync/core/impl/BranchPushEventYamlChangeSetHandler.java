package io.harness.gitsync.core.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest.GitToHarnessFileProcessingRequestBuilder;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepType;
import io.harness.gitsync.common.beans.GitToHarnessProgressStatus;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;
import io.harness.gitsync.common.beans.YamlChangeSetStatus;
import io.harness.gitsync.common.dtos.GitDiffResultFileDTO;
import io.harness.gitsync.common.dtos.GitDiffResultFileListDTO;
import io.harness.gitsync.common.dtos.GitFileChangeDTO;
import io.harness.gitsync.common.dtos.GitToHarnessGetFilesStepRequest;
import io.harness.gitsync.common.dtos.GitToHarnessGetFilesStepResponse;
import io.harness.gitsync.common.dtos.GitToHarnessProcessMsvcStepRequest;
import io.harness.gitsync.common.dtos.GitToHarnessProcessMsvcStepResponse;
import io.harness.gitsync.common.dtos.GitToHarnessProgressDTO;
import io.harness.gitsync.common.helper.GitToHarnessProgressHelper;
import io.harness.gitsync.common.helper.YamlGitConfigHelper;
import io.harness.gitsync.common.service.GitToHarnessProgressService;
import io.harness.gitsync.common.service.ScmOrchestratorService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.gitsync.core.dtos.GitCommitDTO;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.core.service.YamlChangeSetHandler;
import io.harness.utils.FilePathUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class BranchPushEventYamlChangeSetHandler implements YamlChangeSetHandler {
  private YamlGitConfigService yamlGitConfigService;
  private ScmOrchestratorService scmOrchestratorService;
  private GitCommitService gitCommitService;
  private GitToHarnessProcessorService gitToHarnessProcessorService;
  private GitToHarnessProgressService gitToHarnessProgressService;
  private GitToHarnessProgressHelper gitToHarnessProgressHelper;

  @Override
  public YamlChangeSetStatus process(YamlChangeSetDTO yamlChangeSetDTO) {
    String repoURL = yamlChangeSetDTO.getRepoUrl();

    List<YamlGitConfigDTO> yamlGitConfigDTOList = yamlGitConfigService.getByRepo(repoURL);
    if (yamlGitConfigDTOList.isEmpty()) {
      log.info("Repo {} doesn't exist, ignoring the branch push change set event : {}", repoURL, yamlChangeSetDTO);
      return YamlChangeSetStatus.SKIPPED;
    }

    YamlChangeSetStatus queueStatus =
        gitToHarnessProgressHelper.getQueueStatusIfEventInProgressOrAlreadyProcessed(yamlChangeSetDTO);
    if (queueStatus != null) {
      log.info("Ignoring event {} with queue status {} as event might be already completed or in process",
          yamlChangeSetDTO, queueStatus);
      return queueStatus;
    }

    boolean isCommitAlreadyProcessed = gitCommitService.isCommitAlreadyProcessed(yamlChangeSetDTO.getAccountId(),
        yamlChangeSetDTO.getGitWebhookRequestAttributes().getHeadCommitId(), yamlChangeSetDTO.getRepoUrl(),
        yamlChangeSetDTO.getBranch());
    if (isCommitAlreadyProcessed) {
      log.info("CommitId {} already processed, ignoring the branch push change set event : {}",
          yamlChangeSetDTO.getGitWebhookRequestAttributes().getHeadCommitId(), yamlChangeSetDTO);
      return YamlChangeSetStatus.SKIPPED;
    }

    // Init Progress Record for this event
    GitToHarnessProgressDTO gitToHarnessProgressRecord = gitToHarnessProgressService.initProgress(yamlChangeSetDTO,
        YamlChangeSetEventType.BRANCH_PUSH, GitToHarnessProcessingStepType.GET_FILES,
        yamlChangeSetDTO.getGitWebhookRequestAttributes().getHeadCommitId());

    try {
      GitToHarnessGetFilesStepResponse gitToHarnessGetFilesStepResponse =
          performGetFilesStep(GitToHarnessGetFilesStepRequest.builder()
                                  .yamlChangeSetDTO(yamlChangeSetDTO)
                                  .yamlGitConfigDTOList(yamlGitConfigDTOList)
                                  .gitToHarnessProgress(gitToHarnessProgressRecord)
                                  .build());

      GitToHarnessProcessMsvcStepResponse gitToHarnessProcessMsvcStepResponse = performProcessFilesInMsvcStep(
          GitToHarnessProcessMsvcStepRequest.builder()
              .yamlChangeSetDTO(yamlChangeSetDTO)
              .yamlGitConfigDTO(yamlGitConfigDTOList.get(0))
              .gitFileChangeDTOList(gitToHarnessGetFilesStepResponse.getGitFileChangeDTOList())
              .gitDiffResultFileDTOList(gitToHarnessGetFilesStepResponse.getGitDiffResultFileDTOList())
              .progressRecord(gitToHarnessGetFilesStepResponse.getProgressRecord())
              .build());

      if (gitToHarnessProcessMsvcStepResponse.getGitToHarnessProgressStatus().isFailureStatus()) {
        log.error("G2H process files step failed with status : {}, marking branch push event as FAILED for retry",
            gitToHarnessProcessMsvcStepResponse.getGitToHarnessProgressStatus());
        return YamlChangeSetStatus.FAILED_WITH_RETRY;
      }

      return YamlChangeSetStatus.COMPLETED;
    } catch (Exception ex) {
      log.error("Error while processing branch push event {}", yamlChangeSetDTO, ex);
      // Update the g2h terminal status to ERROR
      gitToHarnessProgressService.updateStepStatus(
          gitToHarnessProgressRecord.getUuid(), GitToHarnessProcessingStepStatus.ERROR);
      return YamlChangeSetStatus.FAILED_WITH_RETRY;
    }
  }

  // ---------------------------------- PRIVATE METHODS -------------------------------

  private GitToHarnessGetFilesStepResponse performGetFilesStep(GitToHarnessGetFilesStepRequest request) {
    List<YamlGitConfigDTO> yamlGitConfigDTOList = request.getYamlGitConfigDTOList();
    YamlChangeSetDTO yamlChangeSetDTO = request.getYamlChangeSetDTO();

    // Mark step status in progress
    GitToHarnessProgressDTO gitToHarnessProgressRecord = gitToHarnessProgressService.updateStepStatus(
        request.getGitToHarnessProgress().getUuid(), GitToHarnessProcessingStepStatus.IN_PROGRESS);

    Set<String> rootFolderList = YamlGitConfigHelper.getRootFolderList(yamlGitConfigDTOList);
    // Fetch files that have changed b/w push event commit id and the local commit id
    List<GitDiffResultFileDTO> prFiles = getDiffFilesUsingSCM(yamlChangeSetDTO, yamlGitConfigDTOList.get(0));
    // We need to process only those files which are in root folders
    List<GitDiffResultFileDTO> prFilesTobeProcessed = getFilePathsToBeProcessed(rootFolderList, prFiles);

    List<GitFileChangeDTO> gitFileChangeDTOList =
        getAllFileContent(yamlChangeSetDTO, yamlGitConfigDTOList.get(0), prFilesTobeProcessed);

    // Mark step status done
    gitToHarnessProgressRecord = gitToHarnessProgressService.updateStepStatus(
        gitToHarnessProgressRecord.getUuid(), GitToHarnessProcessingStepStatus.DONE);

    return GitToHarnessGetFilesStepResponse.builder()
        .gitFileChangeDTOList(gitFileChangeDTOList)
        .gitDiffResultFileDTOList(prFilesTobeProcessed)
        .progressRecord(gitToHarnessProgressRecord)
        .build();
  }

  private GitToHarnessProcessMsvcStepResponse performProcessFilesInMsvcStep(
      GitToHarnessProcessMsvcStepRequest request) {
    List<GitToHarnessFileProcessingRequest> fileProcessingRequests =
        prepareFileProcessingRequests(request.getGitFileChangeDTOList(), request.getGitDiffResultFileDTOList());
    GitToHarnessProgressStatus gitToHarnessProgressStatus =
        gitToHarnessProcessorService.processFiles(request.getYamlChangeSetDTO().getAccountId(), fileProcessingRequests,
            request.getYamlChangeSetDTO().getBranch(), request.getYamlGitConfigDTO().getRepo(),
            request.getYamlChangeSetDTO().getGitWebhookRequestAttributes().getHeadCommitId(),
            request.getProgressRecord().getUuid(), request.getYamlChangeSetDTO().getChangesetId());
    return GitToHarnessProcessMsvcStepResponse.builder().gitToHarnessProgressStatus(gitToHarnessProgressStatus).build();
  }

  // Fetch list of files in the diff b/w last processed commit and new pushed commit, along with their change status
  private List<GitDiffResultFileDTO> getDiffFilesUsingSCM(
      YamlChangeSetDTO yamlChangeSetDTO, YamlGitConfigDTO yamlGitConfigDTO) {
    Optional<GitCommitDTO> gitCommitDTO = gitCommitService.findLastGitCommit(
        yamlChangeSetDTO.getAccountId(), yamlChangeSetDTO.getRepoUrl(), yamlChangeSetDTO.getBranch());
    if (!gitCommitDTO.isPresent()) {
      String errorLog = String.format("No last git commit found for repoURL : %s and branch  %s",
          yamlChangeSetDTO.getRepoUrl(), yamlChangeSetDTO.getBranch());
      throw new InvalidRequestException(errorLog);
    }

    // Call to SCM api to find diff files in push event commit id and local commit id
    String initialCommitId = gitCommitDTO.get().getCommitId();
    String finalCommitId = getWebhookCommitId(yamlChangeSetDTO);
    GitDiffResultFileListDTO gitDiffResultFileListDTO =
        scmOrchestratorService.processScmRequest(scmClientFacilitatorService
            -> scmClientFacilitatorService.listCommitsDiffFiles(yamlGitConfigDTO, initialCommitId, finalCommitId),
            yamlGitConfigDTO.getProjectIdentifier(), yamlGitConfigDTO.getOrganizationIdentifier(),
            yamlGitConfigDTO.getAccountIdentifier());

    return gitDiffResultFileListDTO.getPrFileList();
  }

  // Get content for all files at the incoming webhook's commit id
  private List<GitFileChangeDTO> getAllFileContent(
      YamlChangeSetDTO yamlChangeSetDTO, YamlGitConfigDTO yamlGitConfigDTO, List<GitDiffResultFileDTO> prFile) {
    List<String> filePaths = new ArrayList<>();
    prFile.forEach(file -> filePaths.add(file.getPath()));

    return scmOrchestratorService.processScmRequest(scmClientFacilitatorService
        -> scmClientFacilitatorService.listFilesByCommitId(
            yamlGitConfigDTO, filePaths, getWebhookCommitId(yamlChangeSetDTO)),
        yamlGitConfigDTO.getProjectIdentifier(), yamlGitConfigDTO.getOrganizationIdentifier(),
        yamlGitConfigDTO.getAccountIdentifier());
  }

  private List<GitToHarnessFileProcessingRequest> prepareFileProcessingRequests(
      List<GitFileChangeDTO> gitFileChangeDTOList, List<GitDiffResultFileDTO> gitDiffResultFileDTOList) {
    List<GitToHarnessFileProcessingRequest> fileProcessingRequests = new ArrayList<>();
    Map<String, GitToHarnessFileProcessingRequestBuilder> filePathToRequestBuilderMap = new HashMap<>();

    gitFileChangeDTOList.forEach(gitFileChangeDTO
        -> filePathToRequestBuilderMap.put(
            gitFileChangeDTO.getPath(), GitToHarnessFileProcessingRequest.builder().fileDetails(gitFileChangeDTO)));
    gitDiffResultFileDTOList.forEach(gitPRFileDTO -> {
      fileProcessingRequests.add(
          filePathToRequestBuilderMap.get(gitPRFileDTO.getPath()).changeType(gitPRFileDTO.getChangeType()).build());
    });
    return fileProcessingRequests;
  }

  // Create list of files that are part of folders in the root folder list
  private List<GitDiffResultFileDTO> getFilePathsToBeProcessed(
      Set<String> rootFolderList, List<GitDiffResultFileDTO> prFiles) {
    List<GitDiffResultFileDTO> filesToBeProcessed = new ArrayList<>();

    prFiles.forEach(prFile -> {
      for (String rootFolder : rootFolderList) {
        if (FilePathUtils.isFilePartOfFolder(rootFolder, prFile.getPath())) {
          filesToBeProcessed.add(prFile);
          break;
        }
      }
    });
    return filesToBeProcessed;
  }

  private String getWebhookCommitId(YamlChangeSetDTO yamlChangeSetDTO) {
    return yamlChangeSetDTO.getGitWebhookRequestAttributes().getHeadCommitId();
  }
}
