package io.harness.gitsync.common.impl.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.DONE;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.ERROR;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.IN_PROGRESS;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepType.PROCESS_FILES_IN_MSVS;
import static io.harness.ng.core.event.EntityToEntityProtoHelper.getEntityTypeFromProto;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.harness.EntityType;
import io.harness.Microservice;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.ChangeSets;
import io.harness.gitsync.EntityInfo;
import io.harness.gitsync.EntityInfos;
import io.harness.gitsync.GitToHarnessInfo;
import io.harness.gitsync.GitToHarnessProcessRequest;
import io.harness.gitsync.GitToHarnessServiceGrpc;
import io.harness.gitsync.MarkEntityInvalidRequest;
import io.harness.gitsync.MarkEntityInvalidResponse;
import io.harness.gitsync.ProcessingResponse;
import io.harness.gitsync.common.beans.FileProcessingResponseDTO;
import io.harness.gitsync.common.beans.FileProcessingStatus;
import io.harness.gitsync.common.beans.GitSyncDirection;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProcessingInfo;
import io.harness.gitsync.common.beans.GitToHarnessProcessingResponse;
import io.harness.gitsync.common.beans.GitToHarnessProcessingResponseDTO;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus;
import io.harness.gitsync.common.beans.GitToHarnessProgressStatus;
import io.harness.gitsync.common.beans.MsvcProcessingFailureStage;
import io.harness.gitsync.common.dtos.ChangeSetWithYamlStatusDTO;
import io.harness.gitsync.common.dtos.GitSyncEntityDTO;
import io.harness.gitsync.common.helper.GitChangeSetMapper;
import io.harness.gitsync.common.helper.GitSyncGrpcClientUtils;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.gitsync.common.service.GitToHarnessProgressService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.gitsync.core.beans.GitCommit.GitCommitProcessingStatus;
import io.harness.gitsync.core.dtos.GitCommitDTO;
import io.harness.gitsync.core.service.GitCommitService;
import io.harness.gitsync.gitfileactivity.beans.GitFileProcessingSummary;
import io.harness.gitsync.gitsyncerror.GitSyncErrorStatus;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitToHarnessErrorDetailsDTO;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import io.harness.gitsync.helpers.ProcessingResponseMapper;
import io.harness.ng.core.event.EventProtoToEntityHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.StringValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
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
  GitSyncErrorService gitSyncErrorService;
  GitEntityService gitEntityService;

  @Override
  public GitToHarnessProgressStatus processFiles(String accountId,
      List<GitToHarnessFileProcessingRequest> fileContentsList, String branchName, String repoUrl, String commitId,
      String gitToHarnessProgressRecordId, String changeSetId, String commitMessage) {
    final List<YamlGitConfigDTO> yamlGitConfigs = yamlGitConfigService.getByRepo(repoUrl);

    GitToHarnessProcessingInfo gitToHarnessProcessingInfo =
        GitToHarnessProcessingInfo.builder()
            .accountId(accountId)
            .repoUrl(repoUrl)
            .branchName(branchName)
            .commitId(commitId)
            .gitToHarnessProgressRecordId(gitToHarnessProgressRecordId)
            .commitMessage(commitMessage)
            .build();
    List<ChangeSetWithYamlStatusDTO> changeSetsWithYamlStatus =
        gitChangeSetMapper.toChangeSetList(fileContentsList, accountId, yamlGitConfigs, changeSetId, branchName);
    List<ChangeSetWithYamlStatusDTO> invalidChangeSetWithYamlStatusDTO =
        getInvalidChangeSetWithYamlStatusDTO(changeSetsWithYamlStatus);

    markEntitiesInvalidForYamlsWhichCouldNotBeParsedByGitMicroService(
        invalidChangeSetWithYamlStatusDTO, accountId, commitId, repoUrl, branchName);

    List<GitSyncErrorDTO> gitToHarnessErrors =
        new ArrayList<>(getErrorsForInvalidChangeSets(gitToHarnessProcessingInfo, invalidChangeSetWithYamlStatusDTO));
    final List<ChangeSet> invalidChangeSets = markSkippedFiles(invalidChangeSetWithYamlStatusDTO);
    Set<String> filePathsHavingError = getFilePathsFromChangeSet(invalidChangeSets);

    Map<EntityType, List<ChangeSet>> mapOfEntityTypeAndContent =
        createMapOfEntityTypeAndFileContent(changeSetsWithYamlStatus);
    Map<Microservice, List<ChangeSet>> groupedFilesByMicroservices =
        groupFilesByMicroservices(mapOfEntityTypeAndContent);

    List<GitToHarnessProcessingResponse> gitToHarnessProcessingResponses = processInternal(
        gitToHarnessProcessingInfo, groupedFilesByMicroservices, gitToHarnessErrors, filePathsHavingError);
    gitToHarnessProcessingResponses.addAll(processNotFoundFiles(changeSetsWithYamlStatus, accountId));
    Set<String> filePathsWithoutError = getFilePathsWithoutError(gitToHarnessProcessingResponses);
    gitSyncErrorService.markResolvedErrors(accountId, repoUrl, branchName, filePathsWithoutError, commitId);
    updateCommit(commitId, accountId, branchName, repoUrl, gitToHarnessProcessingResponses, invalidChangeSets);
    return updateTheGitToHarnessStatus(gitToHarnessProgressRecordId, gitToHarnessProcessingResponses);
  }

  private void markEntitiesInvalidForYamlsWhichCouldNotBeParsedByGitMicroService(
      List<ChangeSetWithYamlStatusDTO> invalidChangeSets, String accountId, String commitId, String repoUrl,
      String branch) {
    Map<Microservice, List<EntityInfo>> microserviceToEntityInfoMapping = new EnumMap<>(Microservice.class);
    List<ChangeSet> changeSets =
        invalidChangeSets.stream().map(ChangeSetWithYamlStatusDTO::getChangeSet).collect(toList());
    changeSets.forEach(changeSet -> {
      Optional<GitSyncEntityDTO> optionalGitSyncEntityDTO =
          gitEntityService.get(changeSet.getAccountId(), changeSet.getFilePath(), repoUrl, branch);
      if (optionalGitSyncEntityDTO.isPresent()) {
        GitSyncEntityDTO gitSyncEntityDTO = optionalGitSyncEntityDTO.get();
        if (gitSyncEntityDTO.getEntityReference() != null) {
          YamlGitConfigDTO yamlGitConfigDTO = yamlGitConfigService.getByProjectIdAndRepo(accountId,
              gitSyncEntityDTO.getEntityReference().getOrgIdentifier(),
              gitSyncEntityDTO.getEntityReference().getProjectIdentifier(), repoUrl);
          EntityInfo entityInfo = getEntityInfo(changeSet, gitSyncEntityDTO, yamlGitConfigDTO.getIdentifier());

          Microservice microservice = entityTypeMicroserviceMap.get(gitSyncEntityDTO.getEntityType());
          if (microservice != null) {
            List<EntityInfo> entityInfoList =
                microserviceToEntityInfoMapping.getOrDefault(microservice, new ArrayList<>());
            entityInfoList.add(entityInfo);
            microserviceToEntityInfoMapping.put(microservice, entityInfoList);
          }
        }
      }

      microserviceToEntityInfoMapping.forEach((microservice, entityInfoList) -> {
        List<EntityInfo> successfullyMarkedInvalid = markEntitiesInvalidInternal(microservice, commitId, accountId,
            GitToHarnessInfo.newBuilder().setBranch(branch).setRepoUrl(repoUrl).build(), entityInfoList);
        log.info("These entities were marked as invalid: {}", successfullyMarkedInvalid);
      });
    });
  }

  private List<EntityInfo> markEntitiesInvalidInternal(Microservice microservice, String commitId, String accountId,
      GitToHarnessInfo gitInfo, List<EntityInfo> entities) {
    GitToHarnessServiceGrpc.GitToHarnessServiceBlockingStub gitToHarnessServiceBlockingStub =
        gitToHarnessServiceGrpcClient.get(microservice);

    if (!entities.isEmpty()) {
      log.info("These entities are being marked as invalid: {}", entities);
      EntityInfos entityInfos = EntityInfos.newBuilder().addAllEntityInfoList(entities).build();
      MarkEntityInvalidResponse markEntityInvalidResponse =
          gitToHarnessServiceBlockingStub.markEntitiesInvalid(MarkEntityInvalidRequest.newBuilder()
                                                                  .setAccountId(accountId)
                                                                  .setBranchInfo(gitInfo)
                                                                  .setCommitId(StringValue.of(commitId))
                                                                  .setEntityInfoList(entityInfos)
                                                                  .build());
      return markEntityInvalidResponse.getEntityInfos().getEntityInfoListList();
    }
    return new ArrayList<>();
  }

  private EntityInfo getEntityInfo(
      @NotNull ChangeSet changeSet, @NotNull GitSyncEntityDTO gitSyncEntityDTO, String yamlGitConfigId) {
    EntityInfo.Builder builder = EntityInfo.newBuilder()
                                     .setYaml(changeSet.getYaml())
                                     .setEntityType(getEntityTypeFromProto(gitSyncEntityDTO.getEntityType()))
                                     .setFilePath(changeSet.getFilePath())
                                     .setYamlGitConfigId(yamlGitConfigId)
                                     .setLastObjectId(changeSet.getObjectId());
    if (gitSyncEntityDTO.getEntityReference() != null) {
      builder.setOrgIdentifier(gitSyncEntityDTO.getEntityReference().getOrgIdentifier())
          .setProjectIdentifier(gitSyncEntityDTO.getEntityReference().getProjectIdentifier())
          .setIdentifier(gitSyncEntityDTO.getEntityReference().getIdentifier());
    }
    return builder.build();
  }

  public void markEntitiesInvalidForYamlsWhichCouldBeProcessedByMicroService(Microservice microservice,
      Map<String, ChangeSet> filePathToChangeSetMap, GitToHarnessProcessRequest request,
      GitToHarnessProcessingResponseDTO response) {
    List<EntityInfo> entityInfoList = new ArrayList<>();
    response.getFileResponses()
        .stream()
        .filter(x -> x.getFileProcessingStatus() == FileProcessingStatus.FAILURE)
        .map(FileProcessingResponseDTO::getFilePath)
        .map(filePathToChangeSetMap::get)
        .filter(Objects::nonNull)
        .forEach(changeSet -> {
          Optional<GitSyncEntityDTO> optionalGitSyncEntityDTO =
              gitEntityService.get(request.getAccountId(), changeSet.getFilePath(),
                  request.getGitToHarnessBranchInfo().getRepoUrl(), request.getGitToHarnessBranchInfo().getBranch());
          if (optionalGitSyncEntityDTO.isPresent()) {
            entityInfoList.add(getEntityInfo(
                changeSet, optionalGitSyncEntityDTO.get(), changeSet.getYamlGitConfigInfo().getYamlGitConfigId()));
          }
        });
    List<EntityInfo> entities = markEntitiesInvalidInternal(microservice, request.getCommitId().getValue(),
        request.getAccountId(), request.getGitToHarnessBranchInfo(), entityInfoList);
    log.info("These entities were successfully marked invalid: {}", entities);
  }

  private List<GitToHarnessProcessingResponse> processInternal(GitToHarnessProcessingInfo gitToHarnessProcessingInfo,
      Map<Microservice, List<ChangeSet>> groupedFilesByMicroservices, List<GitSyncErrorDTO> gitToHarnessErrors,
      Set<String> filePathsHavingError) {
    List<GitToHarnessProcessingResponse> gitToHarnessProcessingResponses = new ArrayList<>();
    gitToHarnessProgressService.startNewStep(
        gitToHarnessProcessingInfo.getGitToHarnessProgressRecordId(), PROCESS_FILES_IN_MSVS, IN_PROGRESS);
    for (Map.Entry<Microservice, List<ChangeSet>> entry : groupedFilesByMicroservices.entrySet()) {
      Microservice microservice = entry.getKey();
      Map<String, ChangeSet> filePathToChangeSetMap =
          entry.getValue().stream().collect(Collectors.toMap(ChangeSet::getFilePath, Function.identity()));
      GitToHarnessServiceGrpc.GitToHarnessServiceBlockingStub gitToHarnessServiceBlockingStub =
          gitToHarnessServiceGrpcClient.get(microservice);
      ChangeSets changeSetForThisMicroservice = ChangeSets.newBuilder().addAllChangeSet(entry.getValue()).build();
      GitToHarnessInfo.Builder gitToHarnessInfo = GitToHarnessInfo.newBuilder()
                                                      .setRepoUrl(gitToHarnessProcessingInfo.getRepoUrl())
                                                      .setBranch(gitToHarnessProcessingInfo.getBranchName());

      GitToHarnessProcessRequest gitToHarnessProcessRequest =
          GitToHarnessProcessRequest.newBuilder()
              .setChangeSets(changeSetForThisMicroservice)
              .setGitToHarnessBranchInfo(gitToHarnessInfo)
              .setAccountId(gitToHarnessProcessingInfo.getAccountId())
              .setCommitId(StringValue.of(gitToHarnessProcessingInfo.getCommitId()))
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
        markEntitiesInvalidForYamlsWhichCouldBeProcessedByMicroService(
            microservice, filePathToChangeSetMap, gitToHarnessProcessRequest, gitToHarnessProcessingResponseDTO);
      } catch (Exception ex) {
        // This exception happens in the case when we are not able to connect to the microservice
        log.error("Exception in file processing for the microservice {}", entry.getKey(), ex);
        gitToHarnessProcessingResponseDTO = GitToHarnessProcessingResponseDTO.builder()
                                                .msvcProcessingFailureStage(MsvcProcessingFailureStage.RECEIVE_STAGE)
                                                .build();
      }
      List<FileProcessingResponseDTO> fileResponsesHavingError =
          getFileResponsesHavingError(gitToHarnessProcessingResponseDTO.getFileResponses());
      gitToHarnessErrors.addAll(
          getErrorsForProcessingResponse(gitToHarnessProcessingInfo, entry.getValue(), fileResponsesHavingError));
      filePathsHavingError.addAll(getFilePathsFromFileResponses(fileResponsesHavingError));
      GitToHarnessProcessingResponse gitToHarnessResponse = GitToHarnessProcessingResponse.builder()
                                                                .processingResponse(gitToHarnessProcessingResponseDTO)
                                                                .microservice(microservice)
                                                                .build();
      gitToHarnessProcessingResponses.add(gitToHarnessResponse);
      gitToHarnessProgressService.updateProgressWithProcessingResponse(
          gitToHarnessProcessingInfo.getGitToHarnessProgressRecordId(), gitToHarnessResponse);
      log.info("Completed for microservice {}", entry.getKey());
    }
    gitSyncErrorService.markOverriddenErrors(gitToHarnessProcessingInfo.getAccountId(),
        gitToHarnessProcessingInfo.getRepoUrl(), gitToHarnessProcessingInfo.getBranchName(), filePathsHavingError);
    gitSyncErrorService.saveAll(gitToHarnessErrors);
    return gitToHarnessProcessingResponses;
  }

  private List<ChangeSet> markSkippedFiles(List<ChangeSetWithYamlStatusDTO> invalidChangeSetWithYamlStatusDTO) {
    final List<ChangeSet> inValidChangeSets = emptyIfNull(invalidChangeSetWithYamlStatusDTO)
                                                  .stream()
                                                  .map(ChangeSetWithYamlStatusDTO::getChangeSet)
                                                  .collect(toList());
    // todo @deepak: Store the changesets too
    Set<String> filePaths = getFilePathsFromChangeSet(inValidChangeSets);
    log.info("Skipped processing the files [{}]", String.join(" ", filePaths));
    return inValidChangeSets;
  }

  private Set<String> getFilePathsFromChangeSet(List<ChangeSet> changeSets) {
    return emptyIfNull(changeSets).stream().map(ChangeSet::getFilePath).collect(Collectors.toSet());
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

  private List<ChangeSetWithYamlStatusDTO> getInvalidChangeSetWithYamlStatusDTO(
      List<ChangeSetWithYamlStatusDTO> changeSets) {
    return emptyIfNull(changeSets)
        .stream()
        .filter(changeSet -> changeSet.getYamlInputErrorType() != ChangeSetWithYamlStatusDTO.YamlInputErrorType.NIL)
        .filter(changeSet
            -> changeSet.getYamlInputErrorType() != ChangeSetWithYamlStatusDTO.YamlInputErrorType.ENTITY_NOT_FOUND)
        .collect(toList());
  }

  private List<GitSyncErrorDTO> getErrorsForInvalidChangeSets(GitToHarnessProcessingInfo gitToHarnessProcessingInfo,
      List<ChangeSetWithYamlStatusDTO> invalidChangeSetWithYamlStatusDTO) {
    List<GitSyncErrorDTO> gitToHarnessErrors = new ArrayList<>();
    invalidChangeSetWithYamlStatusDTO.forEach(changeSetWithYamlStatusDTO -> {
      String errorMessage = changeSetWithYamlStatusDTO.getYamlInputErrorType().getValue();
      gitToHarnessErrors.add(
          buildGitSyncErrorDTO(gitToHarnessProcessingInfo, errorMessage, changeSetWithYamlStatusDTO.getChangeSet()));
    });
    return gitToHarnessErrors;
  }

  private List<FileProcessingResponseDTO> getFileResponsesHavingError(List<FileProcessingResponseDTO> fileResponses) {
    return emptyIfNull(fileResponses)
        .stream()
        .filter(fileResponse -> fileResponse.getFileProcessingStatus() == FileProcessingStatus.FAILURE)
        .collect(toList());
  }

  private List<GitSyncErrorDTO> getErrorsForProcessingResponse(GitToHarnessProcessingInfo gitToHarnessProcessingInfo,
      List<ChangeSet> changeSets, List<FileProcessingResponseDTO> fileResponsesHavingError) {
    List<GitSyncErrorDTO> gitToHarnessErrors = new ArrayList<>();
    Map<FileProcessingResponseDTO, ChangeSet> mapOfChangeSetAndFileResponse =
        getFileProcessingResponseToChangeSetMap(fileResponsesHavingError, changeSets);
    emptyIfNull(fileResponsesHavingError).forEach(fileProcessingResponseDTO -> {
      gitToHarnessErrors.add(buildGitSyncErrorDTO(gitToHarnessProcessingInfo,
          fileProcessingResponseDTO.getErrorMessage(), mapOfChangeSetAndFileResponse.get(fileProcessingResponseDTO)));
    });
    return gitToHarnessErrors;
  }

  private GitSyncErrorDTO buildGitSyncErrorDTO(
      GitToHarnessProcessingInfo gitToHarnessProcessingInfo, String errorMessage, ChangeSet changeSet) {
    return GitSyncErrorDTO.builder()
        .accountIdentifier(gitToHarnessProcessingInfo.getAccountId())
        .repoUrl(gitToHarnessProcessingInfo.getRepoUrl())
        .branchName(gitToHarnessProcessingInfo.getBranchName())
        .errorType(GitSyncErrorType.GIT_TO_HARNESS)
        .status(GitSyncErrorStatus.ACTIVE)
        .completeFilePath(changeSet.getFilePath())
        .failureReason(errorMessage)
        .changeType(io.harness.git.model.ChangeType.valueOf(changeSet.getChangeType().toString()))
        .entityType(EntityType.fromString(changeSet.getEntityType().toString()))
        .additionalErrorDetails(GitToHarnessErrorDetailsDTO.builder()
                                    .gitCommitId(gitToHarnessProcessingInfo.getCommitId())
                                    .yamlContent(changeSet.getYaml())
                                    .commitMessage(gitToHarnessProcessingInfo.getCommitMessage())
                                    .build())
        .build();
  }

  private Map<FileProcessingResponseDTO, ChangeSet> getFileProcessingResponseToChangeSetMap(
      List<FileProcessingResponseDTO> filesHavingError, List<ChangeSet> changeSets) {
    Map<String, ChangeSet> filePathChangeSetMap =
        changeSets.stream().collect(toMap(ChangeSet::getFilePath, Function.identity()));
    return filesHavingError.stream().collect(toMap(Function.identity(),
        fileProcessingResponseDTO -> filePathChangeSetMap.get(fileProcessingResponseDTO.getFilePath())));
  }

  private Set<String> getFilePathsFromFileResponses(List<FileProcessingResponseDTO> fileResponses) {
    return emptyIfNull(fileResponses).stream().map(FileProcessingResponseDTO::getFilePath).collect(Collectors.toSet());
  }

  private Set<String> getFilePathsWithoutError(List<GitToHarnessProcessingResponse> gitToHarnessProcessingResponses) {
    List<FileProcessingResponseDTO> fileResponses = new ArrayList<>();
    gitToHarnessProcessingResponses.stream()
        .map(GitToHarnessProcessingResponse::getProcessingResponse)
        .map(GitToHarnessProcessingResponseDTO::getFileResponses)
        .forEach(fileProcessingResponseDTO -> fileResponses.addAll(fileProcessingResponseDTO));
    return fileResponses.stream()
        .filter(fileResponse -> fileResponse.getFileProcessingStatus() == FileProcessingStatus.SUCCESS)
        .map(FileProcessingResponseDTO::getFilePath)
        .collect(Collectors.toSet());
  }

  private List<GitToHarnessProcessingResponse> processNotFoundFiles(
      List<ChangeSetWithYamlStatusDTO> changeSetsWithYamlStatus, String accountId) {
    List<String> filePaths =
        emptyIfNull(changeSetsWithYamlStatus)
            .stream()
            .filter(changeSet
                -> changeSet.getYamlInputErrorType() == ChangeSetWithYamlStatusDTO.YamlInputErrorType.ENTITY_NOT_FOUND)
            .map(ChangeSetWithYamlStatusDTO::getChangeSet)
            .map(ChangeSet::getFilePath)
            .collect(toList());
    if (filePaths.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    List<FileProcessingResponseDTO> fileProcessingResponses =
        filePaths.stream()
            .map(filePath
                -> FileProcessingResponseDTO.builder()
                       .filePath(filePath)
                       .fileProcessingStatus(FileProcessingStatus.SUCCESS)
                       .build())
            .collect(Collectors.toList());
    GitToHarnessProcessingResponseDTO gitToHarnessProcessingResponseDTO =
        GitToHarnessProcessingResponseDTO.builder().fileResponses(fileProcessingResponses).accountId(accountId).build();
    return Collections.singletonList(
        GitToHarnessProcessingResponse.builder().processingResponse(gitToHarnessProcessingResponseDTO).build());
  }
}
