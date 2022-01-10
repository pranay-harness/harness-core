/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
