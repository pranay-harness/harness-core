package io.harness.commandlibrary.server.service.impl;

import static io.harness.commandlibrary.server.common.CommandUtils.populateCommandDTO;
import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.commandlibrary.server.service.intfc.CommandService;
import io.harness.commandlibrary.server.service.intfc.CommandVersionService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.commandlibrary.CommandEntity;
import software.wings.beans.commandlibrary.CommandEntity.CommandEntityKeys;
import software.wings.beans.commandlibrary.CommandVersionEntity;
import software.wings.dl.WingsPersistence;

import java.util.List;

@Singleton
@Slf4j
public class CommandServiceImpl implements CommandService {
  private final WingsPersistence wingsPersistence;
  private final CommandVersionService commandVersionService;

  @Inject
  public CommandServiceImpl(WingsPersistence wingsPersistence, CommandVersionService commandVersionService) {
    this.wingsPersistence = wingsPersistence;
    this.commandVersionService = commandVersionService;
  }

  @Override
  public CommandDTO getCommandDetails(String commandStoreName, String commandName) {
    final CommandEntity commandEntity = getCommandEntity(commandStoreName, commandName);
    if (commandEntity != null) {
      final CommandVersionEntity commandVersionEntity = commandVersionService.getCommandVersionEntity(
          commandStoreName, commandName, commandEntity.getLatestVersion());
      final List<CommandVersionEntity> allVersionEntityList =
          commandVersionService.getAllVersionEntitiesForCommand(commandStoreName, commandName);
      return populateCommandDTO(CommandDTO.builder(), commandEntity, commandVersionEntity, allVersionEntityList)
          .build();
    }
    return null;
  }

  private CommandEntity getCommandEntity(String commandStoreName, String commandName) {
    return wingsPersistence.createQuery(CommandEntity.class)
        .filter(CommandEntityKeys.commandStoreName, commandStoreName)
        .filter(CommandEntityKeys.name, commandName)
        .get();
  }

  @Override
  public PageResponse<CommandEntity> listCommandEntity(PageRequest<CommandEntity> pageRequest) {
    return wingsPersistence.query(CommandEntity.class, pageRequest, excludeAuthority);
  }

  @Override
  public String save(CommandEntity commandEntity) {
    return wingsPersistence.save(commandEntity);
  }

  @Override
  public CommandEntity getEntityById(String commandId) {
    return wingsPersistence.get(CommandEntity.class, commandId);
  }
}
