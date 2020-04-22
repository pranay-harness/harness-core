package io.harness.commandlibrary.server.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.commandlibrary.api.dto.CommandDTO;
import software.wings.beans.commandlibrary.CommandEntity;

public interface CommandStoreService {
  PageResponse<CommandDTO> listCommandsForStore(
      String commandStoreName, PageRequest<CommandEntity> pageRequest, String category);
}
