package software.wings.integration;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
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
                      .isDefault(false)
                      .build();
  }

  @Test
  public void testCreateUpdateDeleteVaultConfig_shouldSucceed() {
    // 1. Create a new Vault config.
    WebTarget target = client.target(API_BASE + "/vault?accountId=" + accountId);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(vaultConfig, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    // Verify vault config was successfully created.
    assertEquals(0, restResponse.getResponseMessages().size());
    String vaultConfigId = restResponse.getResource();
    assertTrue(isNotEmpty(vaultConfigId));

    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertNotNull(savedVaultConfig);

    // 2. Update the existing vault config to make it default
    savedVaultConfig.setDefault(true);
    savedVaultConfig.setAuthToken(vaultToken);
    getRequestBuilderWithAuthHeader(target).post(
        entity(savedVaultConfig, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertNotNull(savedVaultConfig);
    assertTrue(savedVaultConfig.isDefault());

    // 3. Delete the vault config
    target = client.target(API_BASE + "/vault?accountId=" + accountId + "&vaultConfigId=" + vaultConfigId);
    RestResponse<Boolean> deleteRestResponse =
        getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse<Boolean>>() {});
    // Verify the vault config was deleted successfully
    assertEquals(0, deleteRestResponse.getResponseMessages().size());
    assertTrue(Boolean.valueOf(deleteRestResponse.getResource()));
    assertNull(wingsPersistence.get(VaultConfig.class, vaultConfigId));
  }

  @Test
  public void test_creatrDuplicateVaultSecretManager_shouldFail() {
    // 1. Create a new Vault config.
    WebTarget target = client.target(API_BASE + "/vault?accountId=" + accountId);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(vaultConfig, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    // Verify vault config was successfully created.
    assertEquals(0, restResponse.getResponseMessages().size());
    String vaultConfigId = restResponse.getResource();
    assertTrue(isNotEmpty(vaultConfigId));

    // 2. Create the same Vault config with a different name.
    try {
      vaultConfig.setName("TestVault_Different_Name");
      getRequestBuilderWithAuthHeader(target).post(
          entity(vaultConfig, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
      fail("Excpetio is expected when creating the same Vault secret manager with a different name");
    } catch (Exception e) {
      // Ignore. Expected.
    } finally {
      // 3. Delete the vault config
      target = client.target(API_BASE + "/vault?accountId=" + accountId + "&vaultConfigId=" + vaultConfigId);
      RestResponse<Boolean> deleteRestResponse =
          getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse<Boolean>>() {});
      // Verify the vault config was deleted successfully
      assertEquals(0, deleteRestResponse.getResponseMessages().size());
      assertTrue(Boolean.valueOf(deleteRestResponse.getResource()));
      assertNull(wingsPersistence.get(VaultConfig.class, vaultConfigId));
    }
  }
}
