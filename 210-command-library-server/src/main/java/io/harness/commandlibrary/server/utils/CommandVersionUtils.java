/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
