package software.wings.service.impl.yaml;

import groovy.lang.Singleton;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

/**
 * Created by anubhaw on 12/3/17.
 */
@Singleton
public class YamlChangeSetHelper {
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private ExecutorService executorService;
  @Inject private YamlGitService yamlGitService;
  @Inject private AppService appService;

  public void queueApplicationUpdateYamlChangeAsync(Application savedApp, Application updatedApp) {
    executorService.submit(() -> {
      if (!savedApp.getName().equals(updatedApp.getName())) {
        queueMoveApplicationYamlChange(savedApp, updatedApp);
      } else {
        queueApplicationYamlChange(
            updatedApp.getAccountId(), entityUpdateService.getAppGitSyncFile(updatedApp, ChangeType.MODIFY));
      }
    });
  }

  public void queueApplicationYamlChangeAsync(Application app, ChangeType changeType) {
    executorService.submit(
        () -> queueApplicationYamlChange(app.getAccountId(), entityUpdateService.getAppGitSyncFile(app, changeType)));
  }

  private void queueApplicationYamlChange(String accountId, GitFileChange gitFileChange) {
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      yamlChangeSetService.queueChangeSet(ygs, Arrays.asList(gitFileChange));
    }
  }

  private void queueMoveApplicationYamlChange(Application oldApp, Application newApp) {
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
      yamlChangeSetService.queueChangeSet(ygs, changeSet);
    }
  }

  public void queueEnvironmentUpdateYamlChangeAsync(Environment savedEnvironment, Environment updatedEnvironment) {
    executorService.submit(() -> {
      if (!savedEnvironment.getName().equals(updatedEnvironment.getName())) {
        queueMoveEnvironmentChange(savedEnvironment, updatedEnvironment);
      } else {
        queueEnvironmentYamlChangeSet(updatedEnvironment, ChangeType.MODIFY);
      }
    });
  }

  public void queueEnvironmentYamlChangeAsync(Environment savedEnvironment, ChangeType changeType) {
    executorService.submit(() -> queueEnvironmentYamlChangeSet(savedEnvironment, changeType));
  }

  private void queueMoveEnvironmentChange(Environment oldEnv, Environment newEnv) {
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
      yamlChangeSetService.queueChangeSet(ygs, changeSet);
    }
  }

  private void queueEnvironmentYamlChangeSet(Environment environment, ChangeType crudType) {
    // check whether we need to push changes (through git sync)
    String accountId = appService.getAccountIdByAppId(environment.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();
      changeSet.add(entityUpdateService.getEnvironmentGitSyncFile(accountId, environment, crudType));
      yamlChangeSetService.queueChangeSet(ygs, changeSet);
    }
  }

  public void queueServiceUpdateYamlChangeAsync(Service service, Service savedService, Service updatedService) {
    executorService.submit(() -> {
      if (!savedService.getName().equals(updatedService.getName())) { // Service name changed
        queueMoveServiceChange(savedService, service);
      } else {
        queueServiceYamlChangeSet(updatedService, ChangeType.MODIFY);
      }
    });
  }

  public void queueServiceYamlChangeAsync(Service finalSavedService, ChangeType add) {
    executorService.submit(() -> queueServiceYamlChangeSet(finalSavedService, add));
  }

  private void queueMoveServiceChange(Service oldService, Service newService) {
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
      yamlChangeSetService.queueChangeSet(ygs, changeSet);
    }
  }

  private void queueServiceYamlChangeSet(Service service, ChangeType crudType) {
    // check whether we need to push changes (through git sync)
    String accountId = appService.getAccountIdByAppId(service.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      List<GitFileChange> changeSet = new ArrayList<>();

      changeSet.add(entityUpdateService.getServiceGitSyncFile(accountId, service, crudType));
      if (crudType.equals(ChangeType.ADD)) {
        service.getServiceCommands().forEach(serviceCommand
            -> changeSet.add(
                entityUpdateService.getCommandGitSyncFile(accountId, service, serviceCommand, ChangeType.ADD)));
      }
      yamlChangeSetService.queueChangeSet(ygs, changeSet);
    }
  }
}
