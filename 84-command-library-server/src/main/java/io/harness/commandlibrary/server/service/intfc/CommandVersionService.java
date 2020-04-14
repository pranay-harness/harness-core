package io.harness.commandlibrary.server.service.intfc;

import software.wings.beans.commandlibrary.CommandVersionEntity;

import java.util.List;

public interface CommandVersionService {
  CommandVersionEntity getCommandVersionEntity(String commandStoreId, String commandId, String version);

  List<CommandVersionEntity> getAllVersionEntitiesForCommand(String commandStoreId, String commandId);

  String save(CommandVersionEntity commandVersionEntity);

  CommandVersionEntity getEntityById(String commandVersionId);
}
