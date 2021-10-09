package io.harness.ng.core.template;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.ng.core.template.TemplateEntityConstants.STAGE;
import static io.harness.ng.core.template.TemplateEntityConstants.STEP;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

@OwnedBy(CDC)
public enum TemplateEntityType {
  @JsonProperty(STEP) STEP_TEMPLATE(STEP),
  @JsonProperty(STAGE) STAGE_TEMPLATE(STAGE);

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
