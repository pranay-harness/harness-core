package software.wings.service.impl.yaml;

import com.google.inject.Inject;

import groovy.lang.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ConfigFile;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 12/3/17.
 */
@Singleton
public class YamlChangeSetHelper {
  private static final Logger logger = LoggerFactory.getLogger(YamlChangeSetHelper.class);

  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private YamlGitService yamlGitService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;

  public List<GitFileChange> getConfigFileGitChangeSet(ConfigFile configFile, ChangeType changeType) {
    return entityUpdateService.obtainEntityGitSyncFileChangeSet(
        configFile.getAccountId(), null, configFile, changeType);
  }

  public void defaultVariableChangeSet(String accountId, String appId, ChangeType changeType) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> gitFileChanges =
          entityUpdateService.obtainDefaultVariableChangeSet(accountId, appId, changeType);

      yamlChangeSetService.saveChangeSet(ygs, gitFileChanges);
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
    entityYamlChangeSet(accountId, null, entity, crudType);
  }

  public <T> void entityYamlChangeSet(String accountId, Service service, T entity, ChangeType crudType) {
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
      String oldPath = yamlDirectoryService.obtainEntityRootPath(null, oldEntity);
      String newPath = yamlDirectoryService.obtainEntityRootPath(null, newEntity);

      List<GitFileChange> changeSet = new ArrayList<>();
      changeSet.add(GitFileChange.Builder.aGitFileChange()
                        .withAccountId(accountId)
                        .withChangeType(ChangeType.RENAME)
                        .withFilePath(newPath)
                        .withOldFilePath(oldPath)
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

      if (newEntity instanceof SettingAttribute) {
        changeSet.addAll(entityUpdateService.obtainSettingAttributeRenameChangeSet(
            accountId, (SettingAttribute) oldEntity, (SettingAttribute) newEntity));
      } else {
        // Rename is delete old and add new
        changeSet.addAll(
            entityUpdateService.obtainEntityGitSyncFileChangeSet(accountId, null, oldEntity, ChangeType.DELETE));
        changeSet.addAll(
            entityUpdateService.obtainEntityGitSyncFileChangeSet(accountId, null, newEntity, ChangeType.ADD));
      }

      changeSet.addAll(yamlGitService.performFullSyncDryRun(accountId));
      yamlChangeSetService.saveChangeSet(ygs, changeSet);
    }
  }
}