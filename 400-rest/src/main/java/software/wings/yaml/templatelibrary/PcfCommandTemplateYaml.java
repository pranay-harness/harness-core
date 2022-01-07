/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.common.TemplateConstants.PCF_PLUGIN;

import io.harness.annotations.dev.OwnedBy;

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
@JsonTypeName(PCF_PLUGIN)
@JsonPropertyOrder({"harnessApiVersion"})
@OwnedBy(CDP)
public class PcfCommandTemplateYaml extends TemplateLibraryYaml {
  private String scriptString;
  private Integer timeoutIntervalInMinutes = 5;

  @Builder
  public PcfCommandTemplateYaml(String type, String harnessApiVersion, String description, String scriptString,
      Integer timeoutIntervalInMinutes, List<TemplateVariableYaml> templateVariableYamlList) {
    super(type, harnessApiVersion, description, templateVariableYamlList);
    this.scriptString = scriptString;
    this.timeoutIntervalInMinutes = timeoutIntervalInMinutes;
  }
}
