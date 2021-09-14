/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.security;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import com.auth0.jwt.JWT;
import java.time.Duration;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServiceTokenGeneratorTest extends CategoryTest {
  public static final String SECRETKEY = "a611aa13eb1d5e77bc3295517ebe65ff";

  @Test(expected = Test.None.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getServiceToken() {
    final ServiceTokenGenerator serviceTokenGenerator = new ServiceTokenGenerator();
    final String token = serviceTokenGenerator.getServiceToken(SECRETKEY);
    final ServiceTokenAuthenticator serviceTokenAuthenticator =
        ServiceTokenAuthenticator.builder().secretKey(SECRETKEY).build();
    serviceTokenAuthenticator.authenticate(token);
  }

  @Test(expected = Test.None.class)
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void test_getServiceTokenWithDuration() {
    final ServiceTokenGenerator serviceTokenGenerator = new ServiceTokenGenerator();
    final String token = serviceTokenGenerator.getServiceTokenWithDuration(SECRETKEY, Duration.ofHours(12));
    final ServiceTokenAuthenticator serviceTokenAuthenticator =
        ServiceTokenAuthenticator.builder().secretKey(SECRETKEY).build();
    serviceTokenAuthenticator.authenticate(token);
    JWT jwt = JWT.decode(token);
    Duration duration = Duration.ofMillis(Math.abs(jwt.getIssuedAt().getTime() - jwt.getExpiresAt().getTime()));
    assertThat(duration).isEqualTo(Duration.ofHours(12));
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getServiceToken_tampered() {
    final ServiceTokenGenerator serviceTokenGenerator = new ServiceTokenGenerator();
    final String token = serviceTokenGenerator.getServiceToken(SECRETKEY);
    final ServiceTokenAuthenticator serviceTokenAuthenticator =
        ServiceTokenAuthenticator.builder().secretKey(SECRETKEY).build();

    serviceTokenAuthenticator.authenticate(token + "123");
  }
}
