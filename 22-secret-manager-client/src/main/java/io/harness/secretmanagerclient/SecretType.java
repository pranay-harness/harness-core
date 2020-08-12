package io.harness.secretmanagerclient;

import software.wings.settings.SettingVariableTypes;

public enum SecretType {
  SecretFile,
  SecretText;

  public static SettingVariableTypes toSettingVariableType(SecretType secretType) {
    if (secretType == SecretType.SecretFile) {
      return SettingVariableTypes.CONFIG_FILE;
    } else if (secretType == SecretType.SecretText) {
      return SettingVariableTypes.SECRET_TEXT;
    }
    throw new IllegalArgumentException("SecretType " + secretType + " cannot be converted to settingVariableType");
  }

  public static SecretType fromSettingVariableType(SettingVariableTypes type) {
    if (type == SettingVariableTypes.SECRET_TEXT) {
      return SecretType.SecretText;
    } else if (type == SettingVariableTypes.CONFIG_FILE) {
      return SecretType.SecretFile;
    }
    throw new IllegalArgumentException("Setting variable type " + type + " cannot be converted to SecretType");
  }
}
