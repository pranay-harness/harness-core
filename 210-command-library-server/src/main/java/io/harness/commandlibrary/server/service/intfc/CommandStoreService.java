package io.harness.commandlibrary.server.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.commandlibrary.api.dto.CommandStoreDTO;
import software.wings.beans.commandlibrary.CommandEntity;

import java.util.List;
import java.util.Optional;

public interface CommandStoreService {
  PageResponse<CommandDTO> listCommandsForStore(
      String commandStoreName, PageRequest<CommandEntity> pageRequest, String category);

  List<CommandStoreDTO> getCommandStores();

  Optional<CommandStoreDTO> getStoreByName(String name);
}
