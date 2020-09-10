package io.harness.utils;

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
