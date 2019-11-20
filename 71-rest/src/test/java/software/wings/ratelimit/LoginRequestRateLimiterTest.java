package software.wings.ratelimit;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.lib.RateBasedLimit;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

import java.util.concurrent.TimeUnit;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoginRequestRateLimiterTest extends WingsBaseTest {
  @Mock private LimitConfigurationService limitConfigurationService;
  @Inject @InjectMocks private LoginRequestRateLimiter loginRequestRateLimiter;

  @Before
  public void setUp() {
    loginRequestRateLimiter = new LoginRequestRateLimiter(limitConfigurationService);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC0_testOverGlobalRateLimiter() {
    boolean overRateLimit = false;
    int count = 15;
    String remoteHost = "remote_host";
    for (int i = 0; i < count; i++) {
      when(limitConfigurationService.getOrDefault(anyString(), eq(ActionType.LOGIN_REQUEST_TASK))).thenReturn(null);
      overRateLimit = loginRequestRateLimiter.isOverRateLimit(remoteHost);
    }
    assertThat(overRateLimit).isFalse();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC1_testOverGlobalRateLimiter() {
    boolean overRateLimit = false;
    int count = 400;
    String remoteHost = "remote_host";
    for (int i = 0; i < count; i++) {
      when(limitConfigurationService.getOrDefault(anyString(), eq(ActionType.LOGIN_REQUEST_TASK))).thenReturn(null);
      overRateLimit = loginRequestRateLimiter.isOverRateLimit(remoteHost);
    }
    assertThat(overRateLimit).isTrue();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void TC2_testGlobalRateLimiter() {
    boolean overRateLimit = false;
    int globalRateLimit = 300;
    String remoteHost = "remote_host";
    int count = 400;

    for (int i = 0; i < count; i++) {
      when(limitConfigurationService.getOrDefault(anyString(), eq(ActionType.LOGIN_REQUEST_TASK)))
          .thenReturn(getConfiguredLimit(remoteHost, globalRateLimit));
      overRateLimit = loginRequestRateLimiter.isOverRateLimit(remoteHost);
    }
    assertThat(overRateLimit).isTrue();
  }

  private ConfiguredLimit getConfiguredLimit(String accountId, int requestCountLimit) {
    RateBasedLimit rateLimit = new RateLimit(requestCountLimit, 1, TimeUnit.MINUTES);
    return new ConfiguredLimit<>(accountId, rateLimit, ActionType.LOGIN_REQUEST_TASK);
  }
}
