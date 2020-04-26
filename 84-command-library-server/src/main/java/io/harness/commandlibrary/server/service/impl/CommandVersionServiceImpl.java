package io.harness.commandlibrary.server.service.impl;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.commandlibrary.server.service.intfc.CommandVersionService;
import io.harness.persistence.HQuery;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.commandlibrary.CommandVersionEntity;
import software.wings.dl.WingsPersistence;

import java.util.List;

@Singleton
@Slf4j
public class CommandVersionServiceImpl implements CommandVersionService {
  private WingsPersistence wingsPersistence;

  @Inject
  public CommandVersionServiceImpl(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public CommandVersionEntity getCommandVersionEntity(String commandStoreName, String commandName, String version) {
    return wingsPersistence.createQuery(CommandVersionEntity.class)
        .filter(CommandVersionEntity.CommandVersionsKeys.commandStoreName, commandStoreName)
        .filter(CommandVersionEntity.CommandVersionsKeys.commandName, commandName)
        .filter(CommandVersionEntity.CommandVersionsKeys.version, version)
        .get();
  }

  @Override
  public List<CommandVersionEntity> getAllVersionEntitiesForCommand(String commandStoreName, String commandName) {
    return emptyIfNull(wingsPersistence.createQuery(CommandVersionEntity.class, HQuery.excludeAuthority)
                           .filter(CommandVersionEntity.CommandVersionsKeys.commandStoreName, commandStoreName)
                           .filter(CommandVersionEntity.CommandVersionsKeys.commandName, commandName)
                           .order(Sort.descending(CommandVersionEntity.CommandVersionsKeys.createdAt))
                           .asList());
  }

  @Override
  public String save(CommandVersionEntity commandVersionEntity) {
    return wingsPersistence.save(commandVersionEntity);
  }

  @Override
  public CommandVersionEntity getEntityById(String commandVersionId) {
    return wingsPersistence.get(CommandVersionEntity.class, commandVersionId);
  }
}
