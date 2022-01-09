/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.limits;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.limits.ActionType;
import io.harness.limits.configuration.LimitConfigurationServiceMongo;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.limits.lib.Limit;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import software.wings.integration.IntegrationTestBase;
import software.wings.integration.IntegrationTestUtils;
import software.wings.utils.WingsTestConstants;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class LimitConfigurationResourceIntegrationTest extends IntegrationTestBase {
  @Inject private LimitConfigurationServiceMongo limits;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testConfigure() throws Exception {
    StaticLimit limit = new StaticLimit(10);
    String url = IntegrationTestUtils.buildAbsoluteUrl("/api/limits/configure/static-limit",
        ImmutableMap.of("accountId", WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID, "action", "CREATE_APPLICATION"));

    WebTarget target = client.target(url);

    RestResponse<Boolean> response = getRequestBuilderWithAuthHeader(target).post(
        entity(limit, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});

    assertThat(response.getResource()).isTrue();
    Limit fetched = limits.get(accountId, ActionType.CREATE_APPLICATION).getLimit();
    assertThat(fetched).isEqualTo(limit);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testConfigureRateLimit() throws Exception {
    RateLimit limit = new RateLimit(10, 24, TimeUnit.HOURS);
    String url = IntegrationTestUtils.buildAbsoluteUrl("/api/limits/configure/rate-limit",
        ImmutableMap.of("accountId", WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID, "action", "DEPLOY"));

    WebTarget target = client.target(url);

    RestResponse<Boolean> response = getRequestBuilderWithAuthHeader(target).post(
        entity(limit, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});

    assertThat(response.getResource()).isTrue();
    Limit fetched = limits.get(accountId, ActionType.DEPLOY).getLimit();
    assertThat(fetched).isEqualTo(limit);
  }
}
