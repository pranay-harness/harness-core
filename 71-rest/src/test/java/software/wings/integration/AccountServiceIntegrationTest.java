package software.wings.integration;

import static javax.ws.rs.client.Entity.entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.RestResponse;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 * @author marklu on 2018-12-26
 */
public class AccountServiceIntegrationTest extends BaseIntegrationTest {
  @Before
  public void setUp() {
    super.loginAdminUser();
  }

  @After
  public void tearDown() {
    // Recover the original account state.
    Account account = accountService.get(accountId);
    account.setNewClusterUrl(null);
    account.getLicenseInfo().setAccountStatus(AccountStatus.ACTIVE);
    accountService.update(account);
    account = accountService.get(accountId);
    assertEquals(AccountStatus.ACTIVE, account.getLicenseInfo().getAccountStatus());
  }

  @Test
  public void testAccountMigration() {
    startAccountMigration(accountId);

    String accountStatus = getAccountStatus(accountId);
    assertEquals(AccountStatus.MIGRATING, accountStatus);

    completeAccountMigration(accountId);
    accountStatus = getAccountStatus(accountId);
    assertEquals(AccountStatus.MIGRATED, accountStatus);
  }

  private String getAccountStatus(String accountId) {
    WebTarget target = client.target(API_BASE + "/account/" + accountId + "/status");
    RestResponse<String> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<String>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    String accountStatus = restResponse.getResource();
    assertNotNull(accountStatus);
    assertTrue(AccountStatus.isValid(accountStatus));
    return accountStatus;
  }

  private void startAccountMigration(String accountId) {
    WebTarget target = client.target(API_BASE + "/account/" + accountId + "/start-migration");
    RestResponse<Boolean> restResponse =
        getRequestBuilderWithAuthHeader(target).post(null, new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    Boolean statusUpdated = restResponse.getResource();
    assertNotNull(statusUpdated);
    assertTrue(statusUpdated);
  }

  private void completeAccountMigration(String accountId) {
    String newClusterUrl = "https://app.harness.io";

    WebTarget target = client.target(API_BASE + "/account/" + accountId + "/complete-migration");
    RestResponse<Boolean> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(newClusterUrl, MediaType.TEXT_PLAIN), new GenericType<RestResponse<Boolean>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    Boolean statusUpdated = restResponse.getResource();
    assertNotNull(statusUpdated);
    assertTrue(statusUpdated);

    Account account = wingsPersistence.get(Account.class, accountId);
    assertNotNull(account);
    assertEquals(newClusterUrl, account.getNewClusterUrl());
  }
}