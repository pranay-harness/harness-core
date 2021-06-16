package io.harness.gitsync.common.impl.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.DONE;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.ERROR;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.IN_PROGRESS;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepType.PROCESS_FILES_IN_MSVS;

import io.harness.EntityType;
import io.harness.Microservice;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.ChangeSets;
import io.harness.gitsync.GitToHarnessInfo;
import io.harness.gitsync.GitToHarnessProcessRequest;
import io.harness.gitsync.GitToHarnessServiceGrpc;
import io.harness.gitsync.ProcessingResponse;
import io.harness.gitsync.common.beans.FileProcessingStatus;
import io.harness.gitsync.common.beans.GitSyncDirection;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProcessingResponse;
import io.harness.gitsync.common.beans.GitToHarnessProcessingResponseDTO;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus;
import io.harness.gitsync.common.beans.GitToHarnessProgressStatus;
import io.harness.gitsync.common.beans.MsvcProcessingFailureStage;
import io.harness.gitsync.common.helper.GitChangeSetMapper;
import io.harness.gitsync.common.helper.GitSyncUtils;
import io.harness.gitsync.common.service.GitToHarnessProgressService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.gitsync.core.beans.GitCommit.GitCommitProcessingStatus;
import io.harness.gitsync.core.dtos.GitCommitDTO;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.gitfileactivity.beans.GitFileProcessingSummary;
import io.harness.gitsync.helpers.ProcessingResponseMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.StringValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class GitToHarnessProcessorServiceImpl implements GitToHarnessProcessorService {
  Map<EntityType, Microservice> entityTypeMicroserviceMap;
  Map<Microservice, GitToHarnessServiceGrpc.GitToHarnessServiceBlockingStub> gitToHarnessServiceGrpcClient;
  GitToHarnessProgressService gitToHarnessProgressService;
  GitCommitService gitCommitService;
  YamlGitConfigService yamlGitConfigService;

  @Override
  public List<GitToHarnessProcessingResponse> processFiles(String accountId,
      List<GitToHarnessFileProcessingRequest> fileContentsList, String branchName, YamlGitConfigDTO yamlGitConfigDTO,
      String commitId, String gitToHarnessProgressRecordId) {
    final List<YamlGitConfigDTO> yamlGitConfigs = yamlGitConfigService.getByRepo(yamlGitConfigDTO.getRepo());
    List<ChangeSet> changeSets = GitChangeSetMapper.toChangeSetList(fileContentsList, accountId, yamlGitConfigs);
    Map<EntityType, List<ChangeSet>> mapOfEntityTypeAndContent = createMapOfEntityTypeAndFileContent(changeSets);
    Map<Microservice, List<ChangeSet>> groupedFilesByMicroservices =
        groupFilesByMicroservices(mapOfEntityTypeAndContent);
    List<GitToHarnessProcessingResponse> gitToHarnessProcessingResponses = new ArrayList<>();
    gitToHarnessProgressService.startNewStep(gitToHarnessProgressRecordId, PROCESS_FILES_IN_MSVS, IN_PROGRESS);
    for (Map.Entry<Microservice, List<ChangeSet>> entry : groupedFilesByMicroservices.entrySet()) {
      Microservice microservice = entry.getKey();
      GitToHarnessServiceGrpc.GitToHarnessServiceBlockingStub gitToHarnessServiceBlockingStub =
          gitToHarnessServiceGrpcClient.get(microservice);
      ChangeSets changeSetForThisMicroservice = ChangeSets.newBuilder().addAllChangeSet(entry.getValue()).build();
      GitToHarnessInfo.Builder gitToHarnessInfo =
          GitToHarnessInfo.newBuilder().setRepoUrl(yamlGitConfigDTO.getRepo()).setBranch(branchName);

      GitToHarnessProcessRequest gitToHarnessProcessRequest = GitToHarnessProcessRequest.newBuilder()
                                                                  .setChangeSets(changeSetForThisMicroservice)
                                                                  .setGitToHarnessBranchInfo(gitToHarnessInfo)
                                                                  .setAccountId(accountId)
                                                                  .setCommitId(StringValue.of(commitId))
                                                                  .build();
      log.info("Sending to microservice {}", entry.getKey());
      GitToHarnessProcessingResponseDTO gitToHarnessProcessingResponseDTO = null;
      try {
        ProcessingResponse processingResponse = gitToHarnessServiceBlockingStub.process(gitToHarnessProcessRequest);
        gitToHarnessProcessingResponseDTO = ProcessingResponseMapper.toProcessingResponseDTO(processingResponse);
        log.info(
            "Got the processing response for the microservice {}, response {}", entry.getKey(), processingResponse);
      } catch (Exception ex) {
        // This exception happens in the case when we are not able to connect to the microservice
        log.info("Exception in file processing for the microservice {}", entry.getKey(), ex);
        gitToHarnessProcessingResponseDTO = GitToHarnessProcessingResponseDTO.builder()
                                                .msvcProcessingFailureStage(MsvcProcessingFailureStage.RECEIVE_STAGE)
                                                .build();
      }
      GitToHarnessProcessingResponse gitToHarnessResponse = GitToHarnessProcessingResponse.builder()
                                                                .processingResponse(gitToHarnessProcessingResponseDTO)
                                                                .microservice(microservice)
                                                                .build();
      gitToHarnessProcessingResponses.add(gitToHarnessResponse);
      gitToHarnessProgressService.updateProgressWithProcessingResponse(
          gitToHarnessProgressRecordId, gitToHarnessResponse);
      log.info("Completed for microservice {}", entry.getKey());
    }
    updateCommit(commitId, accountId, branchName, yamlGitConfigDTO.getRepo(), gitToHarnessProcessingResponses);
    updateTheGitToHarnessStatus(gitToHarnessProgressRecordId, gitToHarnessProcessingResponses);
    return gitToHarnessProcessingResponses;
  }

  private void markSkippedFiles(List<ChangeSet> skippedChangeSet) {
    // @todo deepak mark skipped in progress.
  }

  private void updateTheGitToHarnessStatus(
      String gitToHarnessProgressRecordId, List<GitToHarnessProcessingResponse> gitToHarnessProcessingResponses) {
    GitToHarnessProcessingStepStatus status = getStatus(gitToHarnessProcessingResponses);
    gitToHarnessProgressService.updateStepStatus(gitToHarnessProgressRecordId, status);
    // mark end of the progress for this record
    if (ERROR == status) {
      gitToHarnessProgressService.updateProgressStatus(gitToHarnessProgressRecordId, GitToHarnessProgressStatus.ERROR);
    } else {
      gitToHarnessProgressService.updateProgressStatus(gitToHarnessProgressRecordId, GitToHarnessProgressStatus.DONE);
    }
  }

  private GitToHarnessProcessingStepStatus getStatus(
      List<GitToHarnessProcessingResponse> gitToHarnessProcessingResponses) {
    if (isEmpty(gitToHarnessProcessingResponses)) {
      return DONE;
    }
    for (GitToHarnessProcessingResponse gitToHarnessProcessingResponse : gitToHarnessProcessingResponses) {
      final GitToHarnessProcessingResponseDTO processingResponse =
          gitToHarnessProcessingResponse.getProcessingResponse();
      MsvcProcessingFailureStage processingStageFailure = processingResponse.getMsvcProcessingFailureStage();
      if (processingStageFailure != null) {
        return ERROR;
      }
    }
    return DONE;
  }

  private Map<Microservice, List<ChangeSet>> groupFilesByMicroservices(
      Map<EntityType, List<ChangeSet>> mapOfEntityTypeAndContent) {
    Map<Microservice, List<ChangeSet>> groupedFilesByMicroservices = new HashMap<>();
    if (isEmpty(mapOfEntityTypeAndContent)) {
      return groupedFilesByMicroservices;
    }
    for (Map.Entry<EntityType, List<ChangeSet>> entry : mapOfEntityTypeAndContent.entrySet()) {
      final EntityType entityType = entry.getKey();
      final List<ChangeSet> fileContents = entry.getValue();
      Microservice microservice = entityTypeMicroserviceMap.get(entityType);
      if (groupedFilesByMicroservices.containsKey(microservice)) {
        groupedFilesByMicroservices.get(microservice).addAll(fileContents);
      } else {
        groupedFilesByMicroservices.put(microservice, fileContents);
      }
    }
    return groupedFilesByMicroservices;
  }

  private Map<EntityType, List<ChangeSet>> createMapOfEntityTypeAndFileContent(List<ChangeSet> fileContentsList) {
    Map<EntityType, List<ChangeSet>> mapOfEntityTypeAndContent = new HashMap<>();
    List<ChangeSet> unprocessableChangesets = new ArrayList<>();
    for (ChangeSet fileContent : fileContentsList) {
      final String yamlOfFile = fileContent.getYaml();
      EntityType entityTypeFromYaml;
      try {
        // in case entity type cannot be resolved from yaml we have an unprocessable yaml.
        entityTypeFromYaml = GitSyncUtils.getEntityTypeFromYaml(yamlOfFile);
      } catch (Exception e) {
        log.error("Unknown entity type encountered in file {}", fileContent.getFilePath());
        unprocessableChangesets.add(fileContent);
        continue;
      }
      if (mapOfEntityTypeAndContent.containsKey(entityTypeFromYaml)) {
        mapOfEntityTypeAndContent.get(entityTypeFromYaml).add(fileContent);
      } else {
        List<ChangeSet> newFileContentList = new ArrayList<>();
        newFileContentList.add(fileContent);
        mapOfEntityTypeAndContent.put(entityTypeFromYaml, newFileContentList);
      }
    }
    markSkippedFiles(unprocessableChangesets);
    return mapOfEntityTypeAndContent;
  }

  private void updateCommit(String commitId, String accountId, String branchName, String repoUrl,
      List<GitToHarnessProcessingResponse> gitToHarnessProcessingResponses) {
    GitToHarnessProcessingStepStatus status = getStatus(gitToHarnessProcessingResponses);
    if (status == DONE) {
      GitCommitDTO gitCommitDTO =
          GitCommitDTO.builder()
              .commitId(commitId)
              .gitSyncDirection(GitSyncDirection.GIT_TO_HARNESS)
              .accountIdentifier(accountId)
              .branchName(branchName)
              .repoURL(repoUrl)
              .status(GitCommitProcessingStatus.COMPLETED)
              .fileProcessingSummary(prepareGitFileProcessingSummary(gitToHarnessProcessingResponses))
              .build();
      gitCommitService.upsertOnCommitIdAndRepoUrlAndGitSyncDirection(gitCommitDTO);
    }
  }

  private GitFileProcessingSummary prepareGitFileProcessingSummary(
      List<GitToHarnessProcessingResponse> gitToHarnessProcessingResponses) {
    Map<FileProcessingStatus, Long> fileStatusToCountMap = new HashMap<>();
    gitToHarnessProcessingResponses.forEach(gitToHarnessProcessingResponse
        -> gitToHarnessProcessingResponse.getProcessingResponse().getFileResponses().forEach(
            fileProcessingResponseDTO -> {
              long count = fileStatusToCountMap.getOrDefault(fileProcessingResponseDTO.getFileProcessingStatus(), 0L);
              fileStatusToCountMap.put(fileProcessingResponseDTO.getFileProcessingStatus(), count + 1);
            }));
    long failureCount = fileStatusToCountMap.getOrDefault(FileProcessingStatus.FAILURE, 0L);
    long queuedCount = fileStatusToCountMap.getOrDefault(FileProcessingStatus.UNPROCESSED, 0L);
    long skippedCount = fileStatusToCountMap.getOrDefault(FileProcessingStatus.SKIPPED, 0L);
    long successCount = fileStatusToCountMap.getOrDefault(FileProcessingStatus.SUCCESS, 0L);
    return GitFileProcessingSummary.builder()
        .failureCount(failureCount)
        .queuedCount(queuedCount)
        .skippedCount(skippedCount)
        .successCount(successCount)
        .totalCount(failureCount + queuedCount + skippedCount + successCount)
        .build();
  }
}
