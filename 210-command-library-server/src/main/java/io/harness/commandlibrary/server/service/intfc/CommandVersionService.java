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
