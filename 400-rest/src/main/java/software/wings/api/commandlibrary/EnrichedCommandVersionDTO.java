/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
