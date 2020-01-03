package software.wings.resources;

import static io.harness.rule.OwnerRule.HANTANG;
import static java.lang.String.format;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.health.CEHealthStatus;
import io.harness.ccm.health.HealthStatusService;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.core.GenericType;

public class CCMHealthResourceTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String cloudProviderId = "CLOUD_PROVIDER_ID";

  private static HealthStatusService healthStatusService = mock(HealthStatusService.class);

  @ClassRule
  public static ResourceTestRule RESOURCES =
      ResourceTestRule.builder().addResource(new CCMHealthResource(healthStatusService)).build();

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testGet() {
    RESOURCES.client()
        .target(format("/cost/health/?accountId=%s&cloudProviderId=%s", accountId, cloudProviderId))
        .request()
        .get(new GenericType<RestResponse<CEHealthStatus>>() {});
    verify(healthStatusService).getHealthStatus(eq(cloudProviderId));
  }
}
