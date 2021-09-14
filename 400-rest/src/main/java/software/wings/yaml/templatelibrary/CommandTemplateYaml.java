/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.yaml.templatelibrary;

import static software.wings.common.TemplateConstants.SSH;

import software.wings.beans.command.AbstractCommandUnit;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(SSH)
@JsonPropertyOrder({"harnessApiVersion"})
public class CommandTemplateYaml extends TemplateLibraryYaml {
  private String commandUnitType;
  private List<AbstractCommandUnit.Yaml> commandUnits = new ArrayList<>();

  @Builder
  public CommandTemplateYaml(String type, String harnessApiVersion, String description,
      List<TemplateVariableYaml> templateVariableYamlList, String commandUnitType,
      List<AbstractCommandUnit.Yaml> commandUnits) {
    super(type, harnessApiVersion, description, templateVariableYamlList);
    this.commandUnitType = commandUnitType;
    this.commandUnits = commandUnits;
  }
}
