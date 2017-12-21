package software.wings.service.impl.yaml;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.GitFileChange.Builder;
import software.wings.beans.yaml.YamlConstants;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Arrays;
import java.util.List;

/**
 * Entity Update Service Implementation.
 *
 * @author bsollish
 */
public class EntityUpdateServiceImpl implements EntityUpdateService {
  private static final Logger logger = LoggerFactory.getLogger(EntityUpdateServiceImpl.class);

  @Inject private AppYamlResourceService appYamlResourceService;
  @Inject private YamlResourceService yamlResourceService;
  @Inject private YamlDirectoryService yamlDirectoryService;

  private GitFileChange createGitFileChange(
      String accountId, String path, String name, String yamlContent, ChangeType changeType, boolean isDirectory) {
    return Builder.aGitFileChange()
        .withAccountId(accountId)
        .withChangeType(changeType)
        .withFileContent(yamlContent)
        .withFilePath(changeType.equals(ChangeType.DELETE) && isDirectory
                ? path
                : path + "/" + name + YamlConstants.YAML_EXTENSION)
        .build();
  }

  private GitFileChange createConfigFileChange(
      String accountId, String path, String fileName, String content, ChangeType changeType) {
    return Builder.aGitFileChange()
        .withAccountId(accountId)
        .withChangeType(changeType)
        .withFileContent(content)
        .withFilePath(path + "/" + fileName)
        .build();
  }

  @Override
  public GitFileChange getAppGitSyncFile(Application app, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = appYamlResourceService.getApp(app.getUuid()).getResource().getYaml();
    }
    return createGitFileChange(
        app.getAccountId(), yamlDirectoryService.getRootPathByApp(app), "Entity", yaml, changeType, true);
  }

  @Override
  public GitFileChange getServiceGitSyncFile(String accountId, Service service, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getService(service.getAppId(), service.getUuid()).getResource().getYaml();
    }
    return createGitFileChange(
        accountId, yamlDirectoryService.getRootPathByService(service), "Entity", yaml, changeType, true);
  }

  @Override
  public GitFileChange getCommandGitSyncFile(
      String accountId, Service service, ServiceCommand serviceCommand, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getServiceCommand(serviceCommand.getAppId(), serviceCommand.getUuid())
                 .getResource()
                 .getYaml();
    }
    return createGitFileChange(accountId, yamlDirectoryService.getRootPathByServiceCommand(service, serviceCommand),
        serviceCommand.getName(), yaml, changeType, false);
  }

  @Override
  public List<GitFileChange> getConfigFileGitSyncFileSet(
      String accountId, Service service, ConfigFile configFile, ChangeType changeType, String fileContent) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getConfigFileYaml(accountId, service.getAppId(), configFile.getUuid())
                 .getResource()
                 .getYaml();
    }

    GitFileChange gitFileChange = createGitFileChange(accountId, yamlDirectoryService.getRootPathByConfigFile(service),
        configFile.getRelativeFilePath(), yaml, changeType, false);
    if (fileContent != null) {
      GitFileChange configFileChange =
          createConfigFileChange(accountId, yamlDirectoryService.getRootPathByConfigFile(service),
              configFile.getRelativeFilePath(), fileContent, changeType);
      return Arrays.asList(gitFileChange, configFileChange);
    } else {
      return Arrays.asList(gitFileChange);
    }
  }

  public GitFileChange getEnvironmentGitSyncFile(String accountId, Environment environment, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getEnvironment(environment.getAppId(), environment.getUuid()).getResource().getYaml();
    }
    return createGitFileChange(
        accountId, yamlDirectoryService.getRootPathByEnvironment(environment), "Entity", yaml, changeType, true);
  }

  @Override
  public GitFileChange getInfraMappingGitSyncFile(
      String accountId, InfrastructureMapping infraMapping, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getInfraMapping(accountId, infraMapping.getAppId(), infraMapping.getUuid())
                 .getResource()
                 .getYaml();
    }
    return createGitFileChange(accountId, yamlDirectoryService.getRootPathByInfraMapping(infraMapping),
        infraMapping.getName(), yaml, changeType, false);
  }

  @Override
  public GitFileChange getWorkflowGitSyncFile(String accountId, Workflow workflow, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getWorkflow(workflow.getAppId(), workflow.getUuid()).getResource().getYaml();
    }
    return createGitFileChange(
        accountId, yamlDirectoryService.getRootPathByWorkflow(workflow), workflow.getName(), yaml, changeType, false);
  }

  public GitFileChange getPipelineGitSyncFile(String accountId, Pipeline pipeline, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getPipeline(pipeline.getAppId(), pipeline.getUuid()).getResource().getYaml();
    }
    return createGitFileChange(
        accountId, yamlDirectoryService.getRootPathByPipeline(pipeline), pipeline.getName(), yaml, changeType, false);
  }

  public GitFileChange getArtifactStreamGitSyncFile(
      String accountId, ArtifactStream artifactStream, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml =
          yamlResourceService.getTrigger(artifactStream.getAppId(), artifactStream.getUuid()).getResource().getYaml();
    }
    return createGitFileChange(accountId, yamlDirectoryService.getRootPathByArtifactStream(artifactStream),
        artifactStream.getName(), yaml, changeType, false);
  }

  public GitFileChange getSettingAttributeGitSyncFile(
      String accountId, SettingAttribute settingAttribute, ChangeType changeType) {
    SettingVariableTypes settingVariableType = SettingVariableTypes.valueOf(settingAttribute.getValue().getType());
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getSettingAttribute(accountId, settingAttribute.getUuid()).getResource().getYaml();
    }
    return createGitFileChange(accountId,
        yamlDirectoryService.getRootPathBySettingAttribute(settingAttribute, settingVariableType),
        settingAttribute.getName(), yaml, changeType, false);
  }
}
