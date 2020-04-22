package io.harness.commandlibrary.server.service.intfc;

import software.wings.beans.commandlibrary.CommandVersionEntity;

import java.util.List;

public interface CommandVersionService {
  CommandVersionEntity getCommandVersionEntity(String commandStoreName, String commandName, String version);

  List<CommandVersionEntity> getAllVersionEntitiesForCommand(String commandStoreName, String commandName);

  String save(CommandVersionEntity commandVersionEntity);

  CommandVersionEntity getEntityById(String commandVersionId);
}
