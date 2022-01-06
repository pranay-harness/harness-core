/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
