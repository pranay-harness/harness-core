package software.wings.integration;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

  @Before
  public void setUp() {
    super.loginAdminUser();
    this.vaultToken = System.getProperty("vault.token");
    Preconditions.checkState(isNotEmpty(vaultToken));
  }

  @Test
  public void addNewVault() throws Exception {
    // get all applications
    WebTarget target = client.target(API_BASE + "/vault?accountId=" + accountId);
    RestResponse<String> restResponse =
        getRequestBuilderWithAuthHeader(target).post(entity(VaultConfig.builder()
                                                                .accountId(accountId)
                                                                .vaultUrl("http://127.0.0.1:8200")
                                                                .authToken(vaultToken)
                                                                .isDefault(false)
                                                                .build(),
                                                         APPLICATION_JSON),
            new GenericType<RestResponse<String>>() {});

    assertEquals(0, restResponse.getResponseMessages().size());
    assertTrue(isNotEmpty(restResponse.getResource()));

    assertNotNull(wingsPersistence.get(VaultConfig.class, restResponse.getResource()));
  }
}
