package software.wings.integration;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Preconditions;

import org.junit.Before;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.beans.VaultConfig;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 9/21/18.
 */
public class VaultIntegrationTest extends BaseIntegrationTest {
  private String vaultToken;
  private VaultConfig vaultConfig;
  private VaultConfig vaultConfig2;

  @Before
  public void setUp() {
    super.loginAdminUser();
    this.vaultToken = System.getProperty("vault.token", "root");
    Preconditions.checkState(isNotEmpty(vaultToken));

    vaultConfig = VaultConfig.builder()
                      .accountId(accountId)
                      .name("TestVault")
                      .vaultUrl("http://127.0.0.1:8200")
                      .authToken(vaultToken)
                      .isDefault(true)
                      .build();

    vaultConfig2 = VaultConfig.builder()
                       .accountId(accountId)
                       .name("TestVault2")
                       .vaultUrl("http://127.0.0.1:8300")
                       .authToken(vaultToken)
                       .isDefault(true)
                       .build();
  }

  @Test
  public void testCreateUpdateDeleteVaultConfig_shouldSucceed() {
    // 1. Create a new Vault config.
    String vaultConfigId = createVaultConfig(vaultConfig);

    try {
      VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
      assertNotNull(savedVaultConfig);

      // 2. Update the existing vault config to make it default
      savedVaultConfig.setAuthToken(vaultToken);
      updateVaultConfig(savedVaultConfig);

      savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
      assertNotNull(savedVaultConfig);
      assertTrue(savedVaultConfig.isDefault());
    } finally {
      // 3. Delete the vault config
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  public void test_createDuplicateVaultSecretManager_shouldFail() {
    // 1. Create a new Vault config.
    String vaultConfigId = createVaultConfig(vaultConfig);

    // 2. Create the same Vault config with a different name.
    try {
      updateVaultConfig(vaultConfig);
      fail("Exception is expected when creating the same Vault secret manager with a different name");
    } catch (Exception e) {
      // Ignore. Expected.
    } finally {
      // 3. Delete the vault config
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  public void test_createNewDefaultVault_shouldUnsetPreviousDefaultVaultConfig() {
    // Create the first default vault config
    String vaultConfigId = createVaultConfig(vaultConfig);
    // Create 2nd default vault config. The 1 vault will be set to be non-default.
    String vaultConfig2Id = createVaultConfig(vaultConfig2);

    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertNotNull(savedVaultConfig);

    VaultConfig savedVaultConfig2 = wingsPersistence.get(VaultConfig.class, vaultConfig2Id);
    assertNotNull(savedVaultConfig2);

    try {
      assertFalse(savedVaultConfig.isDefault());
      assertTrue(savedVaultConfig2.isDefault());

      // Update 1st vault config to be default again. 2nd vault config will be set to be non-default.
      savedVaultConfig.setDefault(true);
      savedVaultConfig.setAuthToken(vaultToken);
      updateVaultConfig(savedVaultConfig);

      savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
      assertTrue(savedVaultConfig.isDefault());

      savedVaultConfig2 = wingsPersistence.get(VaultConfig.class, vaultConfig2Id);
      assertFalse(savedVaultConfig2.isDefault());
    } finally {
      // Delete both vault configs.
      deleteVaultConfig(vaultConfigId);
      deleteVaultConfig(vaultConfig2Id);
    }
  }

  @Test
  public void test_unsetOnlyDefaultVault_shouldFail() {
    // Create the first default vault config
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertNotNull(savedVaultConfig);

    try {
      savedVaultConfig.setDefault(false);
      savedVaultConfig.setAuthToken(vaultToken);
      updateVaultConfig(savedVaultConfig);
      fail("Unset the only default vault config manager will fail!");
    } catch (Exception e) {
      // Exception is expected.
    } finally {
      // Clean up.
      deleteVaultConfig(vaultConfigId);
    }
  }

  private String createVaultConfig(VaultConfig vaultConfig) {
    WebTarget target = client.target(API_BASE + "/vault?accountId=" + accountId);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(vaultConfig, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    // Verify vault config was successfully created.
    assertEquals(0, restResponse.getResponseMessages().size());
    String vaultConfigId = restResponse.getResource();
    assertTrue(isNotEmpty(vaultConfigId));

    return vaultConfigId;
  }

  private void updateVaultConfig(VaultConfig vaultConfig) {
    WebTarget target = client.target(API_BASE + "/vault?accountId=" + accountId);
    vaultConfig.setName("TestVault_Different_Name");
    getRequestBuilderWithAuthHeader(target).post(
        entity(vaultConfig, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
  }

  private void deleteVaultConfig(String vaultConfigId) {
    WebTarget target = client.target(API_BASE + "/vault?accountId=" + accountId + "&vaultConfigId=" + vaultConfigId);
    RestResponse<Boolean> deleteRestResponse =
        getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse<Boolean>>() {});
    // Verify the vault config was deleted successfully
    assertEquals(0, deleteRestResponse.getResponseMessages().size());
    assertTrue(Boolean.valueOf(deleteRestResponse.getResource()));
    assertNull(wingsPersistence.get(VaultConfig.class, vaultConfigId));
  }
}