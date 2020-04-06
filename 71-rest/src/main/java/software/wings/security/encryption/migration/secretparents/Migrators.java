package software.wings.security.encryption.migration.secretparents;

import lombok.Getter;

public enum Migrators {
  SETTING_ATTRIBUTE_MIGRATOR,
  SERVICE_VARIABLE_MIGRATOR,
  SECRET_MANAGER_CONFIG_MIGRATOR,
  CONFIG_FILE_MIGRATOR,
  DIRECT_INFRA_MAPPING_MIGRATOR;

  @Getter private String name;

  Migrators() {
    this.name = name();
  }
}
