/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.templatelibrary;

import static software.wings.common.TemplateConstants.SHELL_SCRIPT;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(SHELL_SCRIPT)
@JsonPropertyOrder({"harnessApiVersion"})
public class ShellScriptTemplateYaml extends TemplateLibraryYaml {
  private String scriptType;
  private String scriptString;
  private String outputVars;
  private String secretOutputVars;
  private int timeoutMillis = 600000;

  @Builder
  public ShellScriptTemplateYaml(String type, String harnessApiVersion, String description, String scriptType,
      String scriptString, String outputVars, String secretOutputVars, int timeOutMillis,
      List<TemplateVariableYaml> templateVariableYamlList) {
    super(type, harnessApiVersion, description, templateVariableYamlList);
    this.outputVars = outputVars;
    this.secretOutputVars = secretOutputVars;
    this.scriptString = scriptString;
    this.scriptType = scriptType;
    this.timeoutMillis = timeOutMillis;
  }
}
