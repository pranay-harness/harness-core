package software.wings.service.impl;

import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Singleton;

import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.PageRequest;
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
import javax.inject.Inject;

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
    return wingsPersistence.get(Command.class,
        aPageRequest()
            .addFilter(aSearchFilter().withField("appId", EQ, appId).build())
            .addFilter(aSearchFilter().withField("originEntityId", EQ, originEntityId).build())
            .addFilter(aSearchFilter().withField("version", EQ, version).build())
            .build());
  }

  @Override
  public List<Command> getCommandList(String appId, String originEntityId) {
    PageRequest<Command> pageRequest =
        aPageRequest()
            .addFilter(aSearchFilter().withField("appId", Operator.EQ, appId).build())
            .addFilter(aSearchFilter().withField("originEntityId", Operator.EQ, originEntityId).build())
            .build();
    return wingsPersistence.query(Command.class, pageRequest).getResponse();
  }

  @Override
  public ServiceCommand getServiceCommand(String appId, String serviceCommandId) {
    return wingsPersistence.get(ServiceCommand.class,
        aPageRequest()
            .addFilter(aSearchFilter().withField("appId", EQ, appId).build())
            .addFilter(aSearchFilter().withField("uuid", EQ, serviceCommandId).build())
            .build());
  }

  @Override
  public ServiceCommand getServiceCommandByName(String appId, String serviceId, String serviceCommandName) {
    return wingsPersistence.get(ServiceCommand.class,
        aPageRequest()
            .addFilter(aSearchFilter().withField("appId", EQ, appId).build())
            .addFilter(aSearchFilter().withField("serviceId", EQ, serviceId).build())
            .addFilter(aSearchFilter().withField("name", EQ, serviceCommandName).build())
            .build());
  }

  @Override
  public Command save(Command command, boolean isDefaultCommand) {
    Command savedCommand = wingsPersistence.saveAndGet(Command.class, command);

    if (savedCommand != null) {
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
            yamlChangeSetService.queueChangeSet(ygs, changeSet);
          }
        }
      });
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
        yamlChangeSetService.queueChangeSet(ygs, changeSet);
      }
    });
    return wingsPersistence.saveAndGet(Command.class, command);
  }
}
