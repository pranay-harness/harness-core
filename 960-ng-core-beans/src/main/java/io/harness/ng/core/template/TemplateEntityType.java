/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.template;

import static io.harness.NGCommonEntityConstants.IDENTIFIER_KEY;
import static io.harness.NGCommonEntityConstants.NAME_KEY;
import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.ng.core.template.TemplateEntityConstants.STAGE;
import static io.harness.ng.core.template.TemplateEntityConstants.STAGE_ROOT_FIELD;
import static io.harness.ng.core.template.TemplateEntityConstants.STEP;
import static io.harness.ng.core.template.TemplateEntityConstants.STEP_ROOT_FIELD;

import static java.util.Arrays.asList;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;

@OwnedBy(CDC)
public enum TemplateEntityType {
  @JsonProperty(STEP) STEP_TEMPLATE(STEP, STEP_ROOT_FIELD, asList(IDENTIFIER_KEY, NAME_KEY)),
  @JsonProperty(STAGE) STAGE_TEMPLATE(STAGE, STAGE_ROOT_FIELD, asList(IDENTIFIER_KEY, NAME_KEY));

  private final String yamlType;
  private String rootYamlName;
  private final List<String> yamlFieldKeys;

  TemplateEntityType(String yamlType, String rootYamlName, List<String> yamlFieldKeys) {
    this.yamlType = yamlType;
    this.rootYamlName = rootYamlName;
    this.yamlFieldKeys = yamlFieldKeys;
  }

  @JsonCreator
  public static TemplateEntityType getTemplateType(@JsonProperty("type") String yamlType) {
    for (TemplateEntityType value : TemplateEntityType.values()) {
      if (value.yamlType.equalsIgnoreCase(yamlType)) {
        return value;
      }
    }
    throw new IllegalArgumentException(String.format(
        "Invalid value:%s, the expected values are: %s", yamlType, Arrays.toString(TemplateEntityType.values())));
  }

  @Override
  @JsonValue
  public String toString() {
    return this.yamlType;
  }

  public String getRootYamlName() {
    return this.rootYamlName;
  }

  public List<String> getYamlFieldKeys() {
    return this.yamlFieldKeys;
  }
}
