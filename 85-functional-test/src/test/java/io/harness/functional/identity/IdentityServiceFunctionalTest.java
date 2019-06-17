package io.harness.functional.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.security.SecretManager;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.authentication.AccountSettingsResponse;
import software.wings.security.authentication.AuthenticationMechanism;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;

/**
 * @author marklu on 2019-06-11
 */
@Slf4j
public class IdentityServiceFunctionalTest extends AbstractFunctionalTest {
  private static final String DEV_IDENTITY_SERVICE_SECRET =
      "HVSKUYqD4e5Rxu12hFDdCJKGM64sxgEynvdDhaOHaTHhwwn0K4Ttr0uoOxSsEVYNrUU=";

  @Inject private SecretManager secretManager;
  @Inject private OwnerManager ownerManager;
  private Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
  }

  @Test
  @Category(FunctionalTests.class)
  public void testIdentityServiceClientLoginUserWithApiKey() {
    String identityServiceToken = generateIdentityServiceToken();
    GenericType<RestResponse<User>> returnType = new GenericType<RestResponse<User>>() {};
    User user = getDataForIdentityService(identityServiceToken, "identity/user/login?email=" + ADMIN_USER, returnType);
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo(ADMIN_USER);
    assertThat(user.getToken()).isNullOrEmpty();
  }

  @Test
  @Category(FunctionalTests.class)
  public void testIdentityServiceClientGetUsersWithApiKey() {
    String identityServiceToken = generateIdentityServiceToken();
    GenericType<RestResponse<List<User>>> returnType = new GenericType<RestResponse<List<User>>>() {};
    List<User> users =
        getDataForIdentityService(identityServiceToken, "identity/users?offset=0&limit=2000", returnType);
    assertThat(users).isNotNull();
    assertThat(users.size()).isGreaterThan(1);
    assertThat(users.get(0).getAccounts().size()).isGreaterThan(0);
  }

  @Test
  @Category(FunctionalTests.class)
  public void testIdentityServiceClientGetAccounts() {
    String identityServiceToken = generateIdentityServiceToken();
    GenericType<RestResponse<List<Account>>> returnType = new GenericType<RestResponse<List<Account>>>() {};
    List<Account> accounts =
        getDataForIdentityService(identityServiceToken, "identity/accounts?offset=0&limit=2000", returnType);
    assertThat(accounts).isNotNull();
    assertThat(accounts.size()).isGreaterThan(1);
    assertThat(accounts.get(0).getAccountName()).isNotEmpty();
  }

  @Test
  @Category(FunctionalTests.class)
  public void testIdentityServiceClientGetAccountSettings() {
    String identityServiceToken = generateIdentityServiceToken();
    GenericType<RestResponse<AccountSettingsResponse>> returnType =
        new GenericType<RestResponse<AccountSettingsResponse>>() {};
    AccountSettingsResponse accountSettingsResponse = getDataForIdentityService(
        identityServiceToken, "identity/account-settings?accountId=" + getAccount().getUuid(), returnType);
    assertThat(accountSettingsResponse).isNotNull();
    assertThat(accountSettingsResponse.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.USER_PASSWORD);
    assertThat(accountSettingsResponse.getOauthProviderTypes()).isNullOrEmpty();
    assertThat(accountSettingsResponse.getAllowedDomains()).isNullOrEmpty();
  }

  private String generateIdentityServiceToken() {
    Map<String, String> claims = new HashMap<>();
    claims.put("env", "gateway");
    return secretManager.generateJWTToken(claims, DEV_IDENTITY_SERVICE_SECRET, JWT_CATEGORY.IDENTITY_SERVICE_SECRET);
  }

  private <T> T getDataForIdentityService(
      String identityServiceToken, String url, GenericType<RestResponse<T>> returnType) {
    RestResponse<T> response = Setup.portal()
                                   .header(HttpHeaders.AUTHORIZATION, "IdentityService " + identityServiceToken)
                                   .contentType(ContentType.JSON)
                                   .get(url)
                                   .as(returnType.getType());
    return response.getResource();
  }
}