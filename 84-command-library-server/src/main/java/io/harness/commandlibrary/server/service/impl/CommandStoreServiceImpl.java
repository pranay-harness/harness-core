package io.harness.commandlibrary.server.service.impl;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.commandlibrary.server.common.CommandUtils.populateCommandDTO;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.commandlibrary.server.service.intfc.CommandService;
import io.harness.commandlibrary.server.service.intfc.CommandStoreService;
import io.harness.commandlibrary.server.service.intfc.CommandVersionService;
import io.harness.data.structure.EmptyPredicate;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.commandlibrary.CommandEntity;
import software.wings.beans.commandlibrary.CommandEntity.CommandEntityKeys;
import software.wings.beans.commandlibrary.CommandVersionEntity;

@Singleton
@Slf4j
public class CommandStoreServiceImpl implements CommandStoreService {
  private final CommandService commandService;
  private final CommandVersionService commandVersionService;

  @Inject
  public CommandStoreServiceImpl(CommandService commandService, CommandVersionService commandVersionService) {
    this.commandService = commandService;
    this.commandVersionService = commandVersionService;
  }

  @Override
  public PageResponse<CommandDTO> listCommandsForStore(
      String commandStoreName, PageRequest<CommandEntity> pageRequest, String category) {
    pageRequest.addFilter(SearchFilter.builder()
                              .fieldName(CommandEntityKeys.commandStoreName)
                              .op(EQ)
                              .fieldValues(new String[] {commandStoreName})
                              .build());
    if (EmptyPredicate.isNotEmpty(category)) {
      pageRequest.addFilter(SearchFilter.builder()
                                .fieldName(CommandEntityKeys.category)
                                .op(EQ)
                                .fieldValues(new String[] {category})
                                .build());
    }
    final PageResponse<CommandEntity> commandEntitiesPR = commandService.listCommandEntity(pageRequest);
    return convert(commandEntitiesPR);
  }

  private PageResponse<CommandDTO> convert(PageResponse<CommandEntity> commandEntitiesPR) {
    return aPageResponse()
        .withTotal(commandEntitiesPR.getTotal())
        .withOffset(commandEntitiesPR.getOffset())
        .withLimit(commandEntitiesPR.getLimit())
        .withResponse(
            emptyIfNull(commandEntitiesPR.getResponse()).stream().map(this ::convertToCommandDTO).collect(toList()))
        .build();
  }

  private CommandDTO convertToCommandDTO(CommandEntity commandEntity) {
    return populateCommandDTO(CommandDTO.builder(), commandEntity, getLatestVersionEntity(commandEntity), null).build();
  }

  private CommandVersionEntity getLatestVersionEntity(CommandEntity commandEntity) {
    return commandVersionService.getCommandVersionEntity(
        commandEntity.getCommandStoreName(), commandEntity.getName(), commandEntity.getLatestVersion());
  }
}
