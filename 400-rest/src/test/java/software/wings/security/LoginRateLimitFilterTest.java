/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.limits.ActionType;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.ratelimit.LoginRequestRateLimiter;

import com.google.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class LoginRateLimitFilterTest extends WingsBaseTest {
  @Mock private LoginRequestRateLimiter loginRequestRateLimiter;
  @Mock private HttpServletRequest servletRequest = mock(HttpServletRequest.class);
  @Mock private LimitConfigurationService limitConfigurationService = mock(LimitConfigurationService.class);
  @Inject @InjectMocks private LoginRateLimitFilter loginRateLimitFilter;
  private ContainerRequestContext context = mock(ContainerRequestContext.class);

  @Before
  public void setUp() {
    loginRateLimitFilter = new LoginRateLimitFilter(loginRequestRateLimiter);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testLoginRateLimitFilter() {
    String remoteHost = "remote_host";
    when(servletRequest.getHeader("X-Forwarded-For")).thenReturn(remoteHost);
    when(limitConfigurationService.getOrDefault(eq(remoteHost), eq(ActionType.LOGIN_REQUEST_TASK))).thenReturn(null);
    loginRateLimitFilter.filter(context);
    assertThat(context).isNotNull();
  }
}
