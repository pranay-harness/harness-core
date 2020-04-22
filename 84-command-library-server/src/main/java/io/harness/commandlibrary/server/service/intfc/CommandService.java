package io.harness.commandlibrary.server.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.commandlibrary.api.dto.CommandDTO;
import software.wings.beans.commandlibrary.CommandEntity;

public interface CommandService {
  CommandDTO getCommandDetails(String commandStoreName, String commandName);

  PageResponse<CommandEntity> listCommandEntity(PageRequest<CommandEntity> pageRequest);

  String save(CommandEntity commandEntity);

  CommandEntity getEntityById(String commandId);
}
