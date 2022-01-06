/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml;

import io.harness.beans.FeatureName;
import io.harness.ff.FeatureFlagService;
import io.harness.git.model.ChangeType;

import software.wings.beans.ConfigFile;
import software.wings.beans.SettingAttribute;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.yaml.GitFileChange;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlGitConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by anubhaw on 12/3/17.
 */
@Singleton
@Slf4j
public class YamlChangeSetHelper {
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private YamlGitService yamlGitService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private FeatureFlagService featureFlagService;

  public List<GitFileChange> getConfigFileGitChangeSet(ConfigFile configFile, ChangeType changeType) {
    return entityUpdateService.obtainEntityGitSyncFileChangeSet(
        configFile.getAccountId(), null, configFile, changeType);
  }

  public List<GitFileChange> getManifestFileGitChangeSet(ManifestFile manifestFile, ChangeType changeType) {
    String accountId = entityUpdateService.obtainAccountIdFromEntity(manifestFile);
    return entityUpdateService.obtainEntityGitSyncFileChangeSet(accountId, null, manifestFile, changeType);
  }

  public List<GitFileChange> getApplicationManifestGitChangeSet(
      ApplicationManifest applicationManifest, ChangeType changeType) {
    String accountId = entityUpdateService.obtainAccountIdFromEntity(applicationManifest);
    return entityUpdateService.obtainEntityGitSyncFileChangeSet(accountId, null, applicationManifest, changeType);
  }

  public void defaultVariableChangeSet(String accountId, String appId, ChangeType changeType) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId, appId);
    if (ygs != null) {
      List<GitFileChange> gitFileChanges =
          entityUpdateService.obtainDefaultVariableChangeSet(accountId, appId, changeType);

      yamlChangeSetService.saveChangeSet(accountId, gitFileChanges, appId);
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

  public <R, T> void entityYamlChangeSet(String accountId, R helperEntity, T entity, ChangeType crudType) {
    String appId = entityUpdateService.obtainAppIdFromEntity(entity);
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId, appId);

    if (ygs != null) {
      List<GitFileChange> changeSet =
          entityUpdateService.obtainEntityGitSyncFileChangeSet(accountId, helperEntity, entity, crudType);

      yamlChangeSetService.saveChangeSet(accountId, changeSet, entity);
    }
  }

  private <T> void entityRenameYamlChange(String accountId, T oldEntity, T newEntity) {
    if (!featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
      if (yamlHandlerFactory.isNonLeafEntity(oldEntity)) {
        nonLeafEntityRenameYamlChange(accountId, oldEntity, newEntity);
      } else {
        leafEntityRenameYamlChange(accountId, oldEntity, newEntity);
      }
    } else {
      if (yamlHandlerFactory.isNonLeafEntityWithFeatureFlag(oldEntity)) {
        nonLeafEntityRenameYamlChange(accountId, oldEntity, newEntity);
      } else {
        leafEntityRenameYamlChange(accountId, oldEntity, newEntity);
      }
    }
  }

  private <T> void nonLeafEntityRenameYamlChange(String accountId, T oldEntity, T newEntity) {
    // We dont need to check whether the entity has valid enabled yamlGitConfig. This is to handle the case where you
    // dont have account level yamlGit configured but some app has. In such case we want to push changes for that app to
    // git still.

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
    YamlChangeSet savedChangeSet = yamlChangeSetService.saveChangeSet(accountId, changeSet, newEntity);
    String parentYamlChangeSetId = savedChangeSet.getUuid();

    List<YamlChangeSet> yamlChangeSets = yamlGitService.obtainChangeSetFromFullSyncDryRun(accountId, true);
    for (YamlChangeSet yamlChangeSet : yamlChangeSets) {
      yamlChangeSet.setParentYamlChangeSetId(parentYamlChangeSetId);
      yamlChangeSetService.save(yamlChangeSet);
    }
  }

  private <T> void leafEntityRenameYamlChange(String accountId, T oldEntity, T newEntity) {
    // We dont need to check whether the entity has valid enabled yamlGitConfig. This is to handle the case where you
    // dont have account level yamlGit configured but some app has. In such case we want to push changes for that app to
    // git still.

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
    YamlChangeSet savedChangeSet = yamlChangeSetService.saveChangeSet(accountId, changeSet, newEntity);
    String parentYamlChangeSetId = savedChangeSet.getUuid();

    List<YamlChangeSet> yamlChangeSets = yamlGitService.obtainChangeSetFromFullSyncDryRun(accountId, true);
    for (YamlChangeSet yamlChangeSet : yamlChangeSets) {
      yamlChangeSet.setParentYamlChangeSetId(parentYamlChangeSetId);
      yamlChangeSetService.save(yamlChangeSet);
    }
  }
}
