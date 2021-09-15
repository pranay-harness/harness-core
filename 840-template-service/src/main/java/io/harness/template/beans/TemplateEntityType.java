/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.template.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.template.beans.TemplateEntityConstants.STEP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

@OwnedBy(CDC)
public enum TemplateEntityType {
  @JsonProperty(STEP) STEP_TEMPLATE(STEP);

  private final String yamlType;

  TemplateEntityType(String yamlType) {
    this.yamlType = yamlType;
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
}
