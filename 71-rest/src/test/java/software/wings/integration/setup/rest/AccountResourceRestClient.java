package software.wings.integration.setup.rest;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsIntegrationTestConstants.API_BASE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.RestResponse;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * @author rktummala on 09/25/18
 */
@Singleton
public class AccountResourceRestClient {
  private static final Logger logger = LoggerFactory.getLogger(AccountResourceRestClient.class);

  @Inject private software.wings.integration.UserResourceRestClient userResourceRestClient;

  public Account getAccountByName(Client client, String userToken, String accountId, String accountName)
      throws UnsupportedEncodingException {
    WebTarget target = client.target(
        API_BASE + "/users/accounts?accountId=" + accountId + "&name=" + URLEncoder.encode(accountName, "UTF-8"));
    RestResponse<PageResponse<Account>> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .get(new GenericType<RestResponse<PageResponse<Account>>>() {});
    return isEmpty(response.getResource()) ? null : response.getResource().get(0);
  }

  public Account createAccount(Client client, String userToken, Account account) {
    WebTarget target = client.target(API_BASE + "/users/account");
    RestResponse<Account> response =
        userResourceRestClient.getRequestBuilderWithAuthHeader(userToken, target)
            .post(entity(account, APPLICATION_JSON), new GenericType<RestResponse<Account>>() {});
    assertThat(response.getResource()).isInstanceOf(Account.class);
    return response.getResource();
  }
}
