package io.harness.ngmigration.service;

import io.harness.pms.yaml.ParameterField;

import org.apache.commons.lang3.StringUtils;

public class MigratorUtility {
  public static String generateIdentifier(String name) {
    return name.trim().toLowerCase().replaceAll("[^A-Za-z0-9]", "");
  }

  public static ParameterField<String> getParameterField(String value) {
    if (StringUtils.isBlank(value)) {
      return ParameterField.createValueField("");
    }
    return ParameterField.createValueField(value);
  }
}
