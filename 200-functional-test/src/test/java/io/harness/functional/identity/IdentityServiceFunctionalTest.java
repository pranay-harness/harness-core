/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.identity;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;

import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.security.AuthenticationFilter;
import software.wings.security.HarnessUserAccountActions;
import software.wings.security.JWT_CATEGORY;
import software.wings.security.SecretManager;
import software.wings.security.authentication.AccountSettingsResponse;
import software.wings.security.authentication.oauth.OauthUserInfo;

import com.google.inject.Inject;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author marklu on 2019-06-11
 */
@Slf4j
@OwnedBy(HarnessTeam.PL)
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
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void testIdentityServiceClientLoginUser() {
    String identityServiceToken = generateIdentityServiceToken();
    GenericType<RestResponse<User>> returnType = new GenericType<RestResponse<User>>() {};
    User user = getDataForIdentityService(identityServiceToken, "identity/user/login?email=" + ADMIN_USER, returnType);
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo(ADMIN_USER);
    assertThat(user.getToken()).isNullOrEmpty();
    assertThat(user.isFirstLogin()).isFalse();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void testIdentityServiceClientListUsers() {
    String identityServiceToken = generateIdentityServiceToken();
    GenericType<RestResponse<List<User>>> returnType = new GenericType<RestResponse<List<User>>>() {};
    List<User> users =
        getDataForIdentityService(identityServiceToken, "identity/users?offset=0&limit=2000", returnType);
    assertThat(users).isNotNull();
    assertThat(users.size()).isGreaterThan(1);
    assertThat(users.get(0).getAccounts().size()).isGreaterThan(0);
  }

  @Test
  @Owner(developers = UTKARSH, intermittent = true)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void testIdentityServiceClientListAccounts() {
    String identityServiceToken = generateIdentityServiceToken();
    GenericType<RestResponse<List<Account>>> returnType = new GenericType<RestResponse<List<Account>>>() {};
    List<Account> accounts =
        getDataForIdentityService(identityServiceToken, "identity/accounts?offset=0&limit=2000", returnType);
    assertThat(accounts).isNotNull();
    assertThat(accounts.size()).isGreaterThan(1);
    assertThat(accounts.get(0).getAccountName()).isNotEmpty();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
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

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void testIdentityServiceClientGetPublicUserInfo() {
    String identityServiceToken = generateIdentityServiceToken();
    GenericType<RestResponse<User>> returnType = new GenericType<RestResponse<User>>() {};
    User user = getDataForIdentityService(identityServiceToken, adminUser.getUuid(), "identity/user", returnType);
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isNotNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void testIdentityServiceClientSignupOauthUser() {
    String userEmail = "oauth_trial_user_" + System.currentTimeMillis() + "@harness.io";
    String identityServiceToken = generateIdentityServiceToken();
    GenericType<RestResponse<User>> returnType = new GenericType<RestResponse<User>>() {};
    OauthUserInfo userInfo = OauthUserInfo.builder().email(userEmail).name("Oauth User").build();
    User user =
        signupOauthUser(identityServiceToken, "identity/oauth/signup-user?provider=GITHUB", userInfo, returnType);
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo(userEmail);
    assertThat(user.getUuid()).isNotEmpty();
    assertThat(user.isFirstLogin()).isTrue();
  }

  private String generateIdentityServiceToken() {
    try {
      Map<String, String> claims = new HashMap<>();
      claims.put(SecretManager.ENV, "gateway");
      claims.put(SecretManager.HARNESS_USER, SecretManager.serializeAccountActions(new HarnessUserAccountActions()));
      return secretManager.generateJWTToken(claims, DEV_IDENTITY_SERVICE_SECRET, JWT_CATEGORY.IDENTITY_SERVICE_SECRET);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

  private <T> T getDataForIdentityService(
      String identityServiceToken, String userId, String url, GenericType<RestResponse<T>> returnType) {
    RestResponse<T> response = Setup.portal()
                                   .header(HttpHeaders.AUTHORIZATION, "IdentityService " + identityServiceToken)
                                   .header(AuthenticationFilter.USER_IDENTITY_HEADER, userId)
                                   .contentType(ContentType.JSON)
                                   .get(url)
                                   .as(returnType.getType());
    return response.getResource();
  }

  private User signupOauthUser(
      String identityServiceToken, String url, OauthUserInfo userInfo, GenericType<RestResponse<User>> returnType) {
    RestResponse<User> response = Setup.portal()
                                      .header(HttpHeaders.AUTHORIZATION, "IdentityService " + identityServiceToken)
                                      .contentType(ContentType.JSON)
                                      .body(userInfo)
                                      .post(url)
                                      .as(returnType.getType());
    return response.getResource();
  }
}
