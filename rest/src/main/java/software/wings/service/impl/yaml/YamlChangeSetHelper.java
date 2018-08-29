package software.wings.service.impl.yaml;

import static java.util.Arrays.asList;
import static software.wings.beans.yaml.YamlConstants.DEFAULTS_YAML;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.NotificationGroup;
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
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Created by anubhaw on 12/3/17.
 */
@Singleton
public class YamlChangeSetHelper {
  private static final Logger logger = LoggerFactory.getLogger(YamlChangeSetHelper.class);

  private static final Set<String> nonLeafEntities = new HashSet(obtainNonLeafEntities());

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

  public void containerTaskYamlChangeAsync(
      String accountId, Service service, ContainerTask containerTask, ChangeType changeType) {
    executorService.submit(() -> containerTaskYamlChange(accountId, service, containerTask, changeType));
  }

  public void containerTaskYamlChange(
      String accountId, Service service, ContainerTask containerTask, ChangeType changeType) {
    GitFileChange gitSyncFile =
        entityUpdateService.getContainerTaskGitSyncFile(accountId, service, containerTask, changeType);
    queueYamlChangeSet(accountId, gitSyncFile);
  }

  public void helmChartSpecificationYamlChangeAsync(
      String accountId, Service service, HelmChartSpecification helmChartSpecification, ChangeType changeType) {
    executorService.submit(
        () -> helmChartSpecificationYamlChange(accountId, service, helmChartSpecification, changeType));
  }

  public void pcfServiceSpecificationYamlChangeAsync(
      String accountId, Service service, PcfServiceSpecification pcfServiceSpecification, ChangeType changeType) {
    executorService.submit(
        () -> pcfServiceSpecificationYamlChange(accountId, service, pcfServiceSpecification, changeType));
  }

  public void helmChartSpecificationYamlChange(
      String accountId, Service service, HelmChartSpecification helmChartSpecification, ChangeType changeType) {
    GitFileChange gitSyncFile =
        entityUpdateService.getHelmChartGitSyncFile(accountId, service, helmChartSpecification, changeType);
    queueYamlChangeSet(accountId, gitSyncFile);
  }

  public void pcfServiceSpecificationYamlChange(
      String accountId, Service service, PcfServiceSpecification pcfServiceSpecification, ChangeType changeType) {
    GitFileChange gitSyncFile =
        entityUpdateService.getPcfServiceSpecification(accountId, service, pcfServiceSpecification, changeType);
    queueYamlChangeSet(accountId, gitSyncFile);
  }

  public void lamdbaSpecYamlChangeAsync(
      String accountId, Service service, LambdaSpecification lambdaSpec, ChangeType changeType) {
    executorService.submit(() -> lamdbaSpecYamlChange(accountId, service, lambdaSpec, changeType));
  }

  public void lamdbaSpecYamlChange(
      String accountId, Service service, LambdaSpecification lambdaSpec, ChangeType changeType) {
    GitFileChange gitSyncFile =
        entityUpdateService.getLamdbaSpecGitSyncFile(accountId, service, lambdaSpec, changeType);
    queueYamlChangeSet(accountId, gitSyncFile);
  }

  public void userDataSpecYamlChangeAsync(
      String accountId, Service service, UserDataSpecification userDataSpec, ChangeType changeType) {
    executorService.submit(() -> userDataSpecYamlChange(accountId, service, userDataSpec, changeType));
  }

  public void userDataSpecYamlChange(
      String accountId, Service service, UserDataSpecification userDataSpec, ChangeType changeType) {
    GitFileChange gitSyncFile =
        entityUpdateService.getUserDataGitSyncFile(accountId, service, userDataSpec, changeType);
    queueYamlChangeSet(accountId, gitSyncFile);
  }

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

  public void notificationGroupYamlChangeAsync(NotificationGroup notificationGroup, ChangeType change) {
    executorService.submit(() -> notificationGroupYamlChangeSet(notificationGroup, change));
  }

  public void notificationGroupYamlChangeSet(NotificationGroup notificationGroup, ChangeType crudType) {
    // check whether we need to push changes (through git sync)
    String accountId = notificationGroup.getAccountId();
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();
      changeSet.add(entityUpdateService.getNotificationGroupGitSyncFile(accountId, notificationGroup, crudType));
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }

