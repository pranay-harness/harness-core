package software.wings.service.impl;

import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import software.wings.beans.command.Command;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.CommandService;

import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 11/17/16.
 */
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
  public Command save(Command command) {
    return wingsPersistence.saveAndGet(Command.class, command);
  }

  @Override
  public Command update(Command command) {
    return wingsPersistence.saveAndGet(Command.class, command);
  }
}
