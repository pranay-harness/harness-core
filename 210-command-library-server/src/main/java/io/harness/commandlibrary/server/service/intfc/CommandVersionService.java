/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.commandlibrary.server.service.intfc;

import io.harness.commandlibrary.server.beans.CommandArchiveContext;

import software.wings.beans.commandlibrary.CommandVersionEntity;

import java.util.List;
import java.util.Optional;

public interface CommandVersionService {
  Optional<CommandVersionEntity> getCommandVersionEntity(String commandStoreName, String commandName, String version);

  List<CommandVersionEntity> getAllVersionEntitiesForCommand(String commandStoreName, String commandName);

  String save(CommandVersionEntity commandVersionEntity);

  Optional<CommandVersionEntity> getEntityById(String commandVersionId);

  String createNewVersionFromArchive(CommandArchiveContext commandArchiveContext);
}
