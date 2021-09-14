/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.commandlibrary.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "CommandVersionDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandVersionDTO {
  String commandName;
  String commandStoreName;
  String version;
  String description;
  Set<String> tags;
  String repoUrl;

  @Builder(builderMethodName = "newBuilder")
  public CommandVersionDTO(String commandName, String commandStoreName, String version, String description,
      Set<String> tags, String repoUrl) {
    this.commandName = commandName;
    this.commandStoreName = commandStoreName;
    this.version = version;
    this.description = description;
    this.tags = tags;
    this.repoUrl = repoUrl;
  }
}
