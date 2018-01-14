package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Service;
import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by peeyushaggarwal on 11/17/16.
 */
@Singleton
public class CommandServiceImpl implements CommandService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private AppService appService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private ExecutorService executorService;

  @Override
  public Command getCommand(String appId, String originEntityId, int version) {
    return wingsPersistence.executeGetOneQuery(wingsPersistence.createQuery(Command.class)
                                                   .field("appId")
                                                   .equal(appId)
                                                   .field("originEntityId")
                                                   .equal(originEntityId)
                                                   .field("version")
                                                   .equal(version));
  }

  @Override
  public ServiceCommand getServiceCommand(String appId, String serviceCommandId) {
    return wingsPersistence.get(ServiceCommand.class, appId, serviceCommandId);
  }

  @Override
  public ServiceCommand getServiceCommandByName(String appId, String serviceId, String serviceCommandName) {
    return wingsPersistence.executeGetOneQuery(wingsPersistence.createQuery(ServiceCommand.class)
                                                   .field("appId")
                                                   .equal(appId)
                                                   .field("serviceId")
                                                   .equal(serviceId)
                                                   .field("name")
                                                   .equal(serviceCommandName));
  }

  @Override
  public Command save(Command command, boolean isDefaultCommand, boolean pushToYaml) {
    Command savedCommand = wingsPersistence.saveAndGet(Command.class, command);

    if (savedCommand != null) {
      if ((isDefaultCommand && pushToYaml) || (!isDefaultCommand)) {
        String accountId = appService.getAccountIdByAppId(command.getAppId());
        String serviceCommandId = command.getOriginEntityId();
        ServiceCommand serviceCommand = getServiceCommand(command.getAppId(), serviceCommandId);
        Service service = serviceResourceService.get(serviceCommand.getAppId(), serviceCommand.getServiceId());

        executorService.submit(() -> {
          if (!isDefaultCommand) { // Don't do yaml generation for default commands. We group them with service
            YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
            if (ygs != null) {
              List<GitFileChange> changeSet = new ArrayList<>();
              changeSet.add(
                  entityUpdateService.getCommandGitSyncFile(accountId, service, serviceCommand, ChangeType.ADD));
              yamlChangeSetService.saveChangeSet(ygs, changeSet);
            }
          }
        });
      }
    }
    return savedCommand;
  }

  @Override
  public Command update(Command command) {
    // check whether we need to push changes (through git sync)
    String accountId = appService.getAccountIdByAppId(command.getAppId());
    String serviceCommandId = command.getOriginEntityId();
    ServiceCommand serviceCommand = getServiceCommand(command.getAppId(), serviceCommandId);
    Service service = serviceResourceService.get(serviceCommand.getAppId(), serviceCommand.getServiceId());
    executorService.submit(() -> {
      YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
      if (ygs != null) {
        List<GitFileChange> changeSet = new ArrayList<>();
        changeSet.add(entityUpdateService.getCommandGitSyncFile(accountId, service, serviceCommand, ChangeType.MODIFY));
        yamlChangeSetService.saveChangeSet(ygs, changeSet);
      }
    });
    return wingsPersistence.saveAndGet(Command.class, command);
  }
}
