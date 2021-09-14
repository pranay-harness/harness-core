/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.commandlibrary.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "CommandDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandDTO {
  String commandStoreName;
  String type;
  String name;
  String description;
  String imageUrl;
  CommandVersionDTO latestVersion;
  List<CommandVersionDTO> versionList;
  Set<String> tags;
  String repoUrl;
}
