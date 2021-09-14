/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.commandlibrary.server.utils;

import software.wings.api.commandlibrary.EnrichedCommandVersionDTO.EnrichedCommandVersionDTOBuilder;
import software.wings.beans.commandlibrary.CommandVersionEntity;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CommandVersionUtils {
  public static EnrichedCommandVersionDTOBuilder populateCommandVersionDTO(
      EnrichedCommandVersionDTOBuilder builder, CommandVersionEntity commandVersionEntity) {
    if (commandVersionEntity != null) {
      builder.commandName(commandVersionEntity.getCommandName())
          .commandStoreName(commandVersionEntity.getCommandStoreName())
          .description(commandVersionEntity.getDescription())
          .version(commandVersionEntity.getVersion())
          .yamlContent(commandVersionEntity.getYamlContent())
          .templateObject(commandVersionEntity.getTemplateObject())
          .tags(commandVersionEntity.getTags())
          .repoUrl(commandVersionEntity.getRepoUrl())
          .variables(commandVersionEntity.getVariables());
    }
    return builder;
  }
}
