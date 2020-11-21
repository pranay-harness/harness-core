package migrations.all;

import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.security.encryption.EncryptionType.VAULT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.VaultConfig;

import com.google.inject.Inject;
import java.time.Duration;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VaultConfigRenewalIntervalMigrationTest extends WingsBaseTest {
  @Inject private VaultConfigRenewalIntervalMigration vaultConfigRenewalIntervalMigration;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testMigrate_renewalIntervalIsZero() {
    VaultConfig vaultConfig =
        VaultConfig.builder().name("test").vaultUrl("test.com").authToken("authToken").renewIntervalHours(0).build();
    vaultConfig.setEncryptionType(VAULT);
    String configId = wingsPersistence.save(vaultConfig);
    vaultConfigRenewalIntervalMigration.migrate();
    VaultConfig returnedVaultConfig = wingsPersistence.get(VaultConfig.class, configId);
    assertThat(returnedVaultConfig.getRenewalInterval()).isEqualTo(0);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testMigrate_renewalIntervalIsNonZero() {
    VaultConfig vaultConfig =
        VaultConfig.builder().name("test").vaultUrl("test.com").authToken("authToken").renewIntervalHours(2).build();
    vaultConfig.setEncryptionType(VAULT);
    String configId = wingsPersistence.save(vaultConfig);
    vaultConfigRenewalIntervalMigration.migrate();
    VaultConfig returnedVaultConfig = wingsPersistence.get(VaultConfig.class, configId);
    assertThat(returnedVaultConfig.getRenewalInterval()).isEqualTo(Duration.ofHours(2).toMinutes());
  }
}
