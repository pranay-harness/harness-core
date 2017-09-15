package software.wings.service.impl;

import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Singleton;

import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.CommandService;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 11/17/16.
 */
@Singleton
public class CommandServiceImpl implements CommandService {
  @Inject private WingsPersistence wingsPersistence;

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
  public Command save(Command command) {
    return wingsPersistence.saveAndGet(Command.class, command);
  }

  @Override
  public Command update(Command command) {
    return wingsPersistence.saveAndGet(Command.class, command);
  }
}
