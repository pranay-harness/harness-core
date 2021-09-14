/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.commandlibrary.server.service.impl;

import static io.harness.commandlibrary.server.utils.CommandUtils.populateCommandDTO;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.commandlibrary.server.beans.CommandManifest;
import io.harness.commandlibrary.server.beans.CommandType;
import io.harness.commandlibrary.server.service.intfc.CommandService;
import io.harness.commandlibrary.server.service.intfc.CommandVersionService;
import io.harness.exception.UnexpectedException;

import software.wings.beans.commandlibrary.CommandEntity;
import software.wings.beans.commandlibrary.CommandEntity.CommandEntityBuilder;
import software.wings.beans.commandlibrary.CommandEntity.CommandEntityKeys;
import software.wings.beans.commandlibrary.CommandVersionEntity;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

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
  public Optional<CommandDTO> getCommandDetails(String commandStoreName, String commandName) {
    return getCommandEntity(commandStoreName, commandName).map(this::getCommandDTOFromEntity);
  }

  private CommandDTO getCommandDTOFromEntity(CommandEntity commandEntity) {
    String commandStoreName = commandEntity.getCommandStoreName();
    String commandName = commandEntity.getName();
    final CommandVersionEntity commandVersionEntity =
        commandVersionService.getCommandVersionEntity(commandStoreName, commandName, commandEntity.getLatestVersion())
            .orElse(null);
    final List<CommandVersionEntity> allVersionEntityList =
        commandVersionService.getAllVersionEntitiesForCommand(commandStoreName, commandName);
    return populateCommandDTO(CommandDTO.builder(), commandEntity, commandVersionEntity, allVersionEntityList).build();
  }

  @Override
  public Optional<CommandEntity> getCommandEntity(String commandStoreName, String commandName) {
    return Optional.ofNullable(wingsPersistence.createQuery(CommandEntity.class)
                                   .filter(CommandEntityKeys.commandStoreName, commandStoreName)
                                   .filter(CommandEntityKeys.name, commandName)
                                   .get());
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
  public CommandEntity saveAndGet(CommandEntity commandEntity) {
    final String commandId = wingsPersistence.save(commandEntity);
    return getEntityById(commandId).orElseThrow(() -> new UnexpectedException("could not find command by id"));
  }

  @Override
  public Optional<CommandEntity> getEntityById(String commandId) {
    return Optional.ofNullable(wingsPersistence.get(CommandEntity.class, commandId));
  }

  @Override
  public CommandEntity createFromManifest(String commandStoreName, CommandManifest manifest, String accountId) {
    final CommandEntityBuilder commandBuilder = CommandEntity.builder().commandStoreName(commandStoreName);
    populateFromManifest(commandBuilder, manifest, accountId);
    return saveAndGet(commandBuilder.build());
  }

  @Override
  public boolean isCommandTypeSupported(String commandStoreName, String commandType) {
    try {
      CommandType.valueOf(commandType);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  private CommandEntityBuilder populateFromManifest(
      CommandEntityBuilder commandEntityBuilder, CommandManifest commandManifest, String accountId) {
    return commandEntityBuilder.name(commandManifest.getName())
        .tags(commandManifest.getTags())
        .description(commandManifest.getDescription())
        .createdByAccountId(accountId)
        .lastUpdatedByAccountId(accountId)
        .type(commandManifest.getType());
  }
}
