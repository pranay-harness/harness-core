package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Event.Type;
import software.wings.beans.Service;
import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlPushService;

@Singleton
public class CommandServiceImpl implements CommandService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private AppService appService;
  @Inject private YamlPushService yamlPushService;

  @Override
  public Command getCommand(String appId, String originEntityId, int version) {
    return wingsPersistence.createQuery(Command.class)
        .filter("appId", appId)
        .filter("originEntityId", originEntityId)
        .filter("version", version)
        .get();
  }

  @Override
  public ServiceCommand getServiceCommand(String appId, String serviceCommandId) {
    return wingsPersistence.get(ServiceCommand.class, appId, serviceCommandId);
  }

  @Override
  public ServiceCommand getServiceCommandByName(String appId, String serviceId, String serviceCommandName) {
    return wingsPersistence.createQuery(ServiceCommand.class)
        .filter("appId", appId)
        .filter("serviceId", serviceId)
        .filter("name", serviceCommandName)
        .get();
  }

  @Override
  public Command save(Command command, boolean pushToYaml) {
    Command savedCommand = wingsPersistence.saveAndGet(Command.class, command);
    if (savedCommand != null && pushToYaml) {
      ServiceCommand serviceCommand = getServiceCommand(command.getAppId(), command.getOriginEntityId());
      Service service = serviceResourceService.get(serviceCommand.getAppId(), serviceCommand.getServiceId());
      String accountId = appService.getAccountIdByAppId(command.getAppId());
      yamlPushService.pushYamlChangeSet(accountId, service, serviceCommand, Type.CREATE, command.isSyncFromGit());
    }
    return savedCommand;
  }

  @Override
  public Command update(Command command, boolean pushToYaml) {
    if (pushToYaml) {
      // check whether we need to push changes (through git sync)
      String accountId = appService.getAccountIdByAppId(command.getAppId());
      ServiceCommand serviceCommand = getServiceCommand(command.getAppId(), command.getOriginEntityId());
      Service service = serviceResourceService.get(serviceCommand.getAppId(), serviceCommand.getServiceId());
      yamlPushService.pushYamlChangeSet(accountId, service, serviceCommand, Type.UPDATE, command.isSyncFromGit());
    }
    return wingsPersistence.saveAndGet(Command.class, command);
  }
}
