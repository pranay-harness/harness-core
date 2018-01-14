package software.wings.service.impl.yaml;

import static java.util.Arrays.asList;
import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;

import com.google.inject.Inject;

import groovy.lang.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
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
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private FileService fileService;

  public void applicationUpdateYamlChangeAsync(Application savedApp, Application updatedApp) {
    executorService.submit(() -> {
      if (!savedApp.getName().equals(updatedApp.getName())) {
        moveApplicationYamlChange(savedApp, updatedApp);
      } else {
        applicationYamlChange(
            updatedApp.getAccountId(), entityUpdateService.getAppGitSyncFile(updatedApp, ChangeType.MODIFY));
      }
    });
  }

  public void applicationYamlChangeAsync(Application app, ChangeType changeType) {
    executorService.submit(() -> applicationYamlChange(app, changeType));
  }

  public void applicationYamlChange(Application app, ChangeType changeType) {
    applicationYamlChange(app.getAccountId(), entityUpdateService.getAppGitSyncFile(app, changeType));
  }

  private void applicationYamlChange(String accountId, GitFileChange gitFileChange) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      yamlChangeSetService.saveChangeSet(ygs, asList(gitFileChange));
    }
  }

  public void configFileYamlChangeAsync(ConfigFile configFile, ChangeType changeType) {
    executorService.submit(() -> configFileYamlChange(configFile, changeType));
  }

  public void configFileYamlChange(ConfigFile configFile, ChangeType changeType) {
    if (configFile.getEntityType() == EntityType.SERVICE) {
      String fileContent = loadFileContentIntoString(configFile);

      Service service = serviceResourceService.get(configFile.getAppId(), configFile.getEntityId());
      queueYamlChangeSet(configFile.getAccountId(),
          entityUpdateService.getConfigFileGitSyncFileSet(
              configFile.getAccountId(), service, configFile, changeType, fileContent));
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
      queueYamlChangeSet(configFile.getAccountId(),
          entityUpdateService.getConfigFileOverrideGitSyncFileSet(
              configFile.getAccountId(), environment, configFile, changeType, fileContent));
    } else {
      logger.error("Unsupported override type {} for config file {}", configFile.getEntityType(), configFile.getUuid());
    }
  }

  private String loadFileContentIntoString(ConfigFile configFile) {
    if (!configFile.isEncrypted()) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      fileService.downloadToStream(configFile.getFileUuid(), outputStream, FileBucket.CONFIGS);
      return outputStream.toString();
    }
    return null;
  }

  private void queueYamlChangeSet(String accountId, List<GitFileChange> gitFileChangeList) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      yamlChangeSetService.saveChangeSet(ygs, gitFileChangeList);
    }
  }

  private void moveApplicationYamlChange(Application oldApp, Application newApp) {
    String accountId = newApp.getAccountId();
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();

      String oldPath = yamlDirectoryService.getRootPathByApp(oldApp);
      String newPath = yamlDirectoryService.getRootPathByApp(newApp);

      changeSet.add(GitFileChange.Builder.aGitFileChange()
                        .withAccountId(accountId)
                        .withChangeType(ChangeType.RENAME)
                        .withFilePath(newPath)
                        .withOldFilePath(oldPath)
                        .build());
      changeSet.add(entityUpdateService.getAppGitSyncFile(newApp, ChangeType.MODIFY));
      changeSet.addAll(yamlGitService.performFullSyncDryRun(accountId)); // full sync on name change
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }

  public void environmentUpdateYamlChangeAsync(Environment savedEnvironment, Environment updatedEnvironment) {
    executorService.submit(() -> {
      if (!savedEnvironment.getName().equals(updatedEnvironment.getName())) {
        moveEnvironmentChange(savedEnvironment, updatedEnvironment);
      } else {
        environmentYamlChangeSet(updatedEnvironment, ChangeType.MODIFY);
      }
    });
  }

  public void environmentYamlChangeAsync(Environment savedEnvironment, ChangeType changeType) {
    executorService.submit(() -> environmentYamlChange(savedEnvironment, changeType));
  }

  public void environmentYamlChange(Environment savedEnvironment, ChangeType changeType) {
    environmentYamlChangeSet(savedEnvironment, changeType);
  }

  private void moveEnvironmentChange(Environment oldEnv, Environment newEnv) {
    String accountId = appService.getAccountIdByAppId(newEnv.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();

      String oldEnvnPath = yamlDirectoryService.getRootPathByEnvironment(oldEnv);
      String newEnvnPath = yamlDirectoryService.getRootPathByEnvironment(newEnv);

      changeSet.add(GitFileChange.Builder.aGitFileChange()
                        .withAccountId(accountId)
                        .withChangeType(ChangeType.RENAME)
                        .withFilePath(newEnvnPath)
                        .withOldFilePath(oldEnvnPath)
                        .build());
      changeSet.add(entityUpdateService.getEnvironmentGitSyncFile(accountId, newEnv, ChangeType.MODIFY));
      changeSet.addAll(yamlGitService.performFullSyncDryRun(accountId)); // full sync on name change
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }

  private void environmentYamlChangeSet(Environment environment, ChangeType crudType) {
    // check whether we need to push changes (through git sync)
    String accountId = appService.getAccountIdByAppId(environment.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();
      changeSet.add(entityUpdateService.getEnvironmentGitSyncFile(accountId, environment, crudType));
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }

  public void serviceUpdateYamlChangeAsync(Service service, Service savedService, Service updatedService) {
    executorService.submit(() -> {
      if (!savedService.getName().equals(updatedService.getName())) { // Service name changed
        moveServiceChange(savedService, service);
      } else {
        serviceYamlChangeSet(updatedService, ChangeType.MODIFY);
      }
    });
  }

  public void serviceYamlChangeAsync(Service finalSavedService, ChangeType change) {
    executorService.submit(() -> serviceYamlChange(finalSavedService, change));
  }

  public void serviceYamlChange(Service finalSavedService, ChangeType change) {
    serviceYamlChangeSet(finalSavedService, change);
  }

  private void moveServiceChange(Service oldService, Service newService) {
    String accountId = appService.getAccountIdByAppId(newService.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();

      String oldPath = yamlDirectoryService.getRootPathByService(oldService);
      String newPath = yamlDirectoryService.getRootPathByService(newService);

      changeSet.add(GitFileChange.Builder.aGitFileChange()
                        .withAccountId(accountId)
                        .withChangeType(ChangeType.RENAME)
                        .withFilePath(newPath)
                        .withOldFilePath(oldPath)
                        .build());
      changeSet.add(entityUpdateService.getServiceGitSyncFile(accountId, newService, ChangeType.MODIFY));
      changeSet.addAll(yamlGitService.performFullSyncDryRun(accountId));
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }

  private void serviceYamlChangeSet(Service service, ChangeType crudType) {
    // check whether we need to push changes (through git sync)
    String accountId = appService.getAccountIdByAppId(service.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();
      changeSet.add(entityUpdateService.getServiceGitSyncFile(accountId, service, crudType));
      if (crudType.equals(ChangeType.ADD) && service.getArtifactType().shouldPushCommandsToYaml()) {
        serviceResourceService.getServiceCommands(service.getAppId(), service.getUuid())
            .forEach(serviceCommand
                -> changeSet.add(
                    entityUpdateService.getCommandGitSyncFile(accountId, service, serviceCommand, ChangeType.ADD)));
      }
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }

  public void queueSettingUpdateYamlChangeAsync(
      SettingAttribute savedSettingAttributes, SettingAttribute updatedSettingAttribute) {
    executorService.submit(() -> {
      if (!updatedSettingAttribute.getName().equals(updatedSettingAttribute.getName())) {
        queueMoveSettingChange(savedSettingAttributes, updatedSettingAttribute);
      } else {
        queueSettingYamlChange(updatedSettingAttribute,
            entityUpdateService.getSettingAttributeGitSyncFile(
                updatedSettingAttribute.getAccountId(), updatedSettingAttribute, ChangeType.MODIFY));
      }
    });
  }

  public void queueSettingYamlChangeAsync(
      SettingAttribute settingAttribute, SettingAttribute newSettingAttribute, ChangeType add) {
    executorService.submit(()
                               -> queueSettingYamlChange(newSettingAttribute,
                                   entityUpdateService.getSettingAttributeGitSyncFile(
                                       settingAttribute.getAccountId(), newSettingAttribute, add)));
  }

  private void queueSettingYamlChange(SettingAttribute newSettingAttribute, GitFileChange settingAttributeGitSyncFile) {
    String accountId = newSettingAttribute.getAccountId();
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      yamlChangeSetService.saveChangeSet(ygs, asList(settingAttributeGitSyncFile));
    }
  }

  private void queueMoveSettingChange(SettingAttribute oldSettingAttribute, SettingAttribute newSettingAttribute) {
    String accountId = appService.getAccountIdByAppId(newSettingAttribute.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();

      String oldSettingAttrPath = yamlDirectoryService.getRootPathBySettingAttribute(oldSettingAttribute) + "/"
          + oldSettingAttribute.getName() + YAML_EXTENSION;
      GitFileChange newSettingAttrGitSyncFile =
          entityUpdateService.getSettingAttributeGitSyncFile(accountId, newSettingAttribute, ChangeType.MODIFY);

      changeSet.add(GitFileChange.Builder.aGitFileChange()
                        .withAccountId(newSettingAttrGitSyncFile.getAccountId())
                        .withChangeType(ChangeType.RENAME)
                        .withFilePath(newSettingAttrGitSyncFile.getFilePath())
                        .withOldFilePath(oldSettingAttrPath)
                        .build());
      changeSet.add(newSettingAttrGitSyncFile);
      changeSet.addAll(yamlGitService.performFullSyncDryRun(accountId));
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }
}
