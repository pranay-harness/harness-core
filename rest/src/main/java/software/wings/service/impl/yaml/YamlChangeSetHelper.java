package software.wings.service.impl.yaml;

import static java.util.Arrays.asList;
import static software.wings.beans.yaml.YamlConstants.DEFAULTS_YAML;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;

import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.Validator;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by anubhaw on 12/3/17.
 */
@Singleton
public class YamlChangeSetHelper {
  private static final Logger logger = LoggerFactory.getLogger(YamlChangeSetHelper.class);

  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private ExecutorService executorService;
  @Inject private YamlGitService yamlGitService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private FileService fileService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;

  public void configFileYamlChangeAsync(ConfigFile configFile, ChangeType changeType) {
    executorService.submit(() -> configFileYamlChange(configFile, changeType));
  }

  public void configFileYamlChange(ConfigFile configFile, ChangeType changeType) {
    queueYamlChangeSet(configFile.getAccountId(), getConfigFileGitChangeSet(configFile, changeType));
  }

  public List<GitFileChange> getConfigFileGitChangeSet(ConfigFile configFile, ChangeType changeType) {
    if (configFile.getEntityType() == EntityType.SERVICE) {
      String fileContent = loadFileContentIntoString(configFile);

      Service service = serviceResourceService.get(configFile.getAppId(), configFile.getEntityId());
      return entityUpdateService.getConfigFileGitSyncFileSet(
          configFile.getAccountId(), service, configFile, changeType, fileContent);

    } else if (configFile.getEntityType() == EntityType.SERVICE_TEMPLATE
        || configFile.getEntityType() == EntityType.ENVIRONMENT) {
      String fileContent = loadFileContentIntoString(configFile);
      String envId = null;
      if (configFile.getEntityType() == EntityType.SERVICE_TEMPLATE) {
        envId = configFile.getEnvId();
      } else if (configFile.getEntityType() == EntityType.ENVIRONMENT) {
        envId = configFile.getEntityId();
      }

      Environment environment = environmentService.get(configFile.getAppId(), envId, false);
      Validator.notNullCheck("Environment not found for the given id:" + envId, environment);
      return entityUpdateService.getConfigFileOverrideGitSyncFileSet(
          configFile.getAccountId(), environment, configFile, changeType, fileContent);

    } else {
      String msg =
          "Unsupported override type " + configFile.getEntityType() + " for config file " + configFile.getUuid();
      logger.error(msg);
      throw new WingsException(msg);
    }
  }

