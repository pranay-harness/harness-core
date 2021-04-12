package io.harness.gitsync.common.impl.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toList;

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
import java.util.Collections;
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
  public void readFilesFromBranchAndProcess(YamlGitConfigDTO yamlGitConfig, String branch, String accountId) {
    ScmConnector connectorAssociatedWithGitSyncConfig =
        gitSyncConnectorHelper.getDecryptedConnector(yamlGitConfig, accountId);
    FileBatchContentResponse harnessFilesOfBranch =
        getFilesBelongingToThisBranch(connectorAssociatedWithGitSyncConfig, accountId, branch, yamlGitConfig);
    processTheChangesWeGotFromGit(harnessFilesOfBranch, yamlGitConfig, branch, accountId);
    saveTheShortListedBranch(yamlGitConfig, branch);
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

  private void processTheChangesWeGotFromGit(FileBatchContentResponse harnessFilesOfBranch,
      YamlGitConfigDTO gitSyncConfigDTO, String branch, String accountId) {
    List<ChangeSet> fileContentsList = convertFileListFromSCMToChangeSetList(harnessFilesOfBranch, accountId);
    Map<EntityType, List<ChangeSet>> mapOfEntityTypeAndContent = createMapOfEntityTypeAndFileContent(fileContentsList);
    Map<Microservice, List<ChangeSet>> groupedFilesByMicroservices =
        groupFilesByMicroservices(mapOfEntityTypeAndContent);
    for (Map.Entry<Microservice, List<ChangeSet>> entry : groupedFilesByMicroservices.entrySet()) {
      GitToHarnessServiceGrpc.GitToHarnessServiceBlockingStub gitToHarnessServiceBlockingStub =
          gitToHarnessServiceGrpcClient.get(entry.getKey());
      ChangeSets changeSets = ChangeSets.newBuilder().addAllChangeSet(entry.getValue()).setAccountId(accountId).build();
      GitToHarnessInfo gitToHarnessInfo =
          GitToHarnessInfo.newBuilder().setYamlGitConfigId(gitSyncConfigDTO.get).setBranch().build();
      gitToHarnessServiceBlockingStub.process(changeSets);
    }
  }

  private List<ChangeSet> convertFileListFromSCMToChangeSetList(
      FileBatchContentResponse harnessFilesOfBranch, String accountId) {
    List<FileContent> fileContentsList =
        harnessFilesOfBranch == null ? Collections.emptyList() : harnessFilesOfBranch.getFileContentsList();
    return emptyIfNull(fileContentsList)
        .stream()
        .map(fileContent -> mapToChangeSet(fileContent, accountId))
        .collect(toList());
  }

  private ChangeSet mapToChangeSet(FileContent fileContent, String accountId) {
    // todo @deepak: Set the correct values here
    EntityType entityType = GitSyncUtils.getEntityTypeFromYaml(fileContent.getContent());
    return ChangeSet.newBuilder()
        .setAccountId(accountId)
        .setChangeType(ChangeType.ADD)
        .setCommitId(StringValue.of("dummy"))
        .setEntityType(EntityToEntityProtoHelper.getEntityTypeFromProto(entityType))
        .setId("dummy")
        .setObjectId(StringValue.of("dummy"))
        .setYaml(fileContent.getContent())
        .build();
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
