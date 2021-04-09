package io.harness.gitsync.common.impl.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.EntityType;
import io.harness.Microservice;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.YamlFileDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.common.helper.GitSyncConnectorHelper;
import io.harness.gitsync.common.helper.GitSyncUtils;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.service.ScmClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class GitToHarnessProcessorServiceImpl implements GitToHarnessProcessorService {
  GitSyncConnectorHelper gitSyncConnectorHelper;
  ScmClient scmClient;
  Map<EntityType, Microservice> entityTypeMicroserviceMap;

  @Override
  public void readFilesFromBranchAndProcess(YamlGitConfigDTO gitSyncConfigDTO, String branch, String accountId) {
    ScmConnector connectorAssociatedWithGitSyncConfig =
        gitSyncConnectorHelper.getConnectorAssociatedWithGitSyncConfig(gitSyncConfigDTO, accountId);
    FileBatchContentResponse harnessFilesOfBranch = getFilesBelongingToThisBranch(gitSyncConfigDTO, accountId, branch);
    processTheChangesWeGotFromGit(harnessFilesOfBranch, gitSyncConfigDTO, branch, accountId);
  }

  private FileBatchContentResponse getFilesBelongingToThisBranch(
      YamlGitConfigDTO gitSyncConfigDTO, String accountId, String branch) {
    List<String> filesList = getListOfFilesInTheDefaultBranch();
    return scmClient;
  }

  private void processTheChangesWeGotFromGit(FileBatchContentResponse harnessFilesOfBranch,
      YamlGitConfigDTO gitSyncConfigDTO, String branch, String accountId) {
    List<FileContent> fileContentsList = harnessFilesOfBranch.getFileContentsList();
    Map<EntityType, List<FileContent>> mapOfEntityTypeAndContent =
        createMapOfEntityTypeAndFileContent(fileContentsList);
    Map<Microservice, List<YamlFileDetails>> groupedFilesByMicroservices =
        groupFilesByMicroservices(mapOfEntityTypeAndContent);
    // todo @deepak : Add the logic to call each microservice and get the files processed
  }

  private Map<Microservice, List<YamlFileDetails>> groupFilesByMicroservices(
      Map<EntityType, List<FileContent>> mapOfEntityTypeAndContent) {
    Map<Microservice, List<YamlFileDetails>> groupedFilesByMicroservices = new HashMap<>();
    if (isEmpty(mapOfEntityTypeAndContent)) {
      return groupedFilesByMicroservices;
    }
    for (Map.Entry<EntityType, List<FileContent>> entry : mapOfEntityTypeAndContent.entrySet()) {
      final EntityType entityType = entry.getKey();
      final List<FileContent> fileContents = entry.getValue();
      Microservice microservice = entityTypeMicroserviceMap.get(entityType);
      List<YamlFileDetails> yamlFileDetails = convertToYamlFileDetailsList(fileContents, entityType);
      if (groupedFilesByMicroservices.containsKey(microservice)) {
        groupedFilesByMicroservices.get(microservice).addAll(yamlFileDetails);
      } else {
        groupedFilesByMicroservices.put(microservice, yamlFileDetails);
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
        .collect(Collectors.toList());
  }

  private YamlFileDetails convertToYamlFileDetails(FileContent fileContent, EntityType entityType) {
    return YamlFileDetails.builder().fileContent(fileContent).entityType(entityType).build();
  }

  private Map<EntityType, List<FileContent>> createMapOfEntityTypeAndFileContent(List<FileContent> fileContentsList) {
    Map<EntityType, List<FileContent>> mapOfEntityTypeAndContent = new HashMap<>();
    for (FileContent fileContent : fileContentsList) {
      final String yamlOfFile = fileContent.getContent();
      EntityType entityTypeFromYaml = GitSyncUtils.getEntityTypeFromYaml(yamlOfFile);
      if (mapOfEntityTypeAndContent.containsKey(entityTypeFromYaml)) {
        mapOfEntityTypeAndContent.get(entityTypeFromYaml).add(fileContent);
      } else {
        List<FileContent> newFileContentList = new ArrayList<>();
        newFileContentList.add(fileContent);
        mapOfEntityTypeAndContent.put(entityTypeFromYaml, newFileContentList);
      }
    }
    return mapOfEntityTypeAndContent;
  }
}