  @SuppressFBWarnings("DM_DEFAULT_ENCODING")
  private String loadFileContentIntoString(ConfigFile configFile) {
    if (!configFile.isEncrypted()) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      fileService.downloadToStream(configFile.getFileUuid(), outputStream, FileBucket.CONFIGS);
      return outputStream.toString();
    }
    return null;
  }

  public void commandFileChangeAsync(
      String accountId, Service service, ServiceCommand serviceCommand, ChangeType changeType) {
    executorService.submit(() -> { commandFileChange(accountId, service, serviceCommand, changeType); });
  }

  public void commandFileChange(
      String accountId, Service service, ServiceCommand serviceCommand, ChangeType changeType) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();
      changeSet.add(entityUpdateService.getCommandGitSyncFile(accountId, service, serviceCommand, changeType));
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }

  private void queueYamlChangeSet(String accountId, List<GitFileChange> gitFileChangeList) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      yamlChangeSetService.saveChangeSet(ygs, gitFileChangeList);
    }
  }

  private void queueYamlChangeSet(String accountId, GitFileChange gitFileChange) {
    queueYamlChangeSet(accountId, asList(gitFileChange));
  }

  public void queueSettingUpdateYamlChangeAsync(
      SettingAttribute savedSettingAttributes, SettingAttribute updatedSettingAttribute) {
    executorService.submit(() -> {
      if (!savedSettingAttributes.getName().equals(updatedSettingAttribute.getName())) {
        queueMoveSettingChange(savedSettingAttributes, updatedSettingAttribute);
      } else {
        if (isDefaultVariableType(updatedSettingAttribute)) {
          queueDefaultVariableChange(
              updatedSettingAttribute.getAccountId(), updatedSettingAttribute.getAppId(), ChangeType.MODIFY);
        } else {
          queueSettingYamlChange(updatedSettingAttribute,
              entityUpdateService.getSettingAttributeGitSyncFile(
                  updatedSettingAttribute.getAccountId(), updatedSettingAttribute, ChangeType.MODIFY));
        }
      }
    });
  }

  public void queueSettingYamlChangeAsync(SettingAttribute newSettingAttribute, ChangeType changeType) {
    executorService.submit(() -> {
      if (isDefaultVariableType(newSettingAttribute)) {
        queueDefaultVariableChange(newSettingAttribute.getAccountId(), newSettingAttribute.getAppId(), changeType);
      } else {
        queueSettingYamlChange(newSettingAttribute,
            entityUpdateService.getSettingAttributeGitSyncFile(
                newSettingAttribute.getAccountId(), newSettingAttribute, changeType));
      }
    });
  }

  public void queueDefaultVariableChangeAsync(String accountId, String appId, ChangeType changeType) {
    executorService.submit(() -> { queueDefaultVariableChange(accountId, appId, changeType); });
  }

  private boolean isDefaultVariableType(SettingAttribute settingAttribute) {
    return SettingVariableTypes.STRING.name().equals(settingAttribute.getValue().getType());
  }

  private void queueSettingYamlChange(SettingAttribute newSettingAttribute, GitFileChange settingAttributeGitSyncFile) {
    String accountId = newSettingAttribute.getAccountId();
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      yamlChangeSetService.saveChangeSet(ygs, asList(settingAttributeGitSyncFile));
    }
  }

  public void queueDefaultVariableChange(String accountId, String appId, ChangeType changeType) {
    // The default variables is a special case where one yaml is mapped to a list of setting variables.
    // So, even if a default variable is deleted, we should not delete the whole file.
    // Sending DELETE would do that. So, sending MODIFY
    if (ChangeType.DELETE.equals(changeType)) {
      changeType = ChangeType.MODIFY;
    }
    queueYamlChangeSet(accountId, entityUpdateService.getDefaultVarGitSyncFile(accountId, appId, changeType));
  }

  private void queueMoveSettingChange(SettingAttribute oldSettingAttribute, SettingAttribute newSettingAttribute) {
    String accountId = newSettingAttribute.getAccountId();
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();

      String oldSettingAttrPath;
      GitFileChange newSettingAttrGitSyncFile;
      if (isDefaultVariableType(newSettingAttribute)) {
        oldSettingAttrPath =
            yamlDirectoryService.getRootPathBySettingAttribute(oldSettingAttribute) + PATH_DELIMITER + DEFAULTS_YAML;
        newSettingAttrGitSyncFile =
            entityUpdateService.getDefaultVarGitSyncFile(accountId, newSettingAttribute.getAppId(), ChangeType.MODIFY);
      } else {
        oldSettingAttrPath = yamlDirectoryService.getRootPathBySettingAttribute(oldSettingAttribute) + PATH_DELIMITER
            + oldSettingAttribute.getName() + YAML_EXTENSION;
        newSettingAttrGitSyncFile =
            entityUpdateService.getSettingAttributeGitSyncFile(accountId, newSettingAttribute, ChangeType.MODIFY);
      }

      changeSet.add(GitFileChange.Builder.aGitFileChange()
                        .withAccountId(newSettingAttrGitSyncFile.getAccountId())
                        .withChangeType(ChangeType.DELETE)
                        .withFilePath(oldSettingAttrPath)
                        .build());

      changeSet.add(newSettingAttrGitSyncFile);
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
      // As Setting Attribute has changed, it may affect other entities yaml,
      // e.g. Infra using cloud provider would be affected if cloudProvider name changes
      // So perform git full sync
      addFullSyncChangeSet(ygs, accountId);
    }
  }

  private void addFullSyncChangeSet(YamlGitConfig ygs, String accountId) {
    try {
      List<GitFileChange> changeSet = new ArrayList<>();
      changeSet.addAll(yamlGitService.performFullSyncDryRun(accountId));
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    } catch (Exception e) {
      logger.warn(new StringBuilder()
                      .append("Full sync failed with exception: ")
                      .append(e)
                      .append("\n git may be in inconsistent state, please perform git full sync/Reset")
                      .toString());
    }
  }

  public <T> void entityUpdateYamlChange(String accountId, T oldEntity, T newEntity, boolean isRename) {
    if (isRename) {
      entityRenameYamlChange(accountId, oldEntity, newEntity);
    } else {
      entityYamlChangeSet(accountId, newEntity, ChangeType.MODIFY);
    }
  }

  public <T> void entityYamlChangeSet(String accountId, T entity, ChangeType crudType) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);

    if (ygs != null) {
      List<GitFileChange> changeSet =
          entityUpdateService.obtainEntityGitSyncFileChangeSet(accountId, null, entity, crudType);
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }

  public <T> void entitySpecYamlChangeSet(String accountId, Service service, T entity, ChangeType crudType) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);

    if (ygs != null) {
      List<GitFileChange> changeSet =
          entityUpdateService.obtainEntityGitSyncFileChangeSet(accountId, service, entity, crudType);
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }

  private <T> void entityRenameYamlChange(String accountId, T oldEntity, T newEntity) {
    if (yamlHandlerFactory.isNonLeafEntity(oldEntity)) {
      nonLeafEntityRenameYamlChange(accountId, oldEntity, newEntity);
    } else {
      leafEntityRenameYamlChange(accountId, oldEntity, newEntity);
    }
  }

  private <T> void nonLeafEntityRenameYamlChange(String accountId, T oldEntity, T newEntity) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);

    if (ygs != null) {
      String oldEnvnPath = yamlDirectoryService.obtainEntityRootPath(null, oldEntity);
      String newEnvnPath = yamlDirectoryService.obtainEntityRootPath(null, newEntity);

      List<GitFileChange> changeSet = new ArrayList<>();
      changeSet.add(GitFileChange.Builder.aGitFileChange()
                        .withAccountId(accountId)
                        .withChangeType(ChangeType.RENAME)
                        .withFilePath(newEnvnPath)
                        .withOldFilePath(oldEnvnPath)
                        .build());
      changeSet.addAll(
          entityUpdateService.obtainEntityGitSyncFileChangeSet(accountId, null, newEntity, ChangeType.MODIFY));
      changeSet.addAll(yamlGitService.performFullSyncDryRun(accountId));

      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }

  private <T> void leafEntityRenameYamlChange(String accountId, T oldEntity, T newEntity) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);

    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();

      // Rename is delete old and add new
      changeSet.addAll(
          entityUpdateService.obtainEntityGitSyncFileChangeSet(accountId, null, oldEntity, ChangeType.DELETE));
      changeSet.addAll(
          entityUpdateService.obtainEntityGitSyncFileChangeSet(accountId, null, newEntity, ChangeType.ADD));

      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }
}