package software.wings.resources.limits;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.limits.ActionType;
import io.harness.limits.configuration.LimitConfigurationServiceMongo;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.limits.lib.Limit;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.IntegrationTestUtil;
import software.wings.utils.WingsTestConstants;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

public class LimitConfigurationResourceIntegrationTest extends BaseIntegrationTest {
  @Inject private LimitConfigurationServiceMongo limits;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  public void testConfigure() throws Exception {
    StaticLimit limit = new StaticLimit(10);
    String url = IntegrationTestUtil.buildAbsoluteUrl("/api/limits/configure/static-limit",
        ImmutableMap.of("accountId", WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID, "action", "CREATE_APPLICATION"));

    WebTarget target = client.target(url);

    RestResponse<Boolean> response = getRequestBuilderWithAuthHeader(target).post(
        entity(limit, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});

    assertTrue(response.getResource());
    Limit fetched = limits.get(accountId, ActionType.CREATE_APPLICATION).getLimit();
    assertEquals("fetched limit from db should be same as POST argument", limit, fetched);
  }
}