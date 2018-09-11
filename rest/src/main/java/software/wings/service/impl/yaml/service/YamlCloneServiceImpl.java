package software.wings.service.impl.yaml.service;

import static java.util.Arrays.asList;
import static software.wings.beans.yaml.YamlConstants.APPLICATIONS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PIPELINES_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PROVISIONERS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;
import static software.wings.beans.yaml.YamlConstants.WORKFLOWS_FOLDER;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Pipeline;
import software.wings.beans.ResponseMessage;
import software.wings.beans.RestResponse;
import software.wings.beans.RestResponse.Builder;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.service.intfc.yaml.clone.YamlCloneService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.yaml.YamlVersion.Type;
import software.wings.yaml.directory.AppLevelYamlNode;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.FolderNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class YamlCloneServiceImpl implements YamlCloneService {
  public static final String YAML_EXTENSION = ".yaml";
  @Inject private YamlResourceService yamlResourceService;
  @Inject private YamlService yamlService;
  @Inject private AppService appService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Inject private InfrastructureProvisionerService provisionerService;
  @Inject private YamlGitService yamlGitSyncService;
  @Inject private YamlDirectoryService yamlDirectoryService;

  public RestResponse cloneEntityUsingYaml(
      String accountId, String appId, boolean includeFiles, String entityType, String entiytId, String newEntityName) {
    try {
      DirectoryPath directoryPath = new DirectoryPath(SETUP_FOLDER);
      directoryPath.add(APPLICATIONS_FOLDER);
      Application application = appService.get(appId);
      directoryPath.add(application.getName());

      if (EntityType.WORKFLOW.equals(EntityType.valueOf(entityType))) {
        return cloneWorkflowUsingYaml(accountId, appId, includeFiles, entiytId, newEntityName, directoryPath);
      } else if (EntityType.PIPELINE.equals(EntityType.valueOf(entityType))) {
        return clonePipelineUsingYaml(accountId, appId, includeFiles, entiytId, newEntityName, directoryPath);
      } else if (EntityType.PROVISIONER.equals(EntityType.valueOf(entityType))) {
        return cloneProvisionerUsingYaml(accountId, appId, includeFiles, entiytId, newEntityName, directoryPath);
      }
    } catch (Exception e) {
      return Builder.aRestResponse()
          .withResponseMessages(
              asList(new ResponseMessage[] {ResponseMessage.builder().code(ErrorCode.DEFAULT_ERROR_CODE).build()}))
          .build();
    }

    return Builder.aRestResponse().build();
  }

  private RestResponse cloneWorkflowUsingYaml(String accountId, String appId, boolean includeFiles, String entiytId,
      String newEntityName, DirectoryPath directoryPath) {
    // check workflow with newName does not exist
    if (workflowService.readWorkflowByName(appId, newEntityName) != null) {
      return getRestResponseForFailure("Workflow Already exists with name: " + newEntityName);
    }

    // check if workflow to be cloned from exists
    Workflow workflow = workflowService.readWorkflow(appId, entiytId);
    if (workflow == null) {
      return getRestResponseForFailure("Workflow to be cloned from does not exists: " + entiytId);
    }

    FolderNode workflowsFolder =
        new FolderNode(accountId, WORKFLOWS_FOLDER, Workflow.class, directoryPath, appId, yamlGitSyncService);

    DirectoryPath workflowPath = directoryPath.clone();
    String workflowYamlFileName = newEntityName + YAML_EXTENSION;
    workflowsFolder.addChild(
        new AppLevelYamlNode(accountId, workflow.getUuid(), workflow.getAppId(), workflowYamlFileName, Workflow.class,
            workflowPath.add(workflowYamlFileName), yamlGitSyncService, Type.WORKFLOW));

    return cloneAppLevelEntity(accountId, includeFiles, workflowsFolder);
  }

  private RestResponse clonePipelineUsingYaml(String accountId, String appId, boolean includeFiles, String entiytId,
      String newEntityName, DirectoryPath directoryPath) {
    // check Pipeline with newName does not exist
    if (pipelineService.getPipelineByName(appId, newEntityName) != null) {
      return getRestResponseForFailure("Pipeline Already exists with name: " + newEntityName);
    }

    // check if Pipeline to be cloned from exists
    Pipeline pipeline = pipelineService.readPipeline(appId, entiytId, false);
    if (pipeline == null) {
      return getRestResponseForFailure("Pipeline to be cloned from does not exists: " + entiytId);
    }

    FolderNode pipelinesFolder =
        new FolderNode(accountId, PIPELINES_FOLDER, Pipeline.class, directoryPath, appId, yamlGitSyncService);

    DirectoryPath pipelinePath = directoryPath.clone();
    String pipelineYamlFileName = newEntityName + YAML_EXTENSION;
    pipelinesFolder.addChild(
        new AppLevelYamlNode(accountId, pipeline.getUuid(), pipeline.getAppId(), pipelineYamlFileName, Pipeline.class,
            pipelinePath.add(pipelineYamlFileName), yamlGitSyncService, Type.PIPELINE));

    return cloneAppLevelEntity(accountId, includeFiles, pipelinesFolder);
  }

  private RestResponse cloneProvisionerUsingYaml(String accountId, String appId, boolean includeFiles, String entiytId,
      String newEntityName, DirectoryPath directoryPath) {
    // check Provisioner with newName does not exist
    if (provisionerService.getByName(appId, newEntityName) != null) {
      return getRestResponseForFailure("Proviosioner Already exists with name: " + newEntityName);
    }

    // check if Provisioner to be cloned from exists
    InfrastructureProvisioner infrastructureProvisioner = provisionerService.get(appId, entiytId);
    if (infrastructureProvisioner == null) {
      return getRestResponseForFailure("InfrastructureProvisioner to be cloned from does not exists: " + entiytId);
    }

    FolderNode provisionersFolder = new FolderNode(
        accountId, PROVISIONERS_FOLDER, InfrastructureProvisioner.class, directoryPath, appId, yamlGitSyncService);

    DirectoryPath provisionerPath = directoryPath.clone();
    String provisionerYamlFileName = newEntityName + YAML_EXTENSION;
    provisionersFolder.addChild(new AppLevelYamlNode(accountId, infrastructureProvisioner.getUuid(),
        infrastructureProvisioner.getAppId(), provisionerYamlFileName, InfrastructureProvisioner.class,
        provisionerPath.add(provisionerYamlFileName), yamlGitSyncService, Type.PROVISIONER));

    return cloneAppLevelEntity(accountId, includeFiles, provisionersFolder);
  }

  /**
   * Use this API for cloning App Level Entities like,
   * Workflow, Pipeline, Provisioner, Service etc.
   * @param accountId
   * @param includeFiles
   * @param folderNode
   * @return
   */
  private RestResponse cloneAppLevelEntity(String accountId, boolean includeFiles, FolderNode folderNode) {
    List<GitFileChange> gitFileChanges = new ArrayList<>();
    Optional<List<String>> errorMessages = Optional.of(new ArrayList<String>());
    try {
      traverseDirectory(
          gitFileChanges, accountId, folderNode, folderNode.getDirectoryPath().getPath(), includeFiles, errorMessages);

      // If yamlConversion fails for any files, propogate that message to UI with failure
      if (EmptyPredicate.isNotEmpty(errorMessages.get())) {
        StringBuilder builder = new StringBuilder("Following Errors Happened\n");
        errorMessages.get().forEach(message -> builder.append(message).append(", "));
        return getRestResponseForFailure(builder.toString());
      }

      // yamlification was successful, process generated gitChanges
      List<Change> changeList = new ArrayList<>();
      for (GitFileChange gitFileChange : gitFileChanges) {
        changeList.add(Change.Builder.aFileChange()
                           .withAccountId(gitFileChange.getAccountId())
                           .withFileContent(gitFileChange.getFileContent())
                           .withChangeType(ChangeType.ADD)
                           .withFilePath(gitFileChange.getFilePath())
                           .build());
      }

      yamlService.processChangeSet(changeList, false);
    } catch (Exception e) {
      return getRestResponseForFailure("Failed to Clone Entity with Exception: " + e.getMessage());
    }

    return Builder.aRestResponse().build();
  }

  /**
   * Recursiverly iterate over folder node to generate yaml strcture for entity
   * @param gitFileChanges
   * @param accountId
   * @param fn
   * @param path
   * @param includeFiles
   * @param errorMessages
   */
  private void traverseDirectory(List<GitFileChange> gitFileChanges, String accountId, FolderNode fn, String path,
      boolean includeFiles, Optional<List<String>> errorMessages) {
    path = path + "/" + fn.getName();

    for (DirectoryNode dn : fn.getChildren()) {
      yamlDirectoryService.getGitFileChange(
          dn, path, accountId, includeFiles, gitFileChanges, false, errorMessages, false);

      if (dn instanceof FolderNode) {
        traverseDirectory(gitFileChanges, accountId, (FolderNode) dn, path, includeFiles, errorMessages);
      }
    }
  }

  private RestResponse getRestResponseForFailure(String message) {
    return Builder.aRestResponse()
        .withResponseMessages(Lists.newArrayList(ResponseMessage.builder().message(message).level(Level.ERROR).build()))
        .build();
  }
}
