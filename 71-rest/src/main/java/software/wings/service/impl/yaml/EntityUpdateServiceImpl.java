package software.wings.service.impl.yaml;

import static com.google.common.base.Charsets.UTF_8;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.yaml.YamlConstants.DEFAULTS_YAML;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.GitFileChange.Builder;
import software.wings.beans.yaml.YamlConstants;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Util;
import software.wings.utils.Validator;

import java.io.ByteArrayOutputStream;
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

  @Inject private YamlResourceService yamlResourceService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private AppService appService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private FileService fileService;

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
  public List<GitFileChange> getDefaultVarGitSyncFile(String accountId, String appId, ChangeType changeType) {
    // if (ChangeType.DELETE.equals(changeType)) {
    //   TODO: handle this
    //}
    String yaml = yamlResourceService.getDefaultVariables(accountId, appId).getResource().getYaml();

    if (GLOBAL_APP_ID.equals(appId)) {
      return Lists.newArrayList(createGitFileChange(
          accountId, yamlDirectoryService.getRootPath(), YamlConstants.DEFAULTS, yaml, changeType, false));
    } else {
      Application app = appService.get(appId);
      return Lists.newArrayList(createGitFileChange(
          accountId, yamlDirectoryService.getRootPathByApp(app), YamlConstants.DEFAULTS, yaml, changeType, false));
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
  public List<GitFileChange> obtainSettingAttributeRenameChangeSet(
      String accountId, SettingAttribute oldSettingAttribute, SettingAttribute newSettingAttribute) {
    List<GitFileChange> changeSet = new ArrayList<>();
    String oldSettingAttrPath;
    List<GitFileChange> newSettingAttrGitSyncFile;

    if (isStringSettingAttributeType(newSettingAttribute)) {
      oldSettingAttrPath =
          yamlDirectoryService.getRootPathBySettingAttribute(oldSettingAttribute) + PATH_DELIMITER + DEFAULTS_YAML;
      newSettingAttrGitSyncFile =
          getDefaultVarGitSyncFile(accountId, newSettingAttribute.getAppId(), ChangeType.MODIFY);
    } else {
      oldSettingAttrPath = yamlDirectoryService.getRootPathBySettingAttribute(oldSettingAttribute) + PATH_DELIMITER
          + oldSettingAttribute.getName() + YAML_EXTENSION;
      newSettingAttrGitSyncFile =
          obtainEntityGitSyncFileChangeSet(accountId, null, newSettingAttribute, ChangeType.MODIFY);
    }

    changeSet.add(GitFileChange.Builder.aGitFileChange()
                      .withAccountId(accountId)
                      .withChangeType(ChangeType.DELETE)
                      .withFilePath(oldSettingAttrPath)
                      .build());
    changeSet.addAll(newSettingAttrGitSyncFile);
    return changeSet;
  }

  @Override
  public List<GitFileChange> obtainDefaultVariableChangeSet(String accountId, String appId, ChangeType changeType) {
    // The default variables is a special case where one yaml is mapped to a list of setting variables.
    // So, even if a default variable is deleted, we should not delete the whole file.
    // Sending DELETE would do that. So, sending MODIFY
    if (ChangeType.DELETE.equals(changeType)) {
      changeType = ChangeType.MODIFY;
    }
    return Lists.newArrayList(getDefaultVarGitSyncFile(accountId, appId, changeType));
  }

  private List<GitFileChange> obtainServiceVariableChangeSet(
      String accountId, ServiceVariable serviceVariable, ChangeType changeType) {
    Object entity = obtainEntity(serviceVariable.getAppId(), serviceVariable.getEnvId(), serviceVariable.getEntityId(),
        serviceVariable.getUuid(), serviceVariable.getEntityType());

    return obtainEntityGitSyncFileChangeSet(accountId, null, entity, changeType);
  }

  @Override
  public <R, T> List<GitFileChange> obtainEntityGitSyncFileChangeSet(
      String accountId, R helperEntity, T entity, ChangeType changeType) {
    List<GitFileChange> gitFileChanges = new ArrayList<>();
    String yaml = null;

    // Does special handling for some cases
    if (isStringSettingAttributeType(entity)) {
      return obtainDefaultVariableChangeSet(accountId, ((SettingAttribute) entity).getAppId(), changeType);
    } else if (entity instanceof ServiceVariable) {
      return obtainServiceVariableChangeSet(accountId, (ServiceVariable) entity, changeType);
    }

    if (!changeType.equals(ChangeType.DELETE)) {
      yaml = yamlResourceService.obtainEntityYamlVersion(accountId, entity).getResource().getYaml();
    }

    String yamlFileName = yamlHandlerFactory.obtainYamlFileName(entity);
    yamlFileName = Util.normalize(yamlFileName);
    boolean isNonLeafEntity = yamlHandlerFactory.isNonLeafEntity(entity);

    helperEntity = obtainHelperEntity(helperEntity, entity);
    gitFileChanges.add(createGitFileChange(accountId, yamlDirectoryService.obtainEntityRootPath(helperEntity, entity),
        yamlFileName, yaml, changeType, isNonLeafEntity));
    gitFileChanges.addAll(obtainAdditionalGitSyncFileChangeSet(accountId, helperEntity, entity, changeType));

    return gitFileChanges;
  }

  private <T> boolean isStringSettingAttributeType(T entity) {
    if (entity instanceof SettingAttribute) {
      SettingAttribute settingAttribute = (SettingAttribute) entity;
      return SettingVariableTypes.STRING.name().equals(settingAttribute.getValue().getType());
    }

    return false;
  }

  private <R, T> R obtainHelperEntity(R helperEntity, T entity) {
    if (entity instanceof ConfigFile) {
      ConfigFile configFile = (ConfigFile) entity;
      return obtainEntity(configFile.getAppId(), configFile.getEnvId(), configFile.getEntityId(), configFile.getUuid(),
          configFile.getEntityType());
    }

    return helperEntity;
  }

  private <R> R obtainEntity(String appId, String envId, String entityId, String uuid, EntityType type) {
    if (type == EntityType.SERVICE) {
      return (R) serviceResourceService.get(appId, entityId);
    } else if (type == EntityType.SERVICE_TEMPLATE || type == EntityType.ENVIRONMENT) {
      String finalEnvId = null;

      if (type == EntityType.SERVICE_TEMPLATE) {
        finalEnvId = envId;
      } else if (type == EntityType.ENVIRONMENT) {
        finalEnvId = entityId;
      }

      Environment environment = environmentService.get(appId, finalEnvId, false);
      Validator.notNullCheck("Environment not found for the given id:" + finalEnvId, environment);
      return (R) environment;
    } else {
      String msg = "Unsupported type " + type + " for id " + uuid;
      logger.error(msg);
      throw new WingsException(msg);
    }
  }

  private <R, T> List<GitFileChange> obtainAdditionalGitSyncFileChangeSet(
      String accountId, R helperEntity, T entity, ChangeType changeType) {
    List<GitFileChange> gitFileChanges = new ArrayList<>();

    if (entity instanceof Service) {
      Service service = (Service) entity;

      if (changeType.equals(ChangeType.ADD) && !serviceResourceService.hasInternalCommands(service)) {
        serviceResourceService.getServiceCommands(service.getAppId(), service.getUuid())
            .forEach(serviceCommand
                -> gitFileChanges.add(getCommandGitSyncFile(accountId, service, serviceCommand, ChangeType.ADD)));
      }
    } else if (entity instanceof ConfigFile) {
      ConfigFile configFile = (ConfigFile) entity;
      String fileContent = loadFileContentIntoString(configFile);
      String fileName = Util.normalize(configFile.getRelativeFilePath());

      if (fileContent != null) {
        gitFileChanges.add(createConfigFileChange(
            accountId, yamlDirectoryService.getRootPathByConfigFile(helperEntity), fileName, fileContent, changeType));
      }
    }

    return gitFileChanges;
  }

  private String loadFileContentIntoString(ConfigFile configFile) {
    if (!configFile.isEncrypted()) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      fileService.downloadToStream(configFile.getFileUuid(), outputStream, FileBucket.CONFIGS);
      return new String(outputStream.toByteArray(), UTF_8);
    }
    return null;
  }
}
