package io.harness.gitsync.common.impl.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.DONE;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.ERROR;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus.IN_PROGRESS;
import static io.harness.gitsync.common.beans.GitToHarnessProcessingStepType.PROCESS_FILES_IN_MSVS;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProcessingResponse;
import io.harness.gitsync.common.beans.GitToHarnessProcessingResponseDTO;
import io.harness.gitsync.common.beans.GitToHarnessProcessingStepStatus;
import io.harness.gitsync.common.beans.MsvcProcessingFailureStage;
import io.harness.gitsync.common.helper.GitChangeSetMapper;
import io.harness.gitsync.common.helper.GitSyncUtils;
import io.harness.gitsync.common.service.GitToHarnessProgressService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.gitsync.helpers.ProcessingResponseMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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

  @Override
  public List<GitToHarnessProcessingResponse> processFiles(String accountId,
      List<GitToHarnessFileProcessingRequest> fileContentsList, String branchName, YamlGitConfigDTO yamlGitConfigDTO,
      String gitToHarnessProgressRecordId) {
    List<ChangeSet> changeSets = GitChangeSetMapper.toChangeSetList(fileContentsList, accountId);
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
          GitToHarnessInfo.newBuilder()
              .setRepoUrl(yamlGitConfigDTO.getRepo())
              .setYamlGitConfigProjectIdentifier(yamlGitConfigDTO.getProjectIdentifier())
              .setYamlGitConfigId(yamlGitConfigDTO.getIdentifier())
              .setBranch(branchName);
      if (isNotBlank(yamlGitConfigDTO.getOrganizationIdentifier())) {
        gitToHarnessInfo.setYamlGitConfigOrgIdentifier(yamlGitConfigDTO.getOrganizationIdentifier());
      }
      if (isNotBlank(yamlGitConfigDTO.getProjectIdentifier())) {
        gitToHarnessInfo.setYamlGitConfigProjectIdentifier(yamlGitConfigDTO.getProjectIdentifier());
      }
      GitToHarnessProcessRequest gitToHarnessProcessRequest = GitToHarnessProcessRequest.newBuilder()
                                                                  .setChangeSets(changeSetForThisMicroservice)
                                                                  .setGitToHarnessBranchInfo(gitToHarnessInfo)
                                                                  .setAccountId(accountId)
                                                                  .build();
      log.info("Sending to microservice {}", entry.getKey());
      ProcessingResponse processingResponse = gitToHarnessServiceBlockingStub.process(gitToHarnessProcessRequest);
      log.info("Got the processing response for the microservice {}, response {}", entry.getKey(), processingResponse);
      GitToHarnessProcessingResponse gitToHarnessResponse =
          GitToHarnessProcessingResponse.builder()
              .processingResponse(ProcessingResponseMapper.toProcessingResponseDTO(processingResponse))
              .microservice(microservice)
              .build();
      gitToHarnessProcessingResponses.add(gitToHarnessResponse);
      gitToHarnessProgressService.updateProgressWithProcessingResponse(
          gitToHarnessProgressRecordId, gitToHarnessResponse);
      log.info("Completed for microservice {}", entry.getKey());
    }
    updateTheGitToHarnessStatus(gitToHarnessProgressRecordId, gitToHarnessProcessingResponses);
    return gitToHarnessProcessingResponses;
  }

  private void markSkippedFiles(List<ChangeSet> skippedChangeSet) {
    // @todo deepak mark skipped in progress.
  }

  private void updateTheGitToHarnessStatus(
      String gitToHarnessProgressRecordId, List<GitToHarnessProcessingResponse> gitToHarnessProcessingResponses) {
    GitToHarnessProcessingStepStatus status = getStatus(gitToHarnessProcessingResponses);
    gitToHarnessProgressService.updateStatus(gitToHarnessProgressRecordId, status);
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
}
