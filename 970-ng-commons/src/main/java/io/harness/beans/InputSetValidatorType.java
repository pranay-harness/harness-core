package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PIPELINE)
public enum InputSetValidatorType {
  ALLOWED_VALUES("allowedValues"),
  REGEX("regex");

  private final String yamlName;

  InputSetValidatorType(String yamlName) {
    this.yamlName = yamlName;
  }

  public String getYamlName() {
    return yamlName;
  }
}
