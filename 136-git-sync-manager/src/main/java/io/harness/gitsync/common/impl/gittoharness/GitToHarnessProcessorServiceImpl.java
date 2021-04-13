package io.harness.gitsync.common.impl.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.EntityType;
import io.harness.Microservice;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.YamlFileDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.ChangeSet;
import io.harness.gitsync.ChangeSets;
import io.harness.gitsync.ChangeType;
import io.harness.gitsync.GitToHarnessInfo;
import io.harness.gitsync.GitToHarnessProcessRequest;
import io.harness.gitsync.GitToHarnessServiceGrpc;
import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.gitsync.common.helper.GitSyncConnectorHelper;
import io.harness.gitsync.common.helper.GitSyncUtils;
import io.harness.gitsync.common.service.GitEntityService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.gitsync.entity.HarnessShortListedBranch;
import io.harness.gitsync.service.HarnessShortlistedBranchService;
import io.harness.ng.core.event.EntityToEntityProtoHelper;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.service.ScmClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.StringValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class GitToHarnessProcessorServiceImpl implements GitToHarnessProcessorService {
  GitSyncConnectorHelper gitSyncConnectorHelper;
  ScmClient scmClient;
  Map<EntityType, Microservice> entityTypeMicroserviceMap;
  GitEntityService gitEntityService;
  Map<Microservice, GitToHarnessServiceGrpc.GitToHarnessServiceBlockingStub> gitToHarnessServiceGrpcClient;
  HarnessShortlistedBranchService harnessShortlistedBranchService;

  @Inject
  public GitToHarnessProcessorServiceImpl(GitSyncConnectorHelper gitSyncConnectorHelper, ScmClient scmClient,
      Map<EntityType, Microservice> entityTypeMicroserviceMap, GitEntityService gitEntityService,
      Map<Microservice, GitToHarnessServiceGrpc.GitToHarnessServiceBlockingStub> gitToHarnessServiceGrpcClient,
      HarnessShortlistedBranchService harnessShortlistedBranchService) {
    this.gitSyncConnectorHelper = gitSyncConnectorHelper;
    this.scmClient = scmClient;
    this.entityTypeMicroserviceMap = entityTypeMicroserviceMap;
    this.gitEntityService = gitEntityService;
    this.gitToHarnessServiceGrpcClient = gitToHarnessServiceGrpcClient;
    this.harnessShortlistedBranchService = harnessShortlistedBranchService;
  }

  @Override
  public void readFilesFromBranchAndProcess(YamlGitConfigDTO yamlGitConfig, String branch, String accountId,
      String defaultBranch, String filePathToBeExcluded) {
    ScmConnector connectorAssociatedWithGitSyncConfig =
        gitSyncConnectorHelper.getDecryptedConnector(yamlGitConfig, accountId);
    FileBatchContentResponse harnessFilesOfBranch =
        getFilesBelongingToThisBranch(connectorAssociatedWithGitSyncConfig, accountId, defaultBranch, yamlGitConfig);
    List<FileContent> filteredFileList = removeTheExcludedFile(harnessFilesOfBranch, filePathToBeExcluded);
    processTheChangesWeGotFromGit(filteredFileList, yamlGitConfig, branch, accountId);
  }

  private List<FileContent> removeTheExcludedFile(
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

  private void saveTheShortListedBranch(YamlGitConfigDTO yamlGitConfig, String branch) {
    final HarnessShortListedBranch shortlistedBranch = HarnessShortListedBranch.builder()
                                                           .accountIdentifier(yamlGitConfig.getAccountIdentifier())
                                                           .orgIdentifier(yamlGitConfig.getOrganizationIdentifier())
                                                           .projectIdentifier(yamlGitConfig.getProjectIdentifier())
                                                           .branchName(branch)
                                                           .yamlGitConfigId(yamlGitConfig.getIdentifier())
                                                           .build();
    harnessShortlistedBranchService.save(shortlistedBranch);
  }

  private FileBatchContentResponse getFilesBelongingToThisBranch(
      ScmConnector connector, String accountId, String branch, YamlGitConfigDTO yamlGitConfig) {
    // todo @deepak: Later on we won't list the files from the default branch
    List<String> filesList = getListOfFilesInTheDefaultBranch(yamlGitConfig);
    return scmClient.listFiles(connector, filesList, branch);
  }

  private List<String> getListOfFilesInTheDefaultBranch(YamlGitConfigDTO yamlGitConfig) {
    List<GitFileLocation> gitSyncEntityDTOS = gitEntityService.getDefaultEntities(yamlGitConfig.getAccountIdentifier(),
        yamlGitConfig.getOrganizationIdentifier(), yamlGitConfig.getProjectIdentifier(), yamlGitConfig.getIdentifier());
    return emptyIfNull(gitSyncEntityDTOS).stream().map(GitFileLocation::getEntityGitPath).collect(toList());
  }

  private void processTheChangesWeGotFromGit(
      List<FileContent> harnessFilesOfBranch, YamlGitConfigDTO gitSyncConfigDTO, String branch, String accountId) {
    List<ChangeSet> fileContentsList = convertFileListFromSCMToChangeSetList(harnessFilesOfBranch, accountId);
    Map<EntityType, List<ChangeSet>> mapOfEntityTypeAndContent = createMapOfEntityTypeAndFileContent(fileContentsList);
    Map<Microservice, List<ChangeSet>> groupedFilesByMicroservices =
        groupFilesByMicroservices(mapOfEntityTypeAndContent);
    for (Map.Entry<Microservice, List<ChangeSet>> entry : groupedFilesByMicroservices.entrySet()) {
      GitToHarnessServiceGrpc.GitToHarnessServiceBlockingStub gitToHarnessServiceBlockingStub =
          gitToHarnessServiceGrpcClient.get(entry.getKey());
      ChangeSets changeSets = ChangeSets.newBuilder().addAllChangeSet(entry.getValue()).setAccountId(accountId).build();
      GitToHarnessInfo.Builder gitToHarnessInfo =
          GitToHarnessInfo.newBuilder()
              .setAccountIdentifier(accountId)
              .setYamlGitConfigProjectIdentifier(gitSyncConfigDTO.getProjectIdentifier())
              .setYamlGitConfigId(gitSyncConfigDTO.getIdentifier())
              .setBranch(branch);
      if (isNotBlank(gitSyncConfigDTO.getOrganizationIdentifier())) {
        gitToHarnessInfo.setYamlGitConfigOrgIdentifier(gitSyncConfigDTO.getOrganizationIdentifier());
      }
      if (isNotBlank(gitSyncConfigDTO.getProjectIdentifier())) {
        gitToHarnessInfo.setYamlGitConfigOrgIdentifier(gitSyncConfigDTO.getProjectIdentifier());
      }
      GitToHarnessProcessRequest gitToHarnessProcessRequest = GitToHarnessProcessRequest.newBuilder()
                                                                  .setChangeSets(changeSets)
                                                                  .setGitToHarnessBranchInfo(gitToHarnessInfo)
                                                                  .build();
      gitToHarnessServiceBlockingStub.process(gitToHarnessProcessRequest);
    }
  }

  private List<ChangeSet> convertFileListFromSCMToChangeSetList(List<FileContent> fileContentsList, String accountId) {
    return emptyIfNull(fileContentsList)
        .stream()
        .map(fileContent -> mapToChangeSet(fileContent, accountId))
        .collect(toList());
  }

  private ChangeSet mapToChangeSet(FileContent fileContent, String accountId) {
    // todo @deepak: Set the correct values here
    EntityType entityType = GitSyncUtils.getEntityTypeFromYaml(fileContent.getContent());
    ChangeSet.Builder builder = ChangeSet.newBuilder()
                                    .setAccountId(accountId)
                                    .setChangeType(ChangeType.ADD)
                                    .setCommitId(StringValue.of("dummy"))
                                    .setEntityType(EntityToEntityProtoHelper.getEntityTypeFromProto(entityType))
                                    .setId("dummy")
                                    .setObjectId(StringValue.of(fileContent.getBlobId()))
                                    .setYaml(fileContent.getContent())
                                    .setFilePath(fileContent.getPath());
    if (isNotBlank(fileContent.getBlobId())) {
      builder.setObjectId(StringValue.of(fileContent.getBlobId()));
    }
    return builder.build();
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

  private List<YamlFileDetails> convertToYamlFileDetailsList(List<FileContent> fileContents, EntityType entityType) {
    List<YamlFileDetails> yamlFileDetailsList = new ArrayList<>();
    if (isEmpty(fileContents)) {
      return yamlFileDetailsList;
    }
    return fileContents.stream()
        .map(fileContent -> convertToYamlFileDetails(fileContent, entityType))
        .collect(toList());
  }

  private YamlFileDetails convertToYamlFileDetails(FileContent fileContent, EntityType entityType) {
    return YamlFileDetails.builder().fileContent(fileContent).entityType(entityType).build();
  }

  private Map<EntityType, List<ChangeSet>> createMapOfEntityTypeAndFileContent(List<ChangeSet> fileContentsList) {
    Map<EntityType, List<ChangeSet>> mapOfEntityTypeAndContent = new HashMap<>();
    for (ChangeSet fileContent : fileContentsList) {
      final String yamlOfFile = fileContent.getYaml();
      EntityType entityTypeFromYaml = GitSyncUtils.getEntityTypeFromYaml(yamlOfFile);
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
}
