package io.harness.gitsync.common.impl.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.DONE;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.ERROR;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.IN_PROGRESS;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepType.PROCESS_FILES_IN_MSVS;

import static java.util.stream.Collectors.toList;

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
import io.harness.gitsync.common.dtos.ChangeSetWithYamlStatusDTO;
import io.harness.gitsync.common.helper.GitChangeSetMapper;
import io.harness.gitsync.common.helper.GitSyncGrpcClientUtils;
import io.harness.gitsync.common.service.GitToHarnessProgressService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.gitsync.core.beans.GitCommit.GitCommitProcessingStatus;
import io.harness.gitsync.core.dtos.GitCommitDTO;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.gitfileactivity.beans.GitFileProcessingSummary;
import io.harness.gitsync.helpers.ProcessingResponseMapper;
import io.harness.ng.core.event.EventProtoToEntityHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.StringValue;
import com.hierynomus.utils.Strings;
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
  GitChangeSetMapper gitChangeSetMapper;

  @Override
  public GitToHarnessProgressStatus processFiles(String accountId,
      List<GitToHarnessFileProcessingRequest> fileContentsList, String branchName, String repoUrl, String commitId,
      String gitToHarnessProgressRecordId, String changeSetId) {
    final List<YamlGitConfigDTO> yamlGitConfigs = yamlGitConfigService.getByRepo(repoUrl);
    List<ChangeSetWithYamlStatusDTO> changeSetsWithYamlStatus =
        gitChangeSetMapper.toChangeSetList(fileContentsList, accountId, yamlGitConfigs, changeSetId, branchName);
    final List<ChangeSet> invalidChangeSets = markSkippedFiles(changeSetsWithYamlStatus);
    Map<EntityType, List<ChangeSet>> mapOfEntityTypeAndContent =
        createMapOfEntityTypeAndFileContent(changeSetsWithYamlStatus);
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
          GitToHarnessInfo.newBuilder().setRepoUrl(repoUrl).setBranch(branchName);

      GitToHarnessProcessRequest gitToHarnessProcessRequest = GitToHarnessProcessRequest.newBuilder()
                                                                  .setChangeSets(changeSetForThisMicroservice)
                                                                  .setGitToHarnessBranchInfo(gitToHarnessInfo)
                                                                  .setAccountId(accountId)
                                                                  .setCommitId(StringValue.of(commitId))
                                                                  .build();
      // TODO log for debug purpose, remove after use
      log.info("Sending to microservice {}, request : {}", entry.getKey(), gitToHarnessProcessRequest);
      GitToHarnessProcessingResponseDTO gitToHarnessProcessingResponseDTO = null;
      try {
        ProcessingResponse processingResponse = GitSyncGrpcClientUtils.retryAndProcessException(
            gitToHarnessServiceBlockingStub::process, gitToHarnessProcessRequest);
        gitToHarnessProcessingResponseDTO = ProcessingResponseMapper.toProcessingResponseDTO(processingResponse);
        log.info(
            "Got the processing response for the microservice {}, response {}", entry.getKey(), processingResponse);
      } catch (Exception ex) {
        // This exception happens in the case when we are not able to connect to the microservice
        log.error("Exception in file processing for the microservice {}", entry.getKey(), ex);
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
    updateCommit(commitId, accountId, branchName, repoUrl, gitToHarnessProcessingResponses, invalidChangeSets);
    return updateTheGitToHarnessStatus(gitToHarnessProgressRecordId, gitToHarnessProcessingResponses);
  }

  private List<ChangeSet> markSkippedFiles(List<ChangeSetWithYamlStatusDTO> changeSets) {
    final List<ChangeSet> inValidChangeSets = getInValidChangeSets(changeSets);
    // todo @deepak: Store the changesets too
    List<String> filePaths = emptyIfNull(inValidChangeSets).stream().map(ChangeSet::getFilePath).collect(toList());
    log.info("Skipped processing the files [{}]", Strings.join(filePaths, ' '));
    return inValidChangeSets;
  }

  private List<ChangeSet> getInValidChangeSets(List<ChangeSetWithYamlStatusDTO> changeSets) {
    return emptyIfNull(changeSets)
        .stream()
        .filter(changeSet -> changeSet.getYamlInputErrorType() != ChangeSetWithYamlStatusDTO.YamlInputErrorType.NIL)
        .map(ChangeSetWithYamlStatusDTO::getChangeSet)
        .collect(toList());
  }

  private GitToHarnessProgressStatus updateTheGitToHarnessStatus(
      String gitToHarnessProgressRecordId, List<GitToHarnessProcessingResponse> gitToHarnessProcessingResponses) {
    GitToHarnessProcessingStepStatus status = getStatus(gitToHarnessProcessingResponses);
    gitToHarnessProgressService.updateStepStatus(gitToHarnessProgressRecordId, status);
    // mark end of the progress for this record
    if (ERROR == status) {
      gitToHarnessProgressService.updateProgressStatus(gitToHarnessProgressRecordId, GitToHarnessProgressStatus.ERROR);
      return GitToHarnessProgressStatus.ERROR;
    } else {
      gitToHarnessProgressService.updateProgressStatus(gitToHarnessProgressRecordId, GitToHarnessProgressStatus.DONE);
      return GitToHarnessProgressStatus.DONE;
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

  private Map<EntityType, List<ChangeSet>> createMapOfEntityTypeAndFileContent(
      List<ChangeSetWithYamlStatusDTO> changeSets) {
    List<ChangeSet> validChangeSetList = getValidChangeSets(changeSets);
    Map<EntityType, List<ChangeSet>> mapOfEntityTypeAndContent = new HashMap<>();
    for (ChangeSet fileContent : validChangeSetList) {
      EntityType entityTypeFromYaml = null;
      try {
        entityTypeFromYaml = EventProtoToEntityHelper.getEntityTypeFromProto(fileContent.getEntityType());
      } catch (Exception ex) {
        log.error("Exception getting the yaml type, skipping this file [{}]", fileContent.getFilePath(), ex);
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
    return mapOfEntityTypeAndContent;
  }

  private List<ChangeSet> getValidChangeSets(List<ChangeSetWithYamlStatusDTO> changeSets) {
    return emptyIfNull(changeSets)
        .stream()
        .filter(changeSet -> changeSet.getYamlInputErrorType() == ChangeSetWithYamlStatusDTO.YamlInputErrorType.NIL)
        .map(ChangeSetWithYamlStatusDTO::getChangeSet)
        .collect(toList());
  }

  private void updateCommit(String commitId, String accountId, String branchName, String repoUrl,
      List<GitToHarnessProcessingResponse> gitToHarnessProcessingResponses, List<ChangeSet> invalidChangeSets) {
    GitToHarnessProcessingStepStatus status = getStatus(gitToHarnessProcessingResponses);
    if (status == DONE) {
      GitCommitDTO gitCommitDTO = GitCommitDTO.builder()
                                      .commitId(commitId)
                                      .gitSyncDirection(GitSyncDirection.GIT_TO_HARNESS)
                                      .accountIdentifier(accountId)
                                      .branchName(branchName)
                                      .repoURL(repoUrl)
                                      .status(GitCommitProcessingStatus.COMPLETED)
                                      .fileProcessingSummary(prepareGitFileProcessingSummary(
                                          gitToHarnessProcessingResponses, invalidChangeSets))
                                      .build();
      gitCommitService.upsertOnCommitIdAndRepoUrlAndGitSyncDirection(gitCommitDTO);
    }
  }

  private GitFileProcessingSummary prepareGitFileProcessingSummary(
      List<GitToHarnessProcessingResponse> gitToHarnessProcessingResponses, List<ChangeSet> invalidChangeSet) {
    Map<FileProcessingStatus, Long> fileStatusToCountMap = new HashMap<>();
    gitToHarnessProcessingResponses.forEach(gitToHarnessProcessingResponse
        -> gitToHarnessProcessingResponse.getProcessingResponse().getFileResponses().forEach(
            fileProcessingResponseDTO -> {
              long count = fileStatusToCountMap.getOrDefault(fileProcessingResponseDTO.getFileProcessingStatus(), 0L);
              fileStatusToCountMap.put(fileProcessingResponseDTO.getFileProcessingStatus(), count + 1);
            }));
    long failureCount = fileStatusToCountMap.getOrDefault(FileProcessingStatus.FAILURE, 0L);
    long queuedCount = fileStatusToCountMap.getOrDefault(FileProcessingStatus.UNPROCESSED, 0L);
    long skippedCount =
        fileStatusToCountMap.getOrDefault(FileProcessingStatus.SKIPPED, 0L) + emptyIfNull(invalidChangeSet).size();
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
