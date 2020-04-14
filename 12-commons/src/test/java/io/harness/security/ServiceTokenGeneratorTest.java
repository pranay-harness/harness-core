package io.harness.security;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
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