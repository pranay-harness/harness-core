/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.integration;

import static io.harness.rule.OwnerRule.AMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import software.wings.security.authentication.LoginTypeResponse;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Integration test class for UserResource class.
 */
public class UserResourceIntegrationTest extends IntegrationTestBase {
  final String getLoginTypeURI = "/users/logintype";

  @Test
  @Owner(developers = AMAN)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testLoginTypeResponseForNewAdminUserShouldReturnUserPassWord() {
    String argument = "userName=admin@harness.io";
    String url = getLoginTypeResponseUri(getLoginTypeURI, argument);
    WebTarget target = client.target(url);
    LoginTypeResponse response =
        target.request().get(new GenericType<RestResponse<LoginTypeResponse>>() {}).getResource();
    assertThat(response.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.USER_PASSWORD);
  }

  @Test
  @Owner(developers = AMAN)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testLoginTypeResponseForNonExistentUserShouldReturnUserPassWord() {
    String nonExistingUserArgument = "userName=random@xyz";
    String url = getLoginTypeResponseUri(getLoginTypeURI, nonExistingUserArgument);

    WebTarget target = client.target(url);
    Response response = target.request().get();
    assertThat(401).isEqualTo(response.getStatus());
  }

  private String getLoginTypeResponseUri(final String uri, final String arguments) {
    return API_BASE + uri + "?" + arguments;
  }
}
