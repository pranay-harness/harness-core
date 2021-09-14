/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.secretmanagerclient;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.settings.SettingVariableTypes;

@OwnedBy(PL)
public enum SecretType {
  SecretFile,
  SecretText,
  SSHKey;

  public static SettingVariableTypes toSettingVariableType(SecretType secretType) {
    if (secretType == null) {
      return null;
    }
    switch (secretType) {
      case SecretFile:
        return SettingVariableTypes.CONFIG_FILE;
      case SecretText:
        return SettingVariableTypes.SECRET_TEXT;
      case SSHKey:
        return SettingVariableTypes.SSH_SESSION_CONFIG;
      default:
        throw new IllegalArgumentException("SecretType " + secretType + " cannot be converted to settingVariableType");
    }
  }

  public static SecretType fromSettingVariableType(SettingVariableTypes type) {
    if (type == null) {
      return null;
    }
    switch (type) {
      case SECRET_TEXT:
        return SecretType.SecretText;
      case CONFIG_FILE:
        return SecretType.SecretFile;
      case SSH_SESSION_CONFIG:
        return SecretType.SSHKey;
      default:
        throw new IllegalArgumentException("Setting variable type " + type + " cannot be converted to SecretType");
    }
  }
}
