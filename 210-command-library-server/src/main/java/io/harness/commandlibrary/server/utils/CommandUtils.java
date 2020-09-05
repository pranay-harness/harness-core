package io.harness.commandlibrary.server.utils;

import static io.harness.commandlibrary.server.utils.CommandVersionUtils.populateCommandVersionDTO;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import io.harness.commandlibrary.api.dto.CommandDTO.CommandDTOBuilder;
import lombok.experimental.UtilityClass;
import software.wings.api.commandlibrary.EnrichedCommandVersionDTO;
import software.wings.beans.commandlibrary.CommandEntity;
import software.wings.beans.commandlibrary.CommandVersionEntity;

import java.util.List;

@UtilityClass
public class CommandUtils {
  public static CommandDTOBuilder populateCommandDTO(CommandDTOBuilder commandDTOBuilder, CommandEntity commandEntity,
      CommandVersionEntity latestCommandVersionEntity, List<CommandVersionEntity> allVersionList) {
    if (commandEntity != null) {
      commandDTOBuilder.name(commandEntity.getName())
          .commandStoreName(commandEntity.getCommandStoreName())
          .type(commandEntity.getType())
          .name(commandEntity.getName())
          .description(commandEntity.getDescription())
          .tags(commandEntity.getTags())
          .imageUrl(commandEntity.getImageUrl())
          .repoUrl(commandEntity.getRepoUrl())
          .latestVersion(latestCommandVersionEntity != null
                  ? populateCommandVersionDTO(EnrichedCommandVersionDTO.builder(), latestCommandVersionEntity).build()
                  : null)
          .versionList(emptyIfNull(allVersionList)
                           .stream()
                           .map(versionEntity
                               -> populateCommandVersionDTO(EnrichedCommandVersionDTO.builder(), versionEntity).build())
                           .collect(toList()));
    }
    return commandDTOBuilder;
  }
}
