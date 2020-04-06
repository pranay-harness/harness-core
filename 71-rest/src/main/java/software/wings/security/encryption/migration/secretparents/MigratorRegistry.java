package software.wings.security.encryption.migration.secretparents;

import static software.wings.security.encryption.migration.secretparents.Migrators.CONFIG_FILE_MIGRATOR;
import static software.wings.security.encryption.migration.secretparents.Migrators.DIRECT_INFRA_MAPPING_MIGRATOR;
import static software.wings.security.encryption.migration.secretparents.Migrators.SECRET_MANAGER_CONFIG_MIGRATOR;
import static software.wings.security.encryption.migration.secretparents.Migrators.SERVICE_VARIABLE_MIGRATOR;
import static software.wings.security.encryption.migration.secretparents.Migrators.SETTING_ATTRIBUTE_MIGRATOR;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Singleton
class MigratorRegistry {
  @Inject private Injector injector;
  private final Map<SettingVariableTypes, Migrators> registeredMigrators = new EnumMap<>(SettingVariableTypes.class);

  @Inject
  MigratorRegistry() {
    registeredMigrators.put(SettingVariableTypes.AWS, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.AZURE, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.GCP, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.SCALYR, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.KUBERNETES, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.KUBERNETES_CLUSTER, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.PCF, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.SPOT_INST, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.SMTP, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.JENKINS, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.BAMBOO, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.SPLUNK, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.ELK, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.LOGZ, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.SUMO, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.APP_DYNAMICS, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.INSTANA, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.NEW_RELIC, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.DYNA_TRACE, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.BUG_SNAG, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.DATA_DOG, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.ELB, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.DOCKER, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.ECR, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.NEXUS, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.ARTIFACTORY, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.GIT, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.SMB, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.JIRA, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.SFTP, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.SERVICENOW, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.HTTP_HELM_REPO, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.AZURE_ARTIFACTS_PAT, SETTING_ATTRIBUTE_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.YAML_GIT_SYNC, SETTING_ATTRIBUTE_MIGRATOR);

    registeredMigrators.put(SettingVariableTypes.AWS_SECRETS_MANAGER, SECRET_MANAGER_CONFIG_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.AZURE_VAULT, SECRET_MANAGER_CONFIG_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.CYBERARK, SECRET_MANAGER_CONFIG_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.GCP_KMS, SECRET_MANAGER_CONFIG_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.KMS, SECRET_MANAGER_CONFIG_MIGRATOR);
    registeredMigrators.put(SettingVariableTypes.VAULT, SECRET_MANAGER_CONFIG_MIGRATOR);

    registeredMigrators.put(SettingVariableTypes.SECRET_TEXT, SERVICE_VARIABLE_MIGRATOR);

    registeredMigrators.put(SettingVariableTypes.CONFIG_FILE, CONFIG_FILE_MIGRATOR);

    registeredMigrators.put(SettingVariableTypes.DIRECT, DIRECT_INFRA_MAPPING_MIGRATOR);
  }

  <T extends PersistentEntity& UuidAware> Optional<SecretsMigrator<T>> getMigrator(SettingVariableTypes type) {
    return Optional.ofNullable(registeredMigrators.get(type))
        .flatMap(migrator
            -> Optional.of(injector.getInstance(Key.get(SecretsMigrator.class, Names.named(migrator.getName())))));
  }
}
