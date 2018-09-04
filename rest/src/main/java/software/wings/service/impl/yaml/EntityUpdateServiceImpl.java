package software.wings.service.impl.yaml;

import static java.util.Arrays.asList;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.GitFileChange.Builder;
import software.wings.beans.yaml.YamlConstants;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.yaml.AppYamlResourceService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity Update Service Implementation.
 *
 * @author bsollish
 */
@Singleton
public class EntityUpdateServiceImpl implements EntityUpdateService {
  private static final Logger logger = LoggerFactory.getLogger(EntityUpdateServiceImpl.class);

  @Inject private AppYamlResourceService appYamlResourceService;
  @Inject private YamlResourceService yamlResourceService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private AppService appService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;

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
        app.getAccountId(), yamlDirectoryService.getRootPathByApp(app), YamlConstants.INDEX, yaml, changeType, true);
  }

  @Override
  public GitFileChange getServiceGitSyncFile(String accountId, Service service, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getService(service.getAppId(), service.getUuid()).getResource().getYaml();
    }
    return createGitFileChange(
        accountId, yamlDirectoryService.getRootPathByService(service), YamlConstants.INDEX, yaml, changeType, true);
  }

  @Override
  public GitFileChange getDefaultVarGitSyncFile(String accountId, String appId, ChangeType changeType) {
    // if (ChangeType.DELETE.equals(changeType)) {
    //   TODO: handle this
    //}
    String yaml = yamlResourceService.getDefaultVariables(accountId, appId).getResource().getYaml();

    if (GLOBAL_APP_ID.equals(appId)) {
      return createGitFileChange(
          accountId, yamlDirectoryService.getRootPath(), YamlConstants.DEFAULTS, yaml, changeType, false);
    } else {
      Application app = appService.get(appId);
      return createGitFileChange(
          accountId, yamlDirectoryService.getRootPathByApp(app), YamlConstants.DEFAULTS, yaml, changeType, false);
    }
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
  public GitFileChange getContainerTaskGitSyncFile(
      String accountId, Service service, ContainerTask containerTask, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getContainerTask(accountId, containerTask.getAppId(), containerTask.getUuid())
                 .getResource()
                 .getYaml();
    }

    String name;
    if (DeploymentType.ECS.name().equals(containerTask.getDeploymentType())) {
      name = YamlConstants.ECS_CONTAINER_TASK_YAML_FILE_NAME;
    } else if (DeploymentType.KUBERNETES.name().equals(containerTask.getDeploymentType())) {
      name = YamlConstants.KUBERNETES_CONTAINER_TASK_YAML_FILE_NAME;
    } else {
      throw new WingsException("UnSupported ContainerTask: " + containerTask.getDeploymentType());
    }

    return createGitFileChange(accountId, yamlDirectoryService.getRootPathByContainerTask(service, containerTask), name,
        yaml, changeType, false);
  }

  @Override
  public GitFileChange getHelmChartGitSyncFile(
      String accountId, Service service, HelmChartSpecification helmChartSpecification, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml =
          yamlResourceService
              .getHelmChartSpecification(accountId, helmChartSpecification.getAppId(), helmChartSpecification.getUuid())
              .getResource()
              .getYaml();
    }

    String name = YamlConstants.HELM_CHART_YAML_FILE_NAME;

    return createGitFileChange(accountId,
        yamlDirectoryService.getRootPathByHelmChartSpecification(service, helmChartSpecification), name, yaml,
        changeType, false);
  }

  @Override
  public GitFileChange getPcfServiceSpecification(
      String accountId, Service service, PcfServiceSpecification pcfServiceSpecification, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService
                 .getPcfServiceSpecification(
                     accountId, pcfServiceSpecification.getAppId(), pcfServiceSpecification.getUuid())
                 .getResource()
                 .getYaml();
    }

    String name = YamlConstants.PCF_MANIFEST_YAML_FILE_NAME;

    return createGitFileChange(accountId,
        yamlDirectoryService.getRootPathByPcfServiceSpecification(service, pcfServiceSpecification), name, yaml,
        changeType, false);
  }

  @Override
  public GitFileChange getLamdbaSpecGitSyncFile(
      String accountId, Service service, LambdaSpecification lambdaSpec, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getLambdaSpec(accountId, lambdaSpec.getAppId(), lambdaSpec.getUuid())
                 .getResource()
                 .getYaml();
    }
    return createGitFileChange(accountId, yamlDirectoryService.getRootPathByLambdaSpec(service, lambdaSpec),
        YamlConstants.LAMBDA_SPEC_YAML_FILE_NAME, yaml, changeType, false);
  }

  @Override
  public GitFileChange getUserDataGitSyncFile(
      String accountId, Service service, UserDataSpecification userDataSpec, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getUserDataSpec(accountId, userDataSpec.getAppId(), userDataSpec.getUuid())
                 .getResource()
                 .getYaml();
    }
    return createGitFileChange(accountId, yamlDirectoryService.getRootPathByUserDataSpec(service, userDataSpec),
        YamlConstants.USER_DATA_SPEC_YAML_FILE_NAME, yaml, changeType, false);
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

    String fileName = Util.normalize(configFile.getRelativeFilePath());
    GitFileChange gitFileChange = createGitFileChange(
        accountId, yamlDirectoryService.getRootPathByConfigFile(service), fileName, yaml, changeType, false);
    if (fileContent != null) {
      GitFileChange configFileChange = createConfigFileChange(
          accountId, yamlDirectoryService.getRootPathByConfigFile(service), fileName, fileContent, changeType);
      return asList(gitFileChange, configFileChange);
    } else {
      return asList(gitFileChange);
    }
  }

  @Override
  public List<GitFileChange> getConfigFileOverrideGitSyncFileSet(
      String accountId, Environment environment, ConfigFile configFile, ChangeType changeType, String fileContent) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getConfigFileOverrideYaml(accountId, configFile.getAppId(), configFile)
                 .getResource()
                 .getYaml();
    }

    String fileName = Util.normalize(configFile.getRelativeFilePath());
    GitFileChange gitFileChange = createGitFileChange(accountId,
        yamlDirectoryService.getRootPathByConfigFileOverride(environment), fileName, yaml, changeType, false);
    if (fileContent != null) {
      GitFileChange configFileChange = createConfigFileChange(accountId,
          yamlDirectoryService.getRootPathByConfigFileOverride(environment), fileName, fileContent, changeType);
      return asList(gitFileChange, configFileChange);
    } else {
      return asList(gitFileChange);
    }
  }

  public List<GitFileChange> getEnvironmentGitSyncFile(
      String accountId, Environment environment, ChangeType changeType) {
    List<GitFileChange> gitFileChanges = new ArrayList<>();

    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getEnvironment(environment.getAppId(), environment.getUuid()).getResource().getYaml();
    }

    gitFileChanges.add(createGitFileChange(accountId, yamlDirectoryService.getRootPathByEnvironment(environment),
        YamlConstants.INDEX, yaml, changeType, true));
    return gitFileChanges;
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
  public GitFileChange getInfraProvisionerGitSyncFile(
      String accountId, InfrastructureProvisioner provisioner, ChangeType changeType) {
    String yaml = null;
    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.getProvisioner(provisioner.getAppId(), provisioner.getUuid()).getResource().getYaml();
    }
    return createGitFileChange(accountId, yamlDirectoryService.getRootPathByInfraProvisioner(provisioner),
        provisioner.getName(), yaml, changeType, false);
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

  @Override
  public <T> List<GitFileChange> obtainEntityGitSyncFileChangeSet(String accountId, T entity, ChangeType changeType) {
    List<GitFileChange> gitFileChanges = new ArrayList<>();
    String yaml = null;

    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.obtainEntityYamlVersion(accountId, entity).getResource().getYaml();
    }

    String yamlFileName = yamlHandlerFactory.obtainYamlFileName(entity);
    boolean isNonLeafEntity = yamlHandlerFactory.isNonLeafEntity(entity);

    gitFileChanges.add(createGitFileChange(
        accountId, yamlDirectoryService.obtainEntityRootPath(entity), yamlFileName, yaml, changeType, isNonLeafEntity));

    return gitFileChanges;
  }
}