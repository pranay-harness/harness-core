package io.harness.template.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.template.beans.TemplateEntityConstants.ALL;
import static io.harness.template.beans.TemplateEntityConstants.LAST_UPDATES_TEMPLATE;
import static io.harness.template.beans.TemplateEntityConstants.STABLE_TEMPLATE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

@OwnedBy(CDC)
public enum TemplateListType {
  @JsonProperty(STABLE_TEMPLATE) STABLE_TEMPLATE_TYPE(STABLE_TEMPLATE),
  @JsonProperty(LAST_UPDATES_TEMPLATE) LAST_UPDATED_TEMPLATE_TYPE(LAST_UPDATES_TEMPLATE),
  @JsonProperty(ALL) ALL_TEMPLATE_TYPE(ALL);

  private final String yamlType;

  TemplateListType(String yamlType) {
    this.yamlType = yamlType;
  }

  @JsonCreator
  public static TemplateListType getTemplateType(@JsonProperty("type") String yamlType) {
    for (TemplateListType value : TemplateListType.values()) {
      if (value.yamlType.equalsIgnoreCase(yamlType)) {
        return value;
      }
    }
    throw new IllegalArgumentException(String.format(
        "Invalid value:%s, the expected values are: %s", yamlType, Arrays.toString(TemplateListType.values())));
  }

  @Override
  @JsonValue
  public String toString() {
    return this.yamlType;
  }
}
