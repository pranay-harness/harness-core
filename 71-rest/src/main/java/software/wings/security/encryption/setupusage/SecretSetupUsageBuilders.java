package software.wings.security.encryption.setupusage;

import lombok.Getter;

public enum SecretSetupUsageBuilders {
  SETTING_ATTRIBUTE_SETUP_USAGE_BUILDER,
  SERVICE_VARIABLE_SETUP_USAGE_BUILDER,
  SECRET_MANAGER_CONFIG_SETUP_USAGE_BUILDER,
  CONFIG_FILE_SETUP_USAGE_BUILDER;

  @Getter private String name;

  SecretSetupUsageBuilders() {
    this.name = name();
  }
}
