/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api.commandlibrary;

import io.harness.commandlibrary.api.dto.CommandVersionDTO;

import software.wings.beans.Variable;
import software.wings.beans.template.BaseTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "EnrichedCommandVersionDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrichedCommandVersionDTO extends CommandVersionDTO {
  BaseTemplate templateObject;
  List<Variable> variables;
  String yamlContent;

  @Builder
  public EnrichedCommandVersionDTO(String commandName, String commandStoreName, String version, String description,
      Set<String> tags, String repoUrl, BaseTemplate templateObject, List<Variable> variables, String yamlContent) {
    super(commandName, commandStoreName, version, description, tags, repoUrl);
    this.templateObject = templateObject;
    this.variables = variables;
    this.yamlContent = yamlContent;
  }
}
