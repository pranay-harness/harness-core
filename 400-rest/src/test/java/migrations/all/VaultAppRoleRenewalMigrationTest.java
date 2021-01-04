package migrations.all;

import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.security.encryption.EncryptionType.VAULT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.VaultConfig;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VaultAppRoleRenewalMigrationTest extends WingsBaseTest {
  @Inject private VaultAppRoleRenewalMigration vaultAppRoleRenewalMigration;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testMigration_shouldNotChangeAuthTokenRenewalInterval() {
    VaultConfig vaultConfig =
        VaultConfig.builder().name("test").vaultUrl("test.com").authToken("authToken").renewalInterval(0).build();
    vaultConfig.setEncryptionType(VAULT);
    String configId = wingsPersistence.save(vaultConfig);
    vaultAppRoleRenewalMigration.migrate();
    VaultConfig returnedVaultConfig = wingsPersistence.get(VaultConfig.class, configId);
    assertThat(returnedVaultConfig.getRenewalInterval()).isEqualTo(0);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testMigration_shouldNotChangeNonZeroRenewalInterval() {
    VaultConfig vaultConfig =
        VaultConfig.builder().name("test").vaultUrl("test.com").appRoleId("appRoleId").renewalInterval(10).build();
    vaultConfig.setEncryptionType(VAULT);
    String configId = wingsPersistence.save(vaultConfig);
    vaultAppRoleRenewalMigration.migrate();
    VaultConfig returnedVaultConfig = wingsPersistence.get(VaultConfig.class, configId);
    assertThat(returnedVaultConfig.getRenewalInterval()).isEqualTo(10);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testMigration_shouldChangeRenewalInterval() {
    VaultConfig vaultConfig =
        VaultConfig.builder().name("test").vaultUrl("test.com").appRoleId("appRoleId").renewalInterval(0).build();
    vaultConfig.setEncryptionType(VAULT);
    String configId = wingsPersistence.save(vaultConfig);
    vaultAppRoleRenewalMigration.migrate();
    VaultConfig returnedVaultConfig = wingsPersistence.get(VaultConfig.class, configId);
    assertThat(returnedVaultConfig.getRenewalInterval()).isEqualTo(15);
  }
}
