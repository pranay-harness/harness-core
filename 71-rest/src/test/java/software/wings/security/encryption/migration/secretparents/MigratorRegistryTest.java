package software.wings.security.encryption.migration.secretparents;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.security.encryption.migration.secretparents.migrators.ConfigFileMigrator;
import software.wings.security.encryption.migration.secretparents.migrators.DirectInfraMappingMigrator;
import software.wings.security.encryption.migration.secretparents.migrators.SecretManagerConfigMigrator;
import software.wings.security.encryption.migration.secretparents.migrators.ServiceVariableMigrator;
import software.wings.security.encryption.migration.secretparents.migrators.SettingAttributeMigrator;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Optional;

public class MigratorRegistryTest extends WingsBaseTest {
  @Mock SettingAttributeMigrator settingAttributeMigrator;
  @Mock SecretManagerConfigMigrator secretManagerConfigMigrator;
  @Mock ServiceVariableMigrator serviceVariableMigrator;
  @Mock ConfigFileMigrator configFileMigrator;
  @Mock DirectInfraMappingMigrator directInfraMappingMigrator;
  @Inject @InjectMocks MigratorRegistry migratorRegistry;

  @Before
  public void setup() {
    initMocks(true);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public <T extends PersistentEntity & UuidAware> void testGetMigrator_shouldPass() {
    SettingVariableTypes type = SettingVariableTypes.AWS;
    Optional<SecretsMigrator<T>> secretsMigrator = migratorRegistry.getMigrator(type);
    assertThat(secretsMigrator.isPresent()).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public <T extends PersistentEntity & UuidAware> void testGetMigrator_shouldFail() {
    SettingVariableTypes type = SettingVariableTypes.PHYSICAL_DATA_CENTER;
    Optional<SecretsMigrator<T>> secretsMigrator = migratorRegistry.getMigrator(type);
    assertThat(secretsMigrator.isPresent()).isFalse();
  }
}
