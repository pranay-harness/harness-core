package io.harness.security;

import static io.harness.AuthorizationServiceHeader.BEARER;
import static io.harness.AuthorizationServiceHeader.DEFAULT;
import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.rule.OwnerRule.PHOENIKX;

import static org.assertj.core.api.Fail.fail;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.security.dto.ServicePrincipal;
import io.harness.security.dto.UserPrincipal;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NextGenAuthenticationFilterTest extends CategoryTest {
  public static final String BASE_URL = "https://abc.com";
  public static final String API = "/api/feature-flags";
  public static final String GET_CALL = "GET";
  public static final String EMAIL = "hello@hello.hello";
  public static final String ACCOUNT_ID = "some_random_account_id";
  public static final String AUTHORIZATION = "Authorization";
  private NextGenAuthenticationFilter nextGenAuthenticationFilter;
  private ServiceTokenGenerator serviceTokenGenerator;
  private static final String BEARER_SECRET = "bjalfajkfvsajfjslfagfawqwdbasdbasjdaadsd";
  private static final String SERVICE_SECRET = "bfjasfofugfarkrwhruwrukanlrnajry";

  @Before
  public void setup() {
    Map<String, String> serviceToSecretMapping = new HashMap<>();
    serviceToSecretMapping.put(BEARER.getServiceId(), BEARER_SECRET);
    serviceToSecretMapping.put(DEFAULT.getServiceId(), SERVICE_SECRET);
    serviceTokenGenerator = new ServiceTokenGenerator();
    nextGenAuthenticationFilter = new NextGenAuthenticationFilter(x -> true, null, serviceToSecretMapping);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testAppropriateDataInContext_ForUserInitiatedCall() {
    String userId = "hello_world";
    ContainerRequest containerRequest =
        new ContainerRequest(URI.create(BASE_URL), URI.create(API), GET_CALL, null, new MapPropertiesDelegate());
    containerRequest.header(AUTHORIZATION,
        BEARER.getServiceId() + StringUtils.SPACE
            + serviceTokenGenerator.getServiceTokenWithDuration(
                BEARER_SECRET, Duration.ofHours(4), new UserPrincipal(userId, EMAIL, ACCOUNT_ID)));
    nextGenAuthenticationFilter.filter(containerRequest);

    Assertions.assertThat(SecurityContextBuilder.getPrincipal()).isNotNull();
    Assertions.assertThat(SecurityContextBuilder.getPrincipal().getName()).isEqualTo(userId);
    Assertions.assertThat(SourcePrincipalContextBuilder.getSourcePrincipal()).isNotNull();
    Assertions.assertThat(SourcePrincipalContextBuilder.getSourcePrincipal().getName()).isEqualTo(userId);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testAppropriateDataInContext_ForServiceInitiatedCall_WithSourcePrincipalPresent() {
    String userId = "some_random_user_id";
    ContainerRequest containerRequest =
        new ContainerRequest(URI.create(BASE_URL), URI.create(API), GET_CALL, null, new MapPropertiesDelegate());
    containerRequest.header(AUTHORIZATION,
        NG_MANAGER.getServiceId() + StringUtils.SPACE
            + serviceTokenGenerator.getServiceTokenWithDuration(
                SERVICE_SECRET, Duration.ofHours(4), new ServicePrincipal(NG_MANAGER.getServiceId())));

    containerRequest.header("X-Source-Principal",
        NG_MANAGER.getServiceId() + StringUtils.SPACE
            + serviceTokenGenerator.getServiceTokenWithDuration(
                SERVICE_SECRET, Duration.ofHours(4), new UserPrincipal(userId, EMAIL, ACCOUNT_ID)));

    nextGenAuthenticationFilter.filter(containerRequest);

    Assertions.assertThat(SecurityContextBuilder.getPrincipal()).isNotNull();
    Assertions.assertThat(SecurityContextBuilder.getPrincipal().getName()).isEqualTo(NG_MANAGER.getServiceId());
    Assertions.assertThat(SourcePrincipalContextBuilder.getSourcePrincipal()).isNotNull();
    Assertions.assertThat(SourcePrincipalContextBuilder.getSourcePrincipal().getName()).isEqualTo(userId);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testAppropriateDataInContext_ForServiceInitiatedCall_WithSourcePrincipalAbsent() {
    ContainerRequest containerRequest =
        new ContainerRequest(URI.create(BASE_URL), URI.create(API), GET_CALL, null, new MapPropertiesDelegate());
    containerRequest.header(AUTHORIZATION,
        NG_MANAGER.getServiceId() + StringUtils.SPACE
            + serviceTokenGenerator.getServiceTokenWithDuration(
                SERVICE_SECRET, Duration.ofHours(4), new ServicePrincipal(NG_MANAGER.getServiceId())));

    nextGenAuthenticationFilter.filter(containerRequest);

    Assertions.assertThat(SecurityContextBuilder.getPrincipal()).isNotNull();
    Assertions.assertThat(SecurityContextBuilder.getPrincipal().getName()).isEqualTo(NG_MANAGER.getServiceId());
    Assertions.assertThat(SourcePrincipalContextBuilder.getSourcePrincipal()).isNotNull();
    Assertions.assertThat(SourcePrincipalContextBuilder.getSourcePrincipal().getName())
        .isEqualTo(NG_MANAGER.getServiceId());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testThrowException_WhenSourcePrincipalHeaderPresentButInvalid() {
    ContainerRequest containerRequest =
        new ContainerRequest(URI.create(BASE_URL), URI.create(API), GET_CALL, null, new MapPropertiesDelegate());
    containerRequest.header(AUTHORIZATION,
        NG_MANAGER.getServiceId() + StringUtils.SPACE
            + serviceTokenGenerator.getServiceTokenWithDuration(
                SERVICE_SECRET, Duration.ofHours(4), new ServicePrincipal(NG_MANAGER.getServiceId())));

    containerRequest.header("X-Source-Principal",
        NG_MANAGER.getServiceId() + StringUtils.SPACE
            + serviceTokenGenerator.getServiceTokenWithDuration(
                "some_random_secret", Duration.ofHours(4), new UserPrincipal("userId", EMAIL, ACCOUNT_ID)));

    try {
      nextGenAuthenticationFilter.filter(containerRequest);
      fail("Should not reach here");
    } catch (InvalidRequestException jwtVerificationException) {
      // all good
    }
  }
}
