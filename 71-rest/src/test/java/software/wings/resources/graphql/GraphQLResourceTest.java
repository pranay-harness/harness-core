package software.wings.resources.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import es.moki.ratelimitj.core.limiter.request.RequestRateLimiter;
import graphql.GraphQL;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.lib.RateBasedLimit;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.graphql.provider.QueryLanguageProvider;

import java.util.concurrent.TimeUnit;

/**
 * @author marklu on 9/12/19
 */
public class GraphQLResourceTest extends CategoryTest {
  @Mock private LimitConfigurationService limitConfigurationService;
  @Mock private QueryLanguageProvider<GraphQL> graphQLQueryLanguageProvider;
  private static MainConfiguration mainConfiguration;

  @Inject @InjectMocks private GraphQLResource graphQLResource;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @BeforeClass
  public static void setUp() {
    mainConfiguration = mock(MainConfiguration.class);
    when(mainConfiguration.getPortal()).thenReturn(new PortalConfig());
  }

  @Test
  @Category(UnitTests.class)
  public void testAccountLevelRateLimiter() {
    String accountId = UUIDGenerator.generateUuid();

    // Get default account level rate limiter.
    RequestRateLimiter rateLimiter1 = graphQLResource.getRequestRateLimiterForAccountInternal(accountId);
    assertThat(rateLimiter1).isNotNull();

    int callCountLimit = 30;
    ConfiguredLimit configuredLimit = getConfiguredLimit(accountId, callCountLimit);
    when(limitConfigurationService.getOrDefault(eq(accountId), eq(ActionType.GRAPHQL_CALL)))
        .thenReturn(configuredLimit);

    RequestRateLimiter rateLimiter2 = graphQLResource.getRequestRateLimiterForAccountInternal(accountId);
    assertThat(rateLimiter2).isNotNull();
    assertThat(rateLimiter2).isEqualTo(rateLimiter1);

    callCountLimit = 10;
    configuredLimit = getConfiguredLimit(accountId, callCountLimit);
    when(limitConfigurationService.getOrDefault(eq(accountId), eq(ActionType.GRAPHQL_CALL)))
        .thenReturn(configuredLimit);

    // Get cached customized account level rate limiter.
    RequestRateLimiter rateLimiter3 = graphQLResource.getRequestRateLimiterForAccount(accountId);
    assertThat(rateLimiter3).isNotNull();
    assertThat(rateLimiter3).isNotEqualTo(rateLimiter1);

    // Get the same cached customized account level rate limiter.
    RequestRateLimiter rateLimiter4 = graphQLResource.getRequestRateLimiterForAccount(accountId);
    assertThat(rateLimiter4).isNotNull();
    assertThat(rateLimiter4).isEqualTo(rateLimiter3);

    boolean overRateLimit = false;
    for (int i = 0; i < callCountLimit + 1; i++) {
      overRateLimit = graphQLResource.isOverRateLimit(accountId);
    }
    assertThat(overRateLimit).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testGlobalRateLimiter() {
    // Global rate limiter should check against cross-account overall requests on global limit.
    boolean overRateLimit = false;
    int globalRateLimit = mainConfiguration.getPortal().getGraphQLRateLimitPerMinute();
    for (int i = 0; i < globalRateLimit; i++) {
      String accountId = UUIDGenerator.generateUuid();
      overRateLimit = graphQLResource.isOverRateLimit(accountId);
    }
    assertThat(overRateLimit).isFalse();

    // One more extra call will over the limit
    String accountId = UUIDGenerator.generateUuid();
    overRateLimit = graphQLResource.isOverRateLimit(accountId);
    assertThat(overRateLimit).isTrue();
  }

  private ConfiguredLimit getConfiguredLimit(String accountId, int callCountLimit) {
    RateBasedLimit rateLimit = new RateLimit(callCountLimit, 1, TimeUnit.MINUTES);
    return new ConfiguredLimit<>(accountId, rateLimit, ActionType.GRAPHQL_CALL);
  }
}
