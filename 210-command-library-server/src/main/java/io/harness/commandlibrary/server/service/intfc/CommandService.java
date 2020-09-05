package io.harness.commandlibrary.server.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.commandlibrary.server.beans.CommandManifest;
import software.wings.beans.commandlibrary.CommandEntity;

import java.util.Optional;

public interface CommandService {
  Optional<CommandDTO> getCommandDetails(String commandStoreName, String commandName);

  Optional<CommandEntity> getCommandEntity(String commandStoreName, String commandName);

  PageResponse<CommandEntity> listCommandEntity(PageRequest<CommandEntity> pageRequest);

  String save(CommandEntity commandEntity);

  CommandEntity saveAndGet(CommandEntity commandEntity);

  Optional<CommandEntity> getEntityById(String commandId);

  CommandEntity createFromManifest(String commandStoreName, CommandManifest manifest, String accountId);

  boolean isCommandTypeSupported(String commandStoreName, String commandType);
}
