/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.NameValuePair;
import software.wings.beans.TemplateExpression;
import software.wings.beans.TemplateExpression.Yaml;
import software.wings.yaml.BaseYamlWithType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rktummala on 10/26/17
 */
@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StepYaml extends BaseYamlWithType {
  private String name;
  private Map<String, Object> properties = new HashMap<>();
  private List<TemplateExpression.Yaml> templateExpressions;
  private String templateUri;
  private List<NameValuePair> templateVariables;

  @Builder
  public StepYaml(String type, String name, Map<String, Object> properties, List<Yaml> templateExpressions,
      String templateUri, List<NameValuePair> templateVariables) {
    super(type);
    this.name = name;
    this.properties = properties;
    this.templateExpressions = templateExpressions;
    this.templateUri = templateUri;
    this.templateVariables = templateVariables;
  }
}