  /**
   * This is called when a yaml file has been renamed.
   * e.g. When Service Infra name is changed, it causes rename operation for its yaml file.
   * NOTE: here only file name is changing and not the path.
   * @param updatedValue
   * @param oldValue
   * @param accountId
   */
  public <T> void updateYamlChangeAsync(T updatedValue, T oldValue, String accountId, boolean isRename) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      executorService.submit(() -> updateYamlChange(ygs, updatedValue, oldValue, accountId, isRename));
    }
  }

  private <T> void updateYamlChange(YamlGitConfig ygs, T updatedValue, T oldValue, String accountId, boolean isRename) {
    try {
      if (isRename) {
        // Name was changed, so yaml file name will also change
        yamlFileRenameChange(ygs, updatedValue, oldValue, accountId);
      } else {
        yamlFileUpdateChange(ygs, updatedValue, accountId);
      }
    } catch (Exception e) {
      logger.error("Error in git sync update for: " + updatedValue + ", can not execute getName method");
    }
  }

  /**
   * This method is called when a yaml file is renamed and not dir.
   * So path remains the same, only file name changes.
   * This does not affect any other files as it happens in case of YAML DIR node rename like App.
   * @param ygs
   * @param newValue
   * @param oldValue
   * @param accountId
   */
  private <T> void yamlFileRenameChange(YamlGitConfig ygs, T newValue, T oldValue, String accountId) {
    List<GitFileChange> changeSet = new ArrayList<>();
    // Rename is delete old and add new
    changeSet.add(getGitSyncFile(accountId, oldValue, ChangeType.DELETE));
    changeSet.add(getGitSyncFile(accountId, newValue, ChangeType.ADD));
    yamlChangeSetService.saveChangeSet(ygs, changeSet);
  }

  private <T> void yamlFileUpdateChange(YamlGitConfig ygs, T value, String accountId) {
    List<GitFileChange> changeSet = new ArrayList<>();

    changeSet.add(getGitSyncFile(accountId, value, ChangeType.MODIFY));
    yamlChangeSetService.saveChangeSet(ygs, changeSet);
  }

  private <T> GitFileChange getGitSyncFile(String accountId, T value, ChangeType changeType) {
    GitFileChange gitFileChange = null;
    if (value instanceof InfrastructureMapping) {
      gitFileChange =
          entityUpdateService.getInfraMappingGitSyncFile(accountId, (InfrastructureMapping) value, changeType);
    } else if (value instanceof ArtifactStream) {
      gitFileChange = entityUpdateService.getArtifactStreamGitSyncFile(accountId, (ArtifactStream) value, changeType);
    } else if (value instanceof NotificationGroup) {
      gitFileChange =
          entityUpdateService.getNotificationGroupGitSyncFile(accountId, (NotificationGroup) value, changeType);
    }

    return gitFileChange;
  }

  private void serviceYamlChangeSet(Service service, ChangeType crudType) {
    // check whether we need to push changes (through git sync)
    String accountId = appService.getAccountIdByAppId(service.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();
      changeSet.add(entityUpdateService.getServiceGitSyncFile(accountId, service, crudType));
      if (crudType.equals(ChangeType.ADD) && !serviceResourceService.hasInternalCommands(service)) {
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
      List<GitFileChange> changeSet = entityUpdateService.obtainEntityGitSyncFileChangeSet(accountId, entity, crudType);
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }

  private <T> void entityRenameYamlChange(String accountId, T oldEntity, T newEntity) {
    String entityName = oldEntity.getClass().getSimpleName().toLowerCase();

    if (isNonLeafEntity(entityName)) {
      nonLeafEntityRenameYamlChange(accountId, oldEntity, newEntity);
    } else {
      leafEntityRenameYamlChange(accountId, oldEntity, newEntity);
    }
  }

  private <T> void nonLeafEntityRenameYamlChange(String accountId, T oldEntity, T newEntity) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);

    if (ygs != null) {
      String oldEnvnPath = yamlDirectoryService.obtainEntityRootPath(oldEntity);
      String newEnvnPath = yamlDirectoryService.obtainEntityRootPath(newEntity);

      List<GitFileChange> changeSet = new ArrayList<>();
      changeSet.add(GitFileChange.Builder.aGitFileChange()
                        .withAccountId(accountId)
                        .withChangeType(ChangeType.RENAME)
                        .withFilePath(newEnvnPath)
                        .withOldFilePath(oldEnvnPath)
                        .build());
      changeSet.addAll(entityUpdateService.obtainEntityGitSyncFileChangeSet(accountId, newEntity, ChangeType.MODIFY));
      changeSet.addAll(yamlGitService.performFullSyncDryRun(accountId));

      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }

  private <T> void leafEntityRenameYamlChange(String accountId, T oldEntity, T newEntity) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);

    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();

      // Rename is delete old and add new
      changeSet.add(getGitSyncFile(accountId, oldEntity, ChangeType.DELETE));
      changeSet.add(getGitSyncFile(accountId, newEntity, ChangeType.ADD));
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }

  private boolean isNonLeafEntity(String entity) {
    return nonLeafEntities.contains(entity);
  }

  private static List<String> obtainNonLeafEntities() {
    return Lists.newArrayList(EntityType.ENVIRONMENT.toString().toLowerCase());
  }
}
