/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.yaml.templatelibrary;

import static software.wings.common.TemplateConstants.CUSTOM_DEPLOYMENT_TYPE;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(CUSTOM_DEPLOYMENT_TYPE)
@JsonPropertyOrder({"harnessApiVersion"})
public class CustomDeploymentTypeTemplateYaml extends TemplateLibraryYaml {
  private String fetchInstanceScript;
  private String hostObjectArrayPath;
  private Map<String, String> hostAttributes;

  @Builder
  public CustomDeploymentTypeTemplateYaml(String type, String harnessApiVersion, String description,
      List<TemplateVariableYaml> templateVariableYamlList, String fetchInstanceScript, String hostObjectArrayPath,
      Map<String, String> hostAttributes) {
    super(type, harnessApiVersion, description, templateVariableYamlList);
    this.fetchInstanceScript = fetchInstanceScript;
    this.hostObjectArrayPath = hostObjectArrayPath;
    this.hostAttributes = hostAttributes;
  }
}
